package io.github.artemisia0.rp.controller;

import io.github.artemisia0.rp.dto.SnapshotDiffDto;
import io.github.artemisia0.rp.dto.SnapshotDto;
import io.github.artemisia0.rp.service.IcebergService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class SnapshotController {

    private final IcebergService iceberg;

    public SnapshotController(IcebergService iceberg) {
        this.iceberg = iceberg;
    }

    @GetMapping("/snapshots")
    public List<SnapshotDto> snapshots() {
        return iceberg.listSnapshots();
    }

    @GetMapping("/diffs")
    public List<SnapshotDiffDto> diffs() {
        return iceberg.allDiffs();
    }
}
