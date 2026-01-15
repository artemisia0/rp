package io.github.artemisia0.rp.service;

import io.github.artemisia0.rp.dto.SnapshotDiffDto;
import io.github.artemisia0.rp.dto.SnapshotDto;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.*;
import org.apache.iceberg.catalog.*;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.iceberg.util.SnapshotUtil;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.iceberg.io.FileIO;

@Service
public class IcebergService {

    private final Catalog catalog;
    private final TableIdentifier tableId;

    public IcebergService() {
        Configuration conf = new Configuration(false);
        conf.set("fs.defaultFS", "file:///");

        String warehouse = "file://" + new File("warehouse").getAbsolutePath();
        this.catalog = new HadoopCatalog(conf, warehouse);
        this.tableId = TableIdentifier.of("db", "my_table");
    }

    private Table table() {
        return catalog.loadTable(tableId);
    }

    public List<SnapshotDto> listSnapshots() {
        List<SnapshotDto> out = new ArrayList<>();
        for (Snapshot s : table().snapshots()) {
            out.add(new SnapshotDto(
                s.snapshotId(),
                s.parentId(),
                s.operation(),
                s.timestampMillis()
            ));
        }
        return out;
    }

    public SnapshotDiffDto diff(long snapshotId) {
        Table table = table();
        Snapshot snapshot = table.snapshot(snapshotId);

        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Snapshot not found");
        }

        if (snapshot.parentId() == null) {
            // Compare against empty table: all rows are ADDED
            java.util.Map<Object, java.util.Map<String, Object>> afterRows = readRowsAsMap(table, snapshot.snapshotId());
            java.util.List<io.github.artemisia0.rp.dto.RowChangeDto> rowChanges = new java.util.ArrayList<>();
            for (var e : afterRows.entrySet()) {
                rowChanges.add(new io.github.artemisia0.rp.dto.RowChangeDto("ADDED", null, e.getValue()));
            }
            return new SnapshotDiffDto(snapshotId, List.of(), List.of(), rowChanges);
        }

        Snapshot parent = table.snapshot(snapshot.parentId());
        List<String> addedFiles = new ArrayList<>();
        List<String> removedFiles = new ArrayList<>();
        Function<Long, Snapshot> snapshotFn = table::snapshot;
        FileIO fileIO = table.io();
        for (DataFile f : SnapshotUtil.newFiles(parent.snapshotId(), snapshot.snapshotId(), snapshotFn, fileIO)) {
            addedFiles.add(f.path().toString());
        }
        // deletedFiles is not available in this Iceberg version; skip removedFiles
        // for (DataFile f : SnapshotUtil.deletedFiles(parent.snapshotId(), snapshot.snapshotId(), snapshotFn, fileIO)) {
        //     removedFiles.add(f.path().toString());
        // }

        // Row-level diff
        java.util.Map<Object, java.util.Map<String, Object>> beforeRows = readRowsAsMap(table, parent.snapshotId());
        java.util.Map<Object, java.util.Map<String, Object>> afterRows = readRowsAsMap(table, snapshot.snapshotId());
        java.util.List<io.github.artemisia0.rp.dto.RowChangeDto> rowChanges = new java.util.ArrayList<>();
        for (var e : afterRows.entrySet()) {
            Object pk = e.getKey();
            java.util.Map<String, Object> after = e.getValue();
            java.util.Map<String, Object> before = beforeRows.get(pk);
            if (before == null) {
                rowChanges.add(new io.github.artemisia0.rp.dto.RowChangeDto("ADDED", null, after));
            } else if (!before.equals(after)) {
                rowChanges.add(new io.github.artemisia0.rp.dto.RowChangeDto("UPDATED", before, after));
            }
        }
        for (var e : beforeRows.entrySet()) {
            if (!afterRows.containsKey(e.getKey())) {
                rowChanges.add(new io.github.artemisia0.rp.dto.RowChangeDto("REMOVED", e.getValue(), null));
            }
        }
        return new SnapshotDiffDto(snapshotId, addedFiles, removedFiles, rowChanges);
    }

    private java.util.Map<Object, java.util.Map<String, Object>> readRowsAsMap(Table table, long snapshotId) {
        java.util.Map<Object, java.util.Map<String, Object>> rows = new java.util.HashMap<>();
        try (CloseableIterable<Record> records =
                     IcebergGenerics.read(table)
                             .useSnapshot(snapshotId)
                             .build()) {
            for (org.apache.iceberg.data.Record r : records) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                for (int i = 0; i < r.size(); i++) {
                    row.put(r.struct().fields().get(i).name(), r.get(i));
                }
                Object pk = row.get("id");
                rows.put(pk, row);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read snapshot data", e);
        }
        return rows;
    }

    public List<SnapshotDiffDto> allDiffs() {
        List<SnapshotDiffDto> diffs = new ArrayList<>();
        for (Snapshot s : table().snapshots()) {
            diffs.add(diff(s.snapshotId()));
        }
        return diffs;
    }
}
