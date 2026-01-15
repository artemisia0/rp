package io.github.artemisia0.rp.dto;

import java.util.Map;

public class RowChangeDto {
    private String type; // ADDED, REMOVED, UPDATED
    private Map<String, Object> before;
    private Map<String, Object> after;

    public RowChangeDto() {}

    public RowChangeDto(String type, Map<String, Object> before, Map<String, Object> after) {
        this.type = type;
        this.before = before;
        this.after = after;
    }

    public String getType() { return type; }
    public Map<String, Object> getBefore() { return before; }
    public Map<String, Object> getAfter() { return after; }
}
