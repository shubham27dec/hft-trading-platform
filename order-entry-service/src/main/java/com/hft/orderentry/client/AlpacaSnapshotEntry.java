package com.hft.orderentry.client;

import lombok.Data;

@Data
public class AlpacaSnapshotEntry {

    private Quote latestQuote;

    @Data
    public static class Quote {
        private double ap; // ask price
        private double bp; // bid price
        private long as;   // ask size
        private long bs;   // bid size
        private String t;  // timestamp ISO-8601
    }
}
