package com.hft.execution.handler;

import com.hft.execution.event.TickEvent;
import com.hft.execution.feed.PriceCache;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderBookHandler implements EventHandler<TickEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderBookHandler.class);

    private final PriceCache priceCache;

    public OrderBookHandler(PriceCache priceCache) {
        this.priceCache = priceCache;
    }

    @Override
    public void onEvent(TickEvent event, long sequence, boolean endOfBatch) {
        if (event.symbol == null) return;
        if (event.ask <= 0 && event.bid <= 0) return;

        priceCache.update(event.symbol, event.ask, event.bid);
        log.trace("OrderBook updated {} ask={} bid={}", event.symbol, event.ask, event.bid);
    }
}
