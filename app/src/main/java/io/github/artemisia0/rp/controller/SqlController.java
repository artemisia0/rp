package io.github.artemisia0.rp.controller;

import io.github.artemisia0.rp.dto.SqlRequest;
import io.github.artemisia0.rp.dto.SqlResponse;
import io.github.artemisia0.rp.service.IcebergService;
import io.github.artemisia0.rp.service.SparkSqlService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class SqlController {

    private final SparkSqlService sparkSqlService;
    private final IcebergService icebergService;

    public SqlController(SparkSqlService sparkSqlService, IcebergService icebergService) {
        this.sparkSqlService = sparkSqlService;
        this.icebergService = icebergService;
    }

    @PostMapping("/sql")
    public SqlResponse runSql(@RequestBody SqlRequest request) {
        SqlResponse response = sparkSqlService.runSql(request.getSql());
        // Always reload catalog after SQL execution, as snapshots may be created
        // even if spark-sql exits with non-zero code (warnings, etc.)
        icebergService.reloadCatalog();
        return response;
    }
}
