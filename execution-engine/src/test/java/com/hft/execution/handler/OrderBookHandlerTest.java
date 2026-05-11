package com.hft.execution.handler;

import com.hft.execution.event.TickEvent;
import com.hft.execution.feed.PriceCache;
import com.hft.execution.venue.VenueQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookHandlerTest {

    private PriceCache priceCache;
    private OrderBookHandler handler;

    @BeforeEach
    void setUp() {
        priceCache = new PriceCache();
        handler = new OrderBookHandler(priceCache);
    }

    @Test
    void tick_validQuote_updatesPriceCache() {
        handler.onEvent(buildTick("AAPL", 150.5, 150.3), 0, false);

        VenueQuote q = priceCache.get("AAPL");
        assertNotNull(q);
        assertEquals(150.5, q.ask(), 0.001);
        assertEquals(150.3, q.bid(), 0.001);
    }

    @Test
    void tick_nullSymbol_ignored() {
        handler.onEvent(buildTick(null, 150.0, 149.0), 0, false);
        assertEquals(0, priceCache.size());
    }

    @Test
    void tick_zeroAskAndBid_ignored() {
        handler.onEvent(buildTick("TSLA", 0, 0), 0, false);
        assertNull(priceCache.get("TSLA"));
    }

    @Test
    void tick_onlyAskNonZero_updates() {
        handler.onEvent(buildTick("NVDA", 500.0, 0), 0, false);
        assertNotNull(priceCache.get("NVDA"));
    }

    @Test
    void tick_multipleTicks_lastOneWins() {
        handler.onEvent(buildTick("AAPL", 150.0, 149.0), 0, false);
        handler.onEvent(buildTick("AAPL", 151.0, 150.5), 1, false);

        assertEquals(151.0, priceCache.get("AAPL").ask(), 0.001);
    }

    private TickEvent buildTick(String symbol, double ask, double bid) {
        TickEvent e = new TickEvent();
        e.symbol = symbol;
        e.ask = ask;
        e.bid = bid;
        return e;
    }
}
