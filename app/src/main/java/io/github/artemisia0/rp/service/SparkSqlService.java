package io.github.artemisia0.rp.service;

import io.github.artemisia0.rp.dto.IcebergConnectionDto;
import io.github.artemisia0.rp.dto.SqlResponse;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class SparkSqlService {

    private final IcebergConnectionService connectionService;
    private SparkSession spark;

    public SparkSqlService(IcebergConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @PostConstruct
    public synchronized void init() {
        // Workaround for Java 17+ compatibility with Hadoop
        System.setProperty("HADOOP_HOME", "/tmp/hadoop");
        applyConnection(connectionService.getCurrent());
    }

    @PreDestroy
    public synchronized void cleanup() {
        if (spark != null) {
            spark.stop();
        }
    }

    public synchronized void applyConnection(IcebergConnectionDto connection) {
        if (spark != null) {
            spark.stop();
        }

        String catalog = connection.getCatalog();
        String warehouse = connection.getWarehouse();

        spark = SparkSession.builder()
            .appName("IcebergDataViz")
            .master("local[*]")
            .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
            .config("spark.sql.catalog." + catalog, "org.apache.iceberg.spark.SparkCatalog")
            .config("spark.sql.catalog." + catalog + ".type", "hadoop")
            .config("spark.sql.catalog." + catalog + ".warehouse", warehouse)
            .config("spark.sql.defaultCatalog", catalog)
            .config("spark.ui.enabled", "false")
            .config("spark.sql.warehouse.dir", warehouse)
            .config("spark.hadoop.fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem")
            .config("spark.hadoop.fs.file.impl.disable.cache", "true")
            .getOrCreate();
    }

    public synchronized SqlResponse runSql(String sql) {
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
