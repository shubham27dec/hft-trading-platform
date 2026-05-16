package com.hft.execution.dedup;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class SymbolWatchlist {

    // Bloom Filter for fast-reject: if mightContain returns false,
    // the symbol is DEFINITELY not watched — reject immediately without further processing.
    // False positives (unknown symbol passes through) are acceptable — risk handler catches those.
    private final BloomFilter<String> filter;

    public SymbolWatchlist(Set<String> watchedSymbols) {
        // Size for 10_000 expected symbols, 0.1% false positive rate
        filter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 10_000, 0.001);
        watchedSymbols.forEach(filter::put);
    }

    public boolean isWatched(String symbol) {
        return filter.mightContain(symbol);
    }

    public void addSymbol(String symbol) {
        filter.put(symbol);
    }
}
