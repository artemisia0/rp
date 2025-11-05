package io.github.artemisia0.iceberg_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.hive.HiveCatalog;
import org.apache.iceberg.Table;


@SpringBootApplication
public class IcebergAppApplication {

	public static void main(String[] args) {
		System.out.println("ahoj from main");
		SpringApplication.run(IcebergAppApplication.class, args);
	}

}

@Service
class IcebergService {
	public void setupTable() {
    // create a catalog – e.g., HiveCatalog
    HiveCatalog catalog = new HiveCatalog();
    // configure properties
    Map<String, String> properties = new HashMap<>();
    properties.put("warehouse", "s3://my-bucket/warehouse");  // example
    properties.put("uri", "thrift://mymetastore:9083");       // example

    catalog.initialize("my_catalog", properties);

    Schema schema = new Schema(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.required(2, "data", Types.StringType.get())
    );

    TableIdentifier tableId = TableIdentifier.of("my_catalog", "default", "my_table");

    Table table = catalog.createTable(tableId, schema);
    // … now you can write/read using Iceberg API
	}
}
