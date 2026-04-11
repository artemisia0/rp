package io.github.artemisia0.rp.controller;

import io.github.artemisia0.rp.dto.IcebergConnectionDto;
import io.github.artemisia0.rp.dto.IcebergConnectionStateDto;
import io.github.artemisia0.rp.service.IcebergConnectionService;
import io.github.artemisia0.rp.service.IcebergService;
import io.github.artemisia0.rp.service.SparkSqlService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class ConnectionController {

    private final IcebergConnectionService connectionService;
    private final SparkSqlService sparkSqlService;
    private final IcebergService icebergService;

    public ConnectionController(
        IcebergConnectionService connectionService,
        SparkSqlService sparkSqlService,
        IcebergService icebergService
    ) {
        this.connectionService = connectionService;
        this.sparkSqlService = sparkSqlService;
        this.icebergService = icebergService;
    }

    @GetMapping("/connection")
    public IcebergConnectionStateDto connection() {
        return new IcebergConnectionStateDto(
            connectionService.getCurrent(),
            connectionService.getDefaults()
        );
    }

    @PutMapping("/connection")
    public IcebergConnectionDto updateConnection(@RequestBody IcebergConnectionDto request) {
        IcebergConnectionDto updated = connectionService.update(request);
        sparkSqlService.applyConnection(updated);
        icebergService.applyConnection(updated);
        return updated;
    }

    @PostMapping("/connection/initialize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void initializeConnection() {
        icebergService.initializeIfMissing();
    }
}
