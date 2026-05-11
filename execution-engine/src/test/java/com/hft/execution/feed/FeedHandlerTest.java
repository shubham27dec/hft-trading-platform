package com.hft.execution.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.execution.event.TickEvent;
import com.hft.execution.event.TickEventFactory;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FeedHandlerTest {

    @Mock HttpClient http;

    private Disruptor<TickEvent> tickDisruptor;
    private List<TickEvent> captured;
    private FeedHandler handler;

    @BeforeEach
    void setUp() {
        captured = new ArrayList<>();
        tickDisruptor = new Disruptor<>(new TickEventFactory(), 64, DaemonThreadFactory.INSTANCE);
        tickDisruptor.handleEventsWith((event, seq, eob) -> {
            TickEvent copy = new TickEvent();
            copy.symbol = event.symbol;
            copy.ask = event.ask;
            copy.bid = event.bid;
            captured.add(copy);
        });
        tickDisruptor.start();

        handler = new FeedHandler("key", "secret", Set.of("AAPL", "TSLA"),
                tickDisruptor.getRingBuffer(), http, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        tickDisruptor.shutdown();
    }

    @Test
    void handleMessage_quoteMessage_publishesTickEventToDisruptor() throws Exception {
        handler.handleMessage("[{\"T\":\"q\",\"S\":\"AAPL\",\"ap\":150.5,\"bp\":150.3}]");

        Thread.sleep(50);
        assertEquals(1, captured.size());
        assertEquals("AAPL", captured.get(0).symbol);
        assertEquals(150.5, captured.get(0).ask, 0.001);
        assertEquals(150.3, captured.get(0).bid, 0.001);
    }

    @Test
    void handleMessage_multipleQuotes_publishesAllToDisruptor() throws Exception {
        handler.handleMessage("[" +
                "{\"T\":\"q\",\"S\":\"AAPL\",\"ap\":150.0,\"bp\":149.9}," +
                "{\"T\":\"q\",\"S\":\"TSLA\",\"ap\":200.0,\"bp\":199.5}" +
                "]");

        Thread.sleep(50);
        assertEquals(2, captured.size());
    }

    @Test
    void handleMessage_zeroAskAndBid_doesNotPublish() throws Exception {
        handler.handleMessage("[{\"T\":\"q\",\"S\":\"AAPL\",\"ap\":0,\"bp\":0}]");

        Thread.sleep(50);
        assertEquals(0, captured.size());
    }

    @Test
    void handleMessage_nonQuoteType_ignored() throws Exception {
        handler.handleMessage("[{\"T\":\"t\",\"S\":\"AAPL\",\"ap\":150.0,\"bp\":149.9}]");

        Thread.sleep(50);
        assertEquals(0, captured.size());
    }

    @Test
    void handleMessage_malformedJson_doesNotThrow() {
        assertDoesNotThrow(() -> handler.handleMessage("not json at all"));
    }

    @Test
    void handleMessage_emptyArray_doesNothing() throws Exception {
        assertDoesNotThrow(() -> handler.handleMessage("[]"));
        Thread.sleep(50);
        assertEquals(0, captured.size());
    }

    @Test
    void isRunning_beforeStart_returnsFalse() {
        assertFalse(handler.isRunning());
    }
}
