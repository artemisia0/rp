package io.github.artemisia0.rp.service;

import io.github.artemisia0.rp.dto.IcebergConnectionDto;
import io.github.artemisia0.rp.dto.SnapshotDiffDto;
import io.github.artemisia0.rp.dto.SnapshotDto;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.*;
import org.apache.iceberg.catalog.*;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.SnapshotUtil;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.iceberg.io.FileIO;

@Service
public class IcebergService {

    private static final Schema DEFAULT_TABLE_SCHEMA = new Schema(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get())
    );

    private final Configuration conf;
    private volatile IcebergConnectionDto connection;
    private volatile Catalog catalog;

    public IcebergService(IcebergConnectionService connectionService) {
        // Workaround for Hadoop compatibility with Java 17+
        System.setProperty("HADOOP_HOME", "/tmp");
        
        this.conf = new Configuration(false);
        this.conf.set("fs.defaultFS", "file:///");
        this.conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");

        this.connection = connectionService.getCurrent();
        this.catalog = createCatalog(this.connection);
    }

    private Catalog createCatalog(IcebergConnectionDto connection) {
        return new HadoopCatalog(conf, connection.getWarehouse());
    }

    private TableIdentifier tableId() {
        return TableIdentifier.of(connection.getNamespace(), connection.getTable());
    }

    private Table freshTableOrNull() {
        // Always load table from current catalog and refresh
        TableIdentifier tableId = tableId();
        if (!catalog.tableExists(tableId)) {
            return null;
        }
        Table table = catalog.loadTable(tableId);
        table.refresh();
        return table;
    }

    public synchronized void reloadCatalog() {
        // Create a new catalog instance to pick up filesystem changes
        this.catalog = createCatalog(connection);
    }

    public synchronized void applyConnection(IcebergConnectionDto connection) {
        this.connection = connection;
        this.catalog = createCatalog(connection);
    }

    public synchronized void initializeIfMissing() {
        ensureWarehouseExists(connection.getWarehouse());
        Catalog reloaded = createCatalog(connection);
        ensureNamespaceExists(reloaded);
        ensureTableExists(reloaded);
        this.catalog = reloaded;
    }

    private void ensureNamespaceExists(Catalog targetCatalog) {
        if (targetCatalog instanceof SupportsNamespaces) {
            SupportsNamespaces namespaces = (SupportsNamespaces) targetCatalog;
            Namespace namespace = Namespace.of(connection.getNamespace());
            if (!namespaces.namespaceExists(namespace)) {
                try {
                    namespaces.createNamespace(namespace);
                } catch (AlreadyExistsException ignored) {
                    // Namespace was created concurrently.
                }
            }
        }
    }

    private void ensureTableExists(Catalog targetCatalog) {
        TableIdentifier tableId = tableId();
        if (!targetCatalog.tableExists(tableId)) {
            try {
                targetCatalog.createTable(tableId, DEFAULT_TABLE_SCHEMA, PartitionSpec.unpartitioned());
            } catch (AlreadyExistsException ignored) {
                // Table was created concurrently.
            }
        }
    }

    private void ensureWarehouseExists(String warehouse) {
        if (warehouse == null || !warehouse.startsWith("file:")) {
            return;
        }
        File directory = new File(URI.create(warehouse));
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Unable to create warehouse directory: " + warehouse);
        }
    }

    public List<SnapshotDto> listSnapshots() {
        Table table = freshTableOrNull();
        if (table == null) {
            return List.of();
        }
        List<SnapshotDto> out = new ArrayList<>();
        for (Snapshot s : table.snapshots()) {
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
        Table table = freshTableOrNull();
        if (table == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found");
        }
        return diff(table, snapshotId);
    }

    private SnapshotDiffDto diff(Table table, long snapshotId) {
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
        Table table = freshTableOrNull();
        if (table == null) {
            return List.of();
        }
        List<SnapshotDiffDto> diffs = new ArrayList<>();
        for (Snapshot s : table.snapshots()) {
            diffs.add(diff(table, s.snapshotId()));
        }
        return diffs;
    }
}
