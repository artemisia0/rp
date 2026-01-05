package io.github.artemisia0.rp.service;

import io.github.artemisia0.rp.dto.SnapshotDto;
import io.github.artemisia0.rp.dto.SnapshotDiffDto;
import org.apache.iceberg.*;
import org.apache.iceberg.catalog.*;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class IcebergService {
    private final Table table;

    public IcebergService() {
        Configuration conf = new Configuration(false);
        conf.set("fs.defaultFS", "file:///");
        conf.set("hadoop.security.authentication", "simple");
        UserGroupInformation.setConfiguration(conf);

        String warehouse = "file://" + new File("warehouse").getAbsolutePath();
        Catalog catalog = new HadoopCatalog(conf, warehouse);
        this.table = catalog.loadTable(TableIdentifier.of("db", "my_table"));
    }

    public List<SnapshotDto> listSnapshots() {
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
        Snapshot s = table.snapshot(snapshotId);
        int added = 0;
        int removed = 0;
        for (DataFile f : s.addedDataFiles(table.io())) {
            added++;
        }
        for (DataFile f : s.removedDataFiles(table.io())) {
            removed++;
        }
        return new SnapshotDiffDto(snapshotId, added, removed);
    }
}
