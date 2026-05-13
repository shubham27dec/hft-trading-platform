package com.hft.execution.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.execution.event.TickEvent;
import com.hft.execution.event.TickEventFactory;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FeedHandlerTest {

    private Disruptor<TickEvent> tickDisruptor;
    private BlockingQueue<TickEvent> captured;
    private FeedHandler handler;

    @BeforeEach
    void setUp() {
        captured = new LinkedBlockingQueue<>();
        tickDisruptor = new Disruptor<>(new TickEventFactory(), 64, DaemonThreadFactory.INSTANCE);
        tickDisruptor.handleEventsWith((event, seq, eob) -> {
            TickEvent copy = new TickEvent();
            copy.symbol = event.symbol;
            copy.ask = event.ask;
            copy.bid = event.bid;
            captured.add(copy);
        });
        tickDisruptor.start();

        handler = new FeedHandler(tickDisruptor.getRingBuffer(), new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        tickDisruptor.shutdown();
    }

    @Test
    void handleMessage_quoteMessage_publishesTickEventToDisruptor() throws Exception {
        handler.handleMessage("[{\"T\":\"q\",\"S\":\"AAPL\",\"ap\":150.5,\"bp\":150.3}]");

        TickEvent tick = captured.poll(1, TimeUnit.SECONDS);
        assertNotNull(tick);
        assertEquals("AAPL", tick.symbol);
        assertEquals(150.5, tick.ask, 0.001);
        assertEquals(150.3, tick.bid, 0.001);
    }

    @Test
    void handleMessage_multipleQuotes_publishesAllToDisruptor() throws Exception {
        handler.handleMessage("[" +
                "{\"T\":\"q\",\"S\":\"AAPL\",\"ap\":150.0,\"bp\":149.9}," +
                "{\"T\":\"q\",\"S\":\"TSLA\",\"ap\":200.0,\"bp\":199.5}" +
                "]");

        assertNotNull(captured.poll(1, TimeUnit.SECONDS));
        assertNotNull(captured.poll(1, TimeUnit.SECONDS));
        assertTrue(captured.isEmpty());
    }

    @Test
    void handleMessage_zeroAskAndBid_doesNotPublish() throws Exception {
        handler.handleMessage("[{\"T\":\"q\",\"S\":\"AAPL\",\"ap\":0,\"bp\":0}]");

        assertNull(captured.poll(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void handleMessage_nonQuoteType_ignored() throws Exception {
        handler.handleMessage("[{\"T\":\"t\",\"S\":\"AAPL\",\"ap\":150.0,\"bp\":149.9}]");

        assertNull(captured.poll(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void handleMessage_malformedJson_doesNotThrow() {
        assertDoesNotThrow(() -> handler.handleMessage("not json at all"));
    }

    @Test
    void handleMessage_emptyArray_doesNothing() throws Exception {
        assertDoesNotThrow(() -> handler.handleMessage("[]"));
        assertNull(captured.poll(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void handleMessage_nonArrayJson_ignored() throws Exception {
        assertDoesNotThrow(() -> handler.handleMessage("{\"T\":\"q\"}"));
        assertNull(captured.poll(200, TimeUnit.MILLISECONDS));
    }
}
