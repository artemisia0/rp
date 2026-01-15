package io.github.artemisia0.rp.dto;

import java.util.List;

public class SnapshotDiffDto {
    private long snapshotId;
    private List<String> addedFiles;
    private List<String> removedFiles;
    private List<RowChangeDto> rowChanges;

    public SnapshotDiffDto() {}

    public SnapshotDiffDto(long snapshotId, List<String> addedFiles, List<String> removedFiles, List<RowChangeDto> rowChanges) {
        this.snapshotId = snapshotId;
        this.addedFiles = addedFiles;
        this.removedFiles = removedFiles;
        this.rowChanges = rowChanges;
    }

    public long getSnapshotId() { return snapshotId; }
    public List<String> getAddedFiles() { return addedFiles; }
    public List<String> getRemovedFiles() { return removedFiles; }
    public List<RowChangeDto> getRowChanges() { return rowChanges; }
}

