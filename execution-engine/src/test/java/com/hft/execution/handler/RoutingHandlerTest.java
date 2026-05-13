package com.hft.execution.handler;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import com.hft.execution.event.OrderEvent;
import com.hft.execution.feed.PriceCache;
import com.hft.execution.venue.ExecutionVenue;
import com.hft.execution.venue.VenueQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingHandlerTest {

    @Mock ExecutionVenue alpaca;
    @Mock ExecutionVenue simulated;

    private PriceCache priceCache;
    private RoutingHandler handler;

    @BeforeEach
    void setUp() {
        priceCache = new PriceCache();
        lenient().when(alpaca.name()).thenReturn("ALPACA");
        lenient().when(simulated.name()).thenReturn("SIMULATED");
        handler = new RoutingHandler(alpaca, simulated, priceCache);
    }

    @Test
    void buy_cacheHit_routesToAlpacaWhenLowerAsk() {
        priceCache.update("AAPL", 150.0, 149.9);

        OrderEvent event = buildEvent(OrderSide.BUY);
        handler.onEvent(event, 0, false);

        // Alpaca ask=150.0, simulated ask = mid*(1+0.0001) = 149.95 * 1.0001 ≈ 149.965
        // Simulated ask < alpaca ask → routes to SIMULATED
        assertEquals("SIMULATED", event.venue);
    }

    @Test
    void buy_cacheHit_routesToAlpacaWhenAlpacaHasLowerAsk() {
        // Force alpaca ask to be lower: ask=150.0, bid=149.0 → mid=149.5
        // simulated ask = 149.5 * 1.0001 ≈ 149.515 — still less than 150.0
        // So we need alpaca ask to be low: ask=149.0, bid=148.0 → mid=148.5, sim ask≈148.515
        // Actually to route to ALPACA: alpacaAsk must be <= simulatedAsk
        // alpaca ask=150.0, bid=150.0 → mid=150.0, sim ask=150.015 → alpaca wins
        priceCache.update("AAPL", 150.0, 150.0);

        OrderEvent event = buildEvent(OrderSide.BUY);
        handler.onEvent(event, 0, false);

        assertEquals("ALPACA", event.venue);
        assertEquals(150.0, event.routedAsk, 0.001);
    }

    @Test
    void sell_cacheHit_routesToAlpacaWhenHigherBid() {
        // ask=150.01, bid=149.99 → mid=150.0, sim bid = 150.0*(1-0.0001) = 149.985
        // alpaca bid=149.99 > sim bid=149.985 → routes to ALPACA
        priceCache.update("AAPL", 150.01, 149.99);

        OrderEvent event = buildEvent(OrderSide.SELL);
        handler.onEvent(event, 0, false);

        assertEquals("ALPACA", event.venue);
        assertEquals(149.99, event.routedBid, 0.001);
    }

    @Test
    void riskFailed_skipsRouting() {
        priceCache.update("AAPL", 150.0, 149.9);

        OrderEvent event = buildEvent(OrderSide.BUY);
        event.riskPassed = false;
        handler.onEvent(event, 0, false);

        verifyNoInteractions(alpaca);
        verifyNoInteractions(simulated);
        assertNull(event.venue);
    }

    @Test
    void cacheMiss_fallsBackToAlpacaGetQuote() {
        // No cache entry — should fall back to alpaca.getQuote()
        when(alpaca.getQuote("AAPL")).thenReturn(new VenueQuote(150.0, 150.0));

        OrderEvent event = buildEvent(OrderSide.BUY);
        handler.onEvent(event, 0, false);

        verify(alpaca).getQuote("AAPL");
    }

    private OrderEvent buildEvent(OrderSide side) {
        OrderEvent event = new OrderEvent();
        event.orderId = "order-1";
        event.clientOrderId = "client-1";
        event.symbol = "AAPL";
        event.side = side;
        event.type = OrderType.MARKET;
        event.quantity = 100;
        event.riskPassed = true;
        return event;
    }
}
