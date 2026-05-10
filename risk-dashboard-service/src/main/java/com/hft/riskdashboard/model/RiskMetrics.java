package com.hft.riskdashboard.model;

import lombok.Data;

@Data
public class RiskMetrics {
    private String accountId;
    private long fillCount;
    private long rejectCount;
    private double grossExposure;   // sum of |filledQty * fillPrice| across fills
    private boolean haltActive;     // reflects global halt bit at time of snapshot
}
