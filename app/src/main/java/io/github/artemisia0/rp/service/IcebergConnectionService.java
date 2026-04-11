package io.github.artemisia0.rp.service;

import io.github.artemisia0.rp.dto.IcebergConnectionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;

@Service
public class IcebergConnectionService {

    private static final String DEFAULT_CATALOG = "local";
    private static final String DEFAULT_NAMESPACE = "db";
    private static final String DEFAULT_TABLE = "my_table";
    private final String defaultWarehouse;
    private volatile IcebergConnectionDto current;

    public IcebergConnectionService(@Value("${spark.warehouse:}") String configuredWarehouse) {
        this.defaultWarehouse = normalizeWarehouseValue(configuredWarehouse);
        this.current = new IcebergConnectionDto(
            DEFAULT_CATALOG,
            this.defaultWarehouse,
            DEFAULT_NAMESPACE,
            DEFAULT_TABLE
        );
    }

    public IcebergConnectionDto getCurrent() {
        IcebergConnectionDto value = this.current;
        return new IcebergConnectionDto(
            value.getCatalog(),
            value.getWarehouse(),
            value.getNamespace(),
            value.getTable()
        );
    }

    public IcebergConnectionDto getDefaults() {
        return new IcebergConnectionDto(DEFAULT_CATALOG, defaultWarehouse, DEFAULT_NAMESPACE, DEFAULT_TABLE);
    }

    public synchronized IcebergConnectionDto update(IcebergConnectionDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Connection payload is required");
        }

        String catalog = normalizeRequired(request.getCatalog(), "catalog");
        String warehouse = normalizeWarehouseValue(request.getWarehouse());
        String namespace = normalizeRequired(request.getNamespace(), "namespace");
        String table = normalizeRequired(request.getTable(), "table");

        this.current = new IcebergConnectionDto(catalog, warehouse, namespace, table);
        return getCurrent();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeWarehouseValue(String rawWarehouse) {
        String candidate = rawWarehouse == null ? "" : rawWarehouse.trim();
        if (candidate.isEmpty()) {
            return new File("warehouse").getAbsoluteFile().toURI().toString();
        }
        if (candidate.contains("://") || candidate.startsWith("file:")) {
            return candidate;
        }
        return new File(candidate).getAbsoluteFile().toURI().toString();
    }
}
