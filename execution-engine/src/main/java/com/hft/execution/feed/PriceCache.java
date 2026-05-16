package com.hft.execution.feed;

import com.hft.execution.venue.VenueQuote;
import net.openhft.chronicle.map.ChronicleMap;

public class PriceCache implements AutoCloseable {

    // Off-heap Chronicle Map — data lives outside JVM heap, GC never scans it.
    // getUsing() reads into a pre-allocated ThreadLocal entry — zero allocation on hot path.
    private final ChronicleMap<String, PriceEntry> map;
    private final ThreadLocal<PriceEntry> tlEntry = ThreadLocal.withInitial(PriceEntry::new);

    public PriceCache() {
        try {
            this.map = ChronicleMap
                    .of(String.class, PriceEntry.class)
                    .name("price-cache")
                    .entries(500)
                    .averageKey("AAPL")
                    .averageValueSize(24) // 3 doubles × 8 bytes each
                    .create();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise PriceCache", e);
        }
    }

    public void update(String symbol, double ask, double bid) {
        PriceEntry entry = tlEntry.get();
        entry.ask = ask;
        entry.bid = bid;
        map.put(symbol, entry);
    }

    public void update(String symbol, double ask, double bid, double last) {
        PriceEntry entry = tlEntry.get();
        entry.ask  = ask;
        entry.bid  = bid;
        if (last > 0) entry.last = last;
        map.put(symbol, entry);
    }

    public VenueQuote get(String symbol) {
        PriceEntry result = map.getUsing(symbol, tlEntry.get());
        if (result == null) return null;
        return new VenueQuote(result.ask, result.bid);
    }

    public boolean contains(String symbol) {
        return map.containsKey(symbol);
    }

    public int size() {
        return map.size();
    }

    @Override
    public void close() {
        map.close();
    }
}
