package com.hft.core.model;

import lombok.Data;

@Data
public class OrderBook {
    private String symbol;
    private double bestBid;
    private double bestAsk;
    private long bidSize;
    private long askSize;
}
