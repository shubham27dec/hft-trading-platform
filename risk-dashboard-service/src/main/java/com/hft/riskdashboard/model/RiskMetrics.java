package com.hft.riskdashboard.model;

import lombok.Data;

@Data
public class RiskMetrics {
    private String accountId;
    private long fillCount;
    private long rejectCount;
    private double grossExposure;
    private boolean haltActive;

    public synchronized void recordFill(long qty, double price, boolean halted) {
        fillCount++;
        grossExposure += Math.abs(qty * price);
        haltActive = halted;
    }

    public synchronized void recordRejection(boolean halted) {
        rejectCount++;
        haltActive = halted;
    }

    public synchronized void refreshHaltStatus(boolean halted) {
        haltActive = halted;
    }
}
