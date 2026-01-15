package io.github.artemisia0.rp.service;

import io.github.artemisia0.rp.dto.SnapshotDiffDto;
import io.github.artemisia0.rp.dto.SnapshotDto;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.*;
import org.apache.iceberg.catalog.*;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.iceberg.util.SnapshotUtil;
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
            return new SnapshotDiffDto(snapshotId, List.of(), List.of());
        }

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        Function<Long, Snapshot> snapshotFunction = table::snapshot;
        FileIO fileIO = table.io();
        for (DataFile f : SnapshotUtil.newFiles(snapshot.parentId(), snapshot.snapshotId(), snapshotFunction, fileIO)) {
            added.add(f.path().toString());
        }
        // TODO: Implement removed files logic if/when available in Iceberg
        return new SnapshotDiffDto(snapshotId, added, removed);
    }

    public List<SnapshotDiffDto> allDiffs() {
        List<SnapshotDiffDto> diffs = new ArrayList<>();
        for (Snapshot s : table().snapshots()) {
            if (s.parentId() != null) {
                diffs.add(diff(s.snapshotId()));
            }
        }
        return diffs;
    }
}
