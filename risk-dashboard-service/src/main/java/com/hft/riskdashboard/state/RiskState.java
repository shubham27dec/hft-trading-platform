package com.hft.riskdashboard.state;

import com.hft.riskdashboard.model.RiskMetrics;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RiskState {

    private final ConcurrentHashMap<String, RiskMetrics> metricsMap = new ConcurrentHashMap<>();
    private final AtomicBoolean haltBit = new AtomicBoolean(false);

    public RiskMetrics getOrCreate(String accountId) {
        return metricsMap.computeIfAbsent(accountId, id -> {
            RiskMetrics m = new RiskMetrics();
            m.setAccountId(id);
            return m;
        });
    }

    public List<RiskMetrics> getAll() {
        return new ArrayList<>(metricsMap.values());
    }

    public boolean isHalted() {
        return haltBit.get();
    }

    // PartitionSimulator — used for live CAP demo
    public void simulatePartition() {
        haltBit.set(true);
    }

    public void restore() {
        haltBit.set(false);
    }
}
