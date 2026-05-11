package com.hft.execution.dedup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BloomFilterDedupTest {

    @Test
    void newId_isNotDuplicate() {
        BloomFilterDedup dedup = new BloomFilterDedup();
        assertFalse(dedup.isDuplicate("client-new-1"));
    }

    @Test
    void seenId_isDuplicate() {
        BloomFilterDedup dedup = new BloomFilterDedup();
        dedup.markSeen("client-1");
        assertTrue(dedup.isDuplicate("client-1"));
    }

    @Test
    void differentIds_areNotDuplicates() {
        BloomFilterDedup dedup = new BloomFilterDedup();
        dedup.markSeen("client-a");
        assertFalse(dedup.isDuplicate("client-b"));
    }

    @Test
    void multipleIds_trackedIndependently() {
        BloomFilterDedup dedup = new BloomFilterDedup();
        dedup.markSeen("client-x");
        dedup.markSeen("client-y");
        assertTrue(dedup.isDuplicate("client-x"));
        assertTrue(dedup.isDuplicate("client-y"));
        assertFalse(dedup.isDuplicate("client-z"));
    }
}
