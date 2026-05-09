package com.hft.core.model;

import lombok.Data;

@Data
public class Tick {
    private String symbol;
    private double bidPrice;
    private double askPrice;
    private double lastPrice;
    private long volume;
    private long timestamp;
}
