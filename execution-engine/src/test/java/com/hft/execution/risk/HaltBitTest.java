package com.hft.execution.risk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HaltBitTest {

    @Test
    void initiallyNotHalted() {
        HaltBit haltBit = new HaltBit();
        assertFalse(haltBit.isHalted());
        haltBit.stop();
    }

    @Test
    void manualHalt_setsHaltedTrue() {
        HaltBit haltBit = new HaltBit();
        haltBit.halt("test");
        assertTrue(haltBit.isHalted());
        haltBit.stop();
    }

    @Test
    void resume_clearsHaltedFlag() {
        HaltBit haltBit = new HaltBit();
        haltBit.halt("test");
        assertTrue(haltBit.isHalted());
        haltBit.resume();
        assertFalse(haltBit.isHalted());
        haltBit.stop();
    }

    @Test
    void heartbeat_preventsWatchdogHalt() throws InterruptedException {
        HaltBit haltBit = new HaltBit();
        haltBit.startWatchdog();
        // Send heartbeats for 500ms — watchdog fires every 100ms, 3 misses = 300ms to halt
        for (int i = 0; i < 5; i++) {
            haltBit.heartbeat();
            Thread.sleep(80);
        }
        assertFalse(haltBit.isHalted());
        haltBit.stop();
    }
}
