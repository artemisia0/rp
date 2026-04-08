package io.github.artemisia0.rp.controller;

import io.github.artemisia0.rp.dto.SnapshotDiffDto;
import io.github.artemisia0.rp.dto.SnapshotDto;
import io.github.artemisia0.rp.service.IcebergService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<SnapshotDto>> snapshots() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .body(iceberg.listSnapshots());
    }

    @GetMapping("/diffs")
    public ResponseEntity<List<SnapshotDiffDto>> diffs() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .body(iceberg.allDiffs());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh() {
        iceberg.reloadCatalog();
    }
}
