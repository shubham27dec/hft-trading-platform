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
        riskState.getOrCreate(event.getAccountId())
                .recordFill(event.getFilledQty(), event.getFillPrice(), riskState.isHalted());
    }

    public void processRejection(OrderRejectedEvent event) {
        riskState.getOrCreate(event.getAccountId())
                .recordRejection(riskState.isHalted());
    }

    public RiskMetrics getSnapshot(String accountId) {
        RiskMetrics m = riskState.getOrCreate(accountId);
        m.refreshHaltStatus(riskState.isHalted());
        return m;
    }

    public List<RiskMetrics> getAllSnapshots() {
        List<RiskMetrics> all = riskState.getAll();
        boolean halted = riskState.isHalted();
        all.forEach(m -> m.refreshHaltStatus(halted));
        return all;
    }

    public void simulatePartition() {
        riskState.simulatePartition();
    }

    public void restore() {
        riskState.restore();
    }
}
