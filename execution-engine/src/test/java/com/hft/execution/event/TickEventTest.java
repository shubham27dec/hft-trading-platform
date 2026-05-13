package com.hft.execution.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TickEventTest {

    @Test
    void defaultValues_areZeroAndNull() {
        TickEvent event = new TickEvent();
        assertNull(event.symbol);
        assertEquals(0.0, event.ask);
        assertEquals(0.0, event.bid);
        assertEquals(0.0, event.last);
        assertEquals(0L, event.volume);
        assertEquals(0L, event.timestamp);
    }

    @Test
    void reset_clearsAllFields() {
        TickEvent event = new TickEvent();
        event.symbol = "AAPL";
        event.ask = 150.5;
        event.bid = 150.3;
        event.last = 150.4;
        event.volume = 10000L;
        event.timestamp = 123456789L;

        event.reset();

        assertNull(event.symbol);
        assertEquals(0.0, event.ask);
        assertEquals(0.0, event.bid);
        assertEquals(0.0, event.last);
        assertEquals(0L, event.volume);
        assertEquals(0L, event.timestamp);
    }
}
