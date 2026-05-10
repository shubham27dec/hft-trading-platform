package com.hft.riskdashboard.state;

import com.hft.riskdashboard.model.RiskMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskStateTest {

    private RiskState state;

    @BeforeEach
    void setUp() {
        state = new RiskState();
    }

    @Test
    void getOrCreate_newAccount_returnsInitialisedMetrics() {
        RiskMetrics m = state.getOrCreate("acc-1");

        assertEquals("acc-1", m.getAccountId());
        assertEquals(0, m.getFillCount());
        assertEquals(0, m.getRejectCount());
        assertEquals(0.0, m.getGrossExposure());
    }

    @Test
    void getOrCreate_sameAccount_returnsSameInstance() {
        RiskMetrics first  = state.getOrCreate("acc-1");
        RiskMetrics second = state.getOrCreate("acc-1");

        assertSame(first, second);
    }

    @Test
    void getAll_returnsAllStoredMetrics() {
        state.getOrCreate("acc-1");
        state.getOrCreate("acc-2");

        List<RiskMetrics> all = state.getAll();

        assertEquals(2, all.size());
    }

    @Test
    void isHalted_initiallyFalse() {
        assertFalse(state.isHalted());
    }

    @Test
    void simulatePartition_setsHaltTrue() {
        state.simulatePartition();

        assertTrue(state.isHalted());
    }

    @Test
    void restore_clearsHalt() {
        state.simulatePartition();
        state.restore();

        assertFalse(state.isHalted());
    }
}
