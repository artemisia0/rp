package io.github.artemisia0.rp.dto;

import java.util.List;

public class SnapshotDiffDto {
    private long snapshotId;
    private List<String> addedFiles;
    private List<String> removedFiles;

    public SnapshotDiffDto() {}

    public SnapshotDiffDto(long snapshotId, List<String> addedFiles, List<String> removedFiles) {
        this.snapshotId = snapshotId;
        this.addedFiles = addedFiles;
        this.removedFiles = removedFiles;
    }

    public long getSnapshotId() { return snapshotId; }
    public List<String> getAddedFiles() { return addedFiles; }
    public List<String> getRemovedFiles() { return removedFiles; }
}
