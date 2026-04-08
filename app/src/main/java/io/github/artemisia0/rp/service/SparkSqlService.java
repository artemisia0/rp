package io.github.artemisia0.rp.service;

import io.github.artemisia0.rp.dto.SqlResponse;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;

@Service
public class SparkSqlService {

    private final String warehouse;
    private SparkSession spark;

    public SparkSqlService(
        @Value("${spark.warehouse:}") String warehouse
    ) {
        this.warehouse = warehouse == null || warehouse.isBlank()
            ? "file://" + new File("warehouse").getAbsolutePath()
            : warehouse;
    }

    @PostConstruct
    public void init() {
        // Workaround for Java 17+ compatibility with Hadoop
        System.setProperty("HADOOP_HOME", "/tmp/hadoop");
        
        spark = SparkSession.builder()
            .appName("IcebergDataViz")
            .master("local[*]")
            .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
            .config("spark.sql.catalog.local", "org.apache.iceberg.spark.SparkCatalog")
            .config("spark.sql.catalog.local.type", "hadoop")
            .config("spark.sql.catalog.local.warehouse", warehouse)
            .config("spark.ui.enabled", "false")
            .config("spark.sql.warehouse.dir", warehouse)
            .config("spark.hadoop.fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem")
            .config("spark.hadoop.fs.file.impl.disable.cache", "true")
            .getOrCreate();
        
    }

    @PreDestroy
    public void cleanup() {
        if (spark != null) {
            spark.stop();
        }
    }

    public SqlResponse runSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL is required");
        }

        try {
            StringBuilder output = new StringBuilder();
            
            // Split by semicolon to handle multiple statements
            String[] statements = sql.split(";");
            
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) continue;
                
                try {
                    var result = spark.sql(trimmed);
                    
                    // If it's a query (SELECT), collect results
                    if (trimmed.toLowerCase().startsWith("select") || 
                        trimmed.toLowerCase().startsWith("show") ||
                        trimmed.toLowerCase().startsWith("describe")) {
                        String resultStr = result.showString(100, 20, false);
                        output.append(resultStr).append("\n");
                    } else {
                        // For DML/DDL, just acknowledge execution
                        output.append("OK\n");
                    }
                } catch (Exception e) {
                    output.append("Error executing statement: ").append(e.getMessage()).append("\n");
                    return new SqlResponse(false, 1, output.toString(), e.getMessage());
                }
            }
            
            return new SqlResponse(true, 0, output.toString(), "");
            
        } catch (Exception e) {
            return new SqlResponse(false, 1, "", e.getMessage());
        }
    }
}
