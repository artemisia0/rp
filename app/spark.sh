#!/bin/bash

# Set warehouse directory (absolute path recommended)
WAREHOUSE_DIR="$(cd "$(dirname "$0")" && pwd)/warehouse"
mkdir -p "$WAREHOUSE_DIR"

~/Downloads/spark-3.5.7-bin-hadoop3-scala2.13/bin/spark-sql \
  --packages org.apache.iceberg:iceberg-spark-runtime-3.3_2.12:1.5.0,org.apache.hadoop:hadoop-client:3.4.2,org.apache.avro:avro:1.11.4 \
  --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
  --conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog \
  --conf spark.sql.catalog.local.type=hadoop \
  --conf spark.sql.catalog.local.warehouse="file://$WAREHOUSE_DIR" \
  -e "\
    CREATE DATABASE IF NOT EXISTS local.db;\
    CREATE TABLE IF NOT EXISTS local.db.my_table (id INT, data STRING) USING iceberg;\
    INSERT INTO local.db.my_table VALUES (1, 'a'), (2, 'b');\
    UPDATE local.db.my_table SET data = 'c' WHERE id = 1;\
    DELETE FROM local.db.my_table WHERE id = 2;\
  "

