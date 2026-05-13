package com.hft.execution.dedup;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.StandardCharsets;

public class BloomFilterDedup {

    // 1M expected insertions, 0.01% false positive rate
    private final BloomFilter<String> filter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8), 1_000_000, 0.0001);

    public boolean isDuplicate(String clientOrderId) {
        return filter.mightContain(clientOrderId);
    }

    public void markSeen(String clientOrderId) {
        filter.put(clientOrderId);
    }
}
