package com.hft.execution.handler;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import com.hft.execution.dedup.BloomFilterDedup;
import com.hft.execution.dedup.SymbolWatchlist;
import com.hft.execution.event.OrderEvent;
import com.hft.execution.event.OrderEventFactory;
import com.hft.execution.risk.HaltBit;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RiskCheckHandlerTest {

    private HaltBit haltBit;
    private BloomFilterDedup dedup;
    private SymbolWatchlist symbolWatchlist;
    private RiskCheckHandler handler;
    private Disruptor<OrderEvent> routingDisruptor;
    private BlockingQueue<OrderEvent> captured;

    @BeforeEach
    void setUp() {
        haltBit = new HaltBit();
        dedup = new BloomFilterDedup();
        symbolWatchlist = new SymbolWatchlist(java.util.Set.of("AAPL", "TSLA", "NVDA", "MSFT", "AMZN"));
        captured = new LinkedBlockingQueue<>();

        routingDisruptor = new Disruptor<>(new OrderEventFactory(), 64, DaemonThreadFactory.INSTANCE);
        routingDisruptor.handleEventsWith((event, seq, eob) -> {
            OrderEvent copy = new OrderEvent();
            copy.copyFrom(event);
            captured.add(copy);
        });
        routingDisruptor.start();

        handler = new RiskCheckHandler(haltBit, dedup, symbolWatchlist, routingDisruptor.getRingBuffer());
    }

    @AfterEach
    void tearDown() {
        routingDisruptor.shutdown();
    }

    @Test
    void validOrder_passesAndForwardsToRouting() throws Exception {
        handler.onEvent(buildEvent("order-1", "client-1", 100), 0, false);

        OrderEvent forwarded = captured.poll(1, TimeUnit.SECONDS);
        assertNotNull(forwarded);
        assertTrue(forwarded.riskPassed);
        assertEquals("order-1", forwarded.orderId);
    }

    @Test
    void haltedBit_rejectsAndForwardsToRouting() throws Exception {
        haltBit.halt("test halt");
        handler.onEvent(buildEvent("order-2", "client-2", 100), 0, false);

        OrderEvent forwarded = captured.poll(1, TimeUnit.SECONDS);
        assertNotNull(forwarded);
        assertFalse(forwarded.riskPassed);
        assertEquals("Trading halted", forwarded.rejectionReason);
    }

    @Test
    void duplicateClientOrderId_rejectsSecondAndBothForwarded() throws Exception {
        handler.onEvent(buildEvent("order-3", "client-dup", 100), 0, false);
        handler.onEvent(buildEvent("order-4", "client-dup", 100), 1, false);

        OrderEvent first = captured.poll(1, TimeUnit.SECONDS);
        OrderEvent second = captured.poll(1, TimeUnit.SECONDS);
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.riskPassed);
        assertFalse(second.riskPassed);
        assertTrue(second.rejectionReason.contains("Duplicate"));
    }

    @Test
    void zeroQuantity_rejectsOrder() throws Exception {
        handler.onEvent(buildEvent("order-5", "client-5", 0), 0, false);

        OrderEvent forwarded = captured.poll(1, TimeUnit.SECONDS);
        assertNotNull(forwarded);
        assertFalse(forwarded.riskPassed);
        assertTrue(forwarded.rejectionReason.contains("quantity"));
    }

    @Test
    void symbolNotInWatchlist_bloomFilterRejects() throws Exception {
        handler.onEvent(buildEventWithSymbol("order-6", "client-6", 100, "UNKNOWN_SYM"), 0, false);

        OrderEvent forwarded = captured.poll(1, TimeUnit.SECONDS);
        assertNotNull(forwarded);
        assertFalse(forwarded.riskPassed);
        assertTrue(forwarded.rejectionReason.contains("not in watchlist"));
    }

    private OrderEvent buildEventWithSymbol(String orderId, String clientOrderId, long qty, String symbol) {
        OrderEvent event = new OrderEvent();
        event.orderId = orderId;
        event.clientOrderId = clientOrderId;
        event.symbol = symbol;
        event.side = OrderSide.BUY;
        event.type = OrderType.MARKET;
        event.quantity = qty;
        return event;
    }

    private OrderEvent buildEvent(String orderId, String clientOrderId, long qty) {
        OrderEvent event = new OrderEvent();
        event.orderId = orderId;
        event.clientOrderId = clientOrderId;
        event.symbol = "AAPL";
        event.side = OrderSide.BUY;
        event.type = OrderType.MARKET;
        event.quantity = qty;
        return event;
    }
}
