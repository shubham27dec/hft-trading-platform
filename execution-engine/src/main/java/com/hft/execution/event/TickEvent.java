package com.hft.execution.event;

public class TickEvent {

    public String symbol;
    public double ask;
    public double bid;
    public double last;
    public long volume;
    public long timestamp;

    public void reset() {
        symbol = null;
        ask = 0;
        bid = 0;
        last = 0;
        volume = 0;
        timestamp = 0;
    }
}
