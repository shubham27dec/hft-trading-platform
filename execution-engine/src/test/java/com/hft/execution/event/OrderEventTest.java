package com.hft.execution.event;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderEventTest {

    @Test
    void reset_clearsAllFields() {
        OrderEvent event = populatedEvent();

        event.reset();

        assertNull(event.orderId);
        assertNull(event.clientOrderId);
        assertNull(event.symbol);
        assertNull(event.side);
        assertNull(event.type);
        assertEquals(0L, event.quantity);
        assertEquals(0.0, event.limitPrice);
        assertNull(event.accountId);
        assertEquals(0L, event.submittedAt);
        assertFalse(event.riskPassed);
        assertNull(event.rejectionReason);
        assertNull(event.venue);
        assertEquals(0.0, event.routedAsk);
        assertEquals(0.0, event.routedBid);
        assertNull(event.fillId);
        assertEquals(0.0, event.fillPrice);
        assertEquals(0L, event.filledQty);
        assertEquals(0L, event.filledAt);
        assertFalse(event.filled);
    }

    @Test
    void copyFrom_copiesAllFields() {
        OrderEvent src = populatedEvent();
        OrderEvent dst = new OrderEvent();

        dst.copyFrom(src);

        assertEquals(src.orderId, dst.orderId);
        assertEquals(src.clientOrderId, dst.clientOrderId);
        assertEquals(src.symbol, dst.symbol);
        assertEquals(src.side, dst.side);
        assertEquals(src.type, dst.type);
        assertEquals(src.quantity, dst.quantity);
        assertEquals(src.limitPrice, dst.limitPrice);
        assertEquals(src.accountId, dst.accountId);
        assertEquals(src.submittedAt, dst.submittedAt);
        assertEquals(src.riskPassed, dst.riskPassed);
        assertEquals(src.rejectionReason, dst.rejectionReason);
        assertEquals(src.venue, dst.venue);
        assertEquals(src.routedAsk, dst.routedAsk);
        assertEquals(src.routedBid, dst.routedBid);
        assertEquals(src.fillId, dst.fillId);
        assertEquals(src.fillPrice, dst.fillPrice);
        assertEquals(src.filledQty, dst.filledQty);
        assertEquals(src.filledAt, dst.filledAt);
        assertEquals(src.filled, dst.filled);
    }

    private OrderEvent populatedEvent() {
        OrderEvent event = new OrderEvent();
        event.orderId = "order-1";
        event.clientOrderId = "client-1";
        event.symbol = "AAPL";
        event.side = OrderSide.BUY;
        event.type = OrderType.MARKET;
        event.quantity = 100L;
        event.limitPrice = 150.0;
        event.accountId = "account-1";
        event.submittedAt = 999L;
        event.riskPassed = true;
        event.rejectionReason = "none";
        event.venue = "ALPACA";
        event.routedAsk = 150.1;
        event.routedBid = 149.9;
        event.fillId = "fill-1";
        event.fillPrice = 150.05;
        event.filledQty = 100L;
        event.filledAt = 1000L;
        event.filled = true;
        return event;
    }
}
