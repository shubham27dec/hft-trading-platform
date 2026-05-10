package com.hft.riskdashboard.service;

import com.hft.core.event.OrderFilledEvent;
import com.hft.core.event.OrderRejectedEvent;
import com.hft.riskdashboard.model.RiskMetrics;
import com.hft.riskdashboard.state.RiskState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskState riskState;

    public void processFill(OrderFilledEvent event) {
        RiskMetrics m = riskState.getOrCreate(event.getAccountId());
        synchronized (m) {
            m.setFillCount(m.getFillCount() + 1);
            m.setGrossExposure(m.getGrossExposure()
                    + Math.abs(event.getFilledQty() * event.getFillPrice()));
            m.setHaltActive(riskState.isHalted());
        }
    }

    public void processRejection(OrderRejectedEvent event) {
        RiskMetrics m = riskState.getOrCreate(event.getAccountId());
        synchronized (m) {
            m.setRejectCount(m.getRejectCount() + 1);
            m.setHaltActive(riskState.isHalted());
        }
    }

    public RiskMetrics getSnapshot(String accountId) {
        RiskMetrics m = riskState.getOrCreate(accountId);
        synchronized (m) {
            m.setHaltActive(riskState.isHalted());
        }
        return m;
    }

    public List<RiskMetrics> getAllSnapshots() {
        List<RiskMetrics> all = riskState.getAll();
        boolean halted = riskState.isHalted();
        all.forEach(m -> {
            synchronized (m) {
                m.setHaltActive(halted);
            }
        });
        return all;
    }

    public void simulatePartition() {
        riskState.simulatePartition();
    }

    public void restore() {
        riskState.restore();
    }
}
