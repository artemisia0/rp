package io.github.artemisia0.rp.dto;

public class IcebergConnectionDto {

    private String catalog;
    private String warehouse;
    private String namespace;
    private String table;

    public IcebergConnectionDto() {
    }

    public IcebergConnectionDto(String catalog, String warehouse, String namespace, String table) {
        this.catalog = catalog;
        this.warehouse = warehouse;
        this.namespace = namespace;
        this.table = table;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
}
