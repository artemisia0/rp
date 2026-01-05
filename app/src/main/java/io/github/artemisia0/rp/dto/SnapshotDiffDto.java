package io.github.artemisia0.rp.dto;

public class SnapshotDiffDto {
    private long snapshotId;
    private int addedFiles;
    private int removedFiles;

    public SnapshotDiffDto() {}

    public SnapshotDiffDto(long snapshotId, int addedFiles, int removedFiles) {
        this.snapshotId = snapshotId;
        this.addedFiles = addedFiles;
        this.removedFiles = removedFiles;
    }

    public long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(long snapshotId) { this.snapshotId = snapshotId; }
    public int getAddedFiles() { return addedFiles; }
    public void setAddedFiles(int addedFiles) { this.addedFiles = addedFiles; }
    public int getRemovedFiles() { return removedFiles; }
    public void setRemovedFiles(int removedFiles) { this.removedFiles = removedFiles; }
}
