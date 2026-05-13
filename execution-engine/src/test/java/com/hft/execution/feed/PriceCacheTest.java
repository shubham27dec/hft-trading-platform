package com.hft.execution.feed;

import com.hft.execution.venue.VenueQuote;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceCacheTest {

    @Test
    void update_thenGet_returnsLatestQuote() {
        PriceCache cache = new PriceCache();
        cache.update("AAPL", 150.5, 150.3);

        VenueQuote q = cache.get("AAPL");
        assertNotNull(q);
        assertEquals(150.5, q.ask(), 0.001);
        assertEquals(150.3, q.bid(), 0.001);
    }

    @Test
    void update_overwritesPreviousEntry() {
        PriceCache cache = new PriceCache();
        cache.update("AAPL", 150.0, 149.0);
        cache.update("AAPL", 151.0, 150.5);

        VenueQuote q = cache.get("AAPL");
        assertEquals(151.0, q.ask(), 0.001);
    }

    @Test
    void get_missingSymbol_returnsNull() {
        PriceCache cache = new PriceCache();
        assertNull(cache.get("TSLA"));
    }

    @Test
    void contains_afterUpdate_returnsTrue() {
        PriceCache cache = new PriceCache();
        cache.update("NVDA", 500.0, 499.0);
        assertTrue(cache.contains("NVDA"));
        assertFalse(cache.contains("MSFT"));
    }

    @Test
    void size_reflectsDistinctSymbols() {
        PriceCache cache = new PriceCache();
        cache.update("AAPL", 150.0, 149.0);
        cache.update("TSLA", 200.0, 199.0);
        cache.update("AAPL", 151.0, 150.0); // overwrite, not new entry
        assertEquals(2, cache.size());
    }
}
