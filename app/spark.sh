./spark-3.5.7-bin-hadoop3-scala2.13/bin/spark-sql \
      --packages org.apache.iceberg:iceberg-spark-runtime-3.5_2.13:1.4.2,org.scala-lang:scala-library:2.13.12 \
      --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
      --conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog \
      --conf spark.sql.catalog.local.type=hadoop \
      --conf spark.sql.catalog.local.warehouse=file:/home/boss/skola/semester_3/rp/app/warehouse
