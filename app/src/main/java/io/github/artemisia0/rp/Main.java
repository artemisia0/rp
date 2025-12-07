/*
package io.github.artemisia0.rp;


///////////////
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@SpringBootApplication
@RequestMapping("/api")
public class Main {

  @RequestMapping("/hello")
  String hello() {
    return "Hi there from api!";
  }

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }
}
///////

import org.apache.iceberg.Table;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;

// import rest catalog

import java.util.Map;


public class Main {
  public static void main(String[] args) {
    Catalog catalog = new RESTCatalog();
    catalog.initialize("rest", Map.of(
      "uri", "http://localhost:8181",
      "warehouse", "file:./warehouse"
    ));
              
    TableIdentifier tableId = TableIdentifier.of("db", "mytable");
    Table table = catalog.loadTable(id);

    System.out.println("=== Snapshots ===");
    for (Snapshot snap : table.snapshots()) {
      System.out.printf("Snapshot id: %d, op: %s, ts: %d%n",
        snap.snapshotId(),
        snap.operation(),
        snap.timestampMillis()
      );
    }
  }
}

*/





package io.github.artemisia0.rp;

import org.apache.iceberg.Table;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.Table;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.catalog.Catalog;
import java.util.Map;
import java.util.List;

public class Main {
  public static void main(String[] args) throws Exception {
    // Simplest way: just load the table and print snapshots
    // You must create the table externally (e.g., with Spark or Iceberg CLI)
    // Example: using a Hadoop warehouse (adjust as needed)
    // Catalog initialization is omitted for brevity; see Iceberg docs for details
    // Initialize HadoopCatalog (simplest for local warehouse)
    org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
    hadoopConf.set("fs.defaultFS", "file:///");
    String warehousePath = "file://" + new java.io.File("warehouse").getAbsolutePath();
    org.apache.iceberg.catalog.Catalog catalog = new org.apache.iceberg.hadoop.HadoopCatalog(hadoopConf, warehousePath);
    TableIdentifier id = TableIdentifier.of("db", "my_table");
    Table table = catalog.loadTable(id);
    Iterable<Snapshot> snapshots = table.snapshots();
    int count = 0;
    for (Snapshot snap : snapshots) {
      System.out.printf("Snapshot id: %d, op: %s, ts: %d%n",
        snap.snapshotId(),
        snap.operation(),
        snap.timestampMillis()
      );
      count++;
    }
    if (count < 2) {
      System.out.println("Not enough snapshots for diff");
      return;
    }
    // Optionally, print diff info here
  }
}
