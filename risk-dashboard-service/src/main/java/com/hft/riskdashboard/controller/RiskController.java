package com.hft.riskdashboard.controller;

import com.hft.riskdashboard.model.RiskMetrics;
import com.hft.riskdashboard.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping
    public ResponseEntity<List<RiskMetrics>> getAllSnapshots() {
        return ResponseEntity.ok(riskService.getAllSnapshots());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<RiskMetrics> getSnapshot(@PathVariable String accountId) {
        return ResponseEntity.ok(riskService.getSnapshot(accountId));
    }

    @PostMapping("/simulate-partition")
    public ResponseEntity<Void> simulatePartition() {
        riskService.simulatePartition();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restore")
    public ResponseEntity<Void> restore() {
        riskService.restore();
        return ResponseEntity.ok().build();
    }
}
