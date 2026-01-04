package io.github.artemisia0.rp;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;

import org.apache.iceberg.Table;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.hadoop.HadoopCatalog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

  static {
    Configuration conf = new Configuration(false);
    conf.set("fs.defaultFS", "file:///");
    conf.set("hadoop.security.authentication", "simple");
    UserGroupInformation.setConfiguration(conf);
  }

  public static void main(String[] args) throws Exception {

    String warehousePath =
        "file://" + new File("warehouse").getAbsolutePath();

    Configuration hadoopConf = new Configuration();
    Catalog catalog = new HadoopCatalog(hadoopConf, warehousePath);

    TableIdentifier tableId = TableIdentifier.of("db", "my_table");
    Table table = catalog.loadTable(tableId);

    List<Snapshot> snapshots = new ArrayList<>();
    for (Snapshot s : table.snapshots()) {
      snapshots.add(s);
      System.out.printf(
          "Snapshot %d | parent=%s | op=%s | ts=%d%n",
          s.snapshotId(),
          s.parentId(),
          s.operation(),
          s.timestampMillis()
      );
    }

    if (snapshots.size() < 2) {
      System.out.println("Not enough snapshots to compute diffs.");
      return;
    }

    System.out.println("\n=== Snapshot diffs ===");

    for (int i = 1; i < snapshots.size(); i++) {
      Snapshot curr = snapshots.get(i);

      System.out.printf(
          "%nSnapshot %d diff (op=%s)%n",
          curr.snapshotId(),
          curr.operation()
      );

      printDiff(table, curr);
    }
  }

  private static void printDiff(Table table, Snapshot snapshot) {

    int added = 0;
    int removed = 0;

    for (DataFile f : snapshot.addedDataFiles(table.io())) {
      added++;
    }

    for (DataFile f : snapshot.removedDataFiles(table.io())) {
      removed++;
    }

    System.out.printf(
        "  Added files: %d%n  Removed files: %d%n",
        added,
        removed
    );
  }
}
