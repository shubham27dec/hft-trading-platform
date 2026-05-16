package com.hft.orderentry.client;

import lombok.Data;

@Data
public class AlpacaSnapshotEntry {

    private Quote latestQuote;
    private Trade latestTrade;
    private DailyBar dailyBar;

    @Data
    public static class Quote {
        private double ap; // ask price
        private double bp; // bid price
        private long as;   // ask size
        private long bs;   // bid size
        private String t;  // timestamp ISO-8601
    }

    @Data
    public static class Trade {
        private double p; // last trade price
        private long s;   // size
    }

    @Data
    public static class DailyBar {
        private double c; // close price
        private double o; // open
        private double h; // high
        private double l; // low
        private long v;   // volume
    }
}
