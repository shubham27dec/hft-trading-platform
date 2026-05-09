package com.hft.core.model;

import lombok.Data;

@Data
public class Position {
    private String accountId;
    private String symbol;
    private long netQty;
    private double avgCostBasis;
    private double unrealizedPnL;
    private double realizedPnL;
    private long lastUpdatedAt;
}
