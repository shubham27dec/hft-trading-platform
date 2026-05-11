package com.hft.execution.venue;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import com.hft.execution.event.OrderEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulatedExecutionVenueTest {

    @Test
    void name_returnsSimulated() {
        SimulatedExecutionVenue venue = new SimulatedExecutionVenue(mockSource(150.0, 149.9));
        assertEquals("SIMULATED", venue.name());
    }

    @Test
    void getQuote_widensSpreadvsBasisVenue() {
        SimulatedExecutionVenue venue = new SimulatedExecutionVenue(mockSource(150.0, 149.9));
        VenueQuote quote = venue.getQuote("AAPL");
        // ask should be higher, bid should be lower than source
        assertTrue(quote.ask() > 150.0);
        assertTrue(quote.bid() < 149.9);
    }

    @Test
    void execute_buy_fillsAtRoutedAsk() {
        SimulatedExecutionVenue venue = new SimulatedExecutionVenue(mockSource(150.0, 149.9));
        OrderEvent event = buildEvent(OrderSide.BUY, 150.10, 149.90);

        ExecutionResult result = venue.execute(event);

        assertEquals(150.10, result.fillPrice(), 0.001);
        assertEquals(100, result.filledQty());
        assertNotNull(result.fillId());
    }

    @Test
    void execute_sell_fillsAtRoutedBid() {
        SimulatedExecutionVenue venue = new SimulatedExecutionVenue(mockSource(150.0, 149.9));
        OrderEvent event = buildEvent(OrderSide.SELL, 150.10, 149.90);

        ExecutionResult result = venue.execute(event);

        assertEquals(149.90, result.fillPrice(), 0.001);
    }

    private ExecutionVenue mockSource(double ask, double bid) {
        ExecutionVenue source = mock(ExecutionVenue.class);
        when(source.getQuote(anyString())).thenReturn(new VenueQuote(ask, bid));
        return source;
    }

    private OrderEvent buildEvent(OrderSide side, double routedAsk, double routedBid) {
        OrderEvent event = new OrderEvent();
        event.orderId = "order-1";
        event.symbol = "AAPL";
        event.side = side;
        event.type = OrderType.MARKET;
        event.quantity = 100;
        event.routedAsk = routedAsk;
        event.routedBid = routedBid;
        return event;
    }
}
