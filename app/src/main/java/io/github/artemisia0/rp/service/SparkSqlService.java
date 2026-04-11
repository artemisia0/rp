package io.github.artemisia0.rp.service;

import io.github.artemisia0.rp.dto.IcebergConnectionDto;
import io.github.artemisia0.rp.dto.SqlResponse;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Locale;
import java.util.Set;

@Service
public class SparkSqlService {

    private static final Set<String> ALLOWED_SQL_COMMANDS = Set.of("SELECT", "INSERT", "UPDATE", "DELETE");
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
            int statementIndex = 0;
            
            // Split by semicolon to handle multiple statements
            String[] statements = sql.split(";");
            
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) continue;
                statementIndex++;

                String keyword = extractLeadingKeyword(trimmed);
                if (!ALLOWED_SQL_COMMANDS.contains(keyword)) {
                    String resolvedKeyword = keyword.isEmpty() ? "UNKNOWN" : keyword;
                    String message = "Only SELECT, INSERT, UPDATE, and DELETE are allowed. "
                        + "Statement " + statementIndex + " starts with: " + resolvedKeyword;
                    output.append("Error executing statement: ").append(message).append("\n");
                    return new SqlResponse(false, 1, output.toString(), message);
                }
                
                try {
                    var result = spark.sql(trimmed);
                    
                    // If it's a query (SELECT), collect results
                    if ("SELECT".equals(keyword)) {
                        String resultStr = result.showString(100, 20, false);
                        output.append(resultStr).append("\n");
                    } else {
                        // For non-query statements, just acknowledge execution
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

    private String extractLeadingKeyword(String statement) {
        String remaining = statement;
        while (true) {
            remaining = remaining.stripLeading();
            if (remaining.startsWith("--")) {
                int nextLine = remaining.indexOf('\n');
                if (nextLine < 0) {
                    return "";
                }
                remaining = remaining.substring(nextLine + 1);
                continue;
            }
            if (remaining.startsWith("/*")) {
                int commentEnd = remaining.indexOf("*/", 2);
                if (commentEnd < 0) {
                    return "";
                }
                remaining = remaining.substring(commentEnd + 2);
                continue;
            }
            break;
        }

        int wordEnd = 0;
        while (wordEnd < remaining.length() && Character.isLetter(remaining.charAt(wordEnd))) {
            wordEnd++;
        }
        if (wordEnd == 0) {
            return "";
        }
        return remaining.substring(0, wordEnd).toUpperCase(Locale.ROOT);
    }
}
