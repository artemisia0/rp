package io.github.artemisia0.rp.dto;

public class SnapshotDto {
    private long snapshotId;
    private Long parentId;
    private String operation;
    private long timestampMillis;

    public SnapshotDto() {}

    public SnapshotDto(long snapshotId, Long parentId, String operation, long timestampMillis) {
        this.snapshotId = snapshotId;
        this.parentId = parentId;
        this.operation = operation;
        this.timestampMillis = timestampMillis;
    }

    public long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(long snapshotId) { this.snapshotId = snapshotId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
}
