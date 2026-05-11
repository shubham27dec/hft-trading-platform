package com.hft.execution.feed;

import com.hft.execution.venue.VenueQuote;

import java.util.concurrent.ConcurrentHashMap;

public class PriceCache {

    private final ConcurrentHashMap<String, VenueQuote> cache = new ConcurrentHashMap<>();

    public void update(String symbol, double ask, double bid) {
        cache.put(symbol, new VenueQuote(ask, bid));
    }

    public VenueQuote get(String symbol) {
        return cache.get(symbol);
    }

    public boolean contains(String symbol) {
        return cache.containsKey(symbol);
    }

    public int size() {
        return cache.size();
    }
}
