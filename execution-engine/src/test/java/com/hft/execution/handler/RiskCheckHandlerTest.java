package com.hft.execution.handler;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import com.hft.execution.dedup.BloomFilterDedup;
import com.hft.execution.event.OrderEvent;
import com.hft.execution.event.OrderEventFactory;
import com.hft.execution.risk.HaltBit;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskCheckHandlerTest {

    private HaltBit haltBit;
    private BloomFilterDedup dedup;
    private RiskCheckHandler handler;
    private Disruptor<OrderEvent> routingDisruptor;
    private List<OrderEvent> captured;

    @BeforeEach
    void setUp() {
        haltBit = new HaltBit();
        dedup = new BloomFilterDedup();
        captured = new ArrayList<>();

        // Real Disruptor #3 ring buffer — capture handler records what arrives
        routingDisruptor = new Disruptor<>(new OrderEventFactory(), 64, DaemonThreadFactory.INSTANCE);
        routingDisruptor.handleEventsWith((event, seq, eob) -> {
            OrderEvent copy = new OrderEvent();
            copy.copyFrom(event);
            captured.add(copy);
        });
        routingDisruptor.start();

        handler = new RiskCheckHandler(haltBit, dedup, routingDisruptor.getRingBuffer());
    }

    @AfterEach
    void tearDown() {
        routingDisruptor.shutdown();
    }

    @Test
    void validOrder_passesAndForwardsToRouting() throws Exception {
        OrderEvent event = buildEvent("order-1", "client-1", 100);
        handler.onEvent(event, 0, false);

        assertTrue(event.riskPassed);
        Thread.sleep(50);
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).riskPassed);
        assertEquals("order-1", captured.get(0).orderId);
    }

    @Test
    void haltedBit_rejectsAndForwardsToRouting() throws Exception {
        haltBit.halt("test halt");
        OrderEvent event = buildEvent("order-2", "client-2", 100);
        handler.onEvent(event, 0, false);

        assertFalse(event.riskPassed);
        assertEquals("Trading halted", event.rejectionReason);
        Thread.sleep(50);
        assertEquals(1, captured.size());
        assertFalse(captured.get(0).riskPassed);
    }

    @Test
    void duplicateClientOrderId_rejectsSecondAndBothForwarded() throws Exception {
        handler.onEvent(buildEvent("order-3", "client-dup", 100), 0, false);
        handler.onEvent(buildEvent("order-4", "client-dup", 100), 1, false);

        Thread.sleep(50);
        assertEquals(2, captured.size());
        assertTrue(captured.get(0).riskPassed);
        assertFalse(captured.get(1).riskPassed);
        assertTrue(captured.get(1).rejectionReason.contains("Duplicate"));
    }

    @Test
    void zeroQuantity_rejectsOrder() throws Exception {
        handler.onEvent(buildEvent("order-5", "client-5", 0), 0, false);

        Thread.sleep(50);
        assertEquals(1, captured.size());
        assertFalse(captured.get(0).riskPassed);
        assertTrue(captured.get(0).rejectionReason.contains("quantity"));
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
