package com.hft.execution.handler;

import com.hft.core.enums.OrderSide;
import com.hft.execution.event.OrderEvent;
import com.hft.execution.feed.PriceCache;
import com.hft.execution.venue.ExecutionVenue;
import com.hft.execution.venue.VenueQuote;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingHandler implements EventHandler<OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(RoutingHandler.class);

    private static final double SIMULATED_SPREAD = 0.0001;

    private final ExecutionVenue alpaca;
    private final ExecutionVenue simulated;
    private final PriceCache priceCache;

    public RoutingHandler(ExecutionVenue alpaca, ExecutionVenue simulated, PriceCache priceCache) {
        this.alpaca = alpaca;
        this.simulated = simulated;
        this.priceCache = priceCache;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (!event.riskPassed) return;

        VenueQuote alpacaQuote = fromCache(event.symbol);
        VenueQuote simulatedQuote = syntheticFromAlpaca(alpacaQuote);

        if (event.side == OrderSide.BUY) {
            if (alpacaQuote.ask() > 0 && alpacaQuote.ask() <= simulatedQuote.ask()) {
                route(event, alpaca.name(), alpacaQuote);
            } else {
                route(event, simulated.name(), simulatedQuote);
            }
        } else {
            if (alpacaQuote.bid() > 0 && alpacaQuote.bid() >= simulatedQuote.bid()) {
                route(event, alpaca.name(), alpacaQuote);
            } else {
                route(event, simulated.name(), simulatedQuote);
            }
        }
    }

    private VenueQuote fromCache(String symbol) {
        VenueQuote cached = priceCache.get(symbol);
        if (cached != null) {
            return cached;
        }
        // Cold-start fallback — REST call only if WebSocket hasn't provided a price yet
        log.warn("PriceCache miss for {} — falling back to REST quote", symbol);
        return alpaca.getQuote(symbol);
    }

    private VenueQuote syntheticFromAlpaca(VenueQuote alpacaQuote) {
        double mid = (alpacaQuote.ask() + alpacaQuote.bid()) / 2.0;
        return new VenueQuote(mid * (1 + SIMULATED_SPREAD), mid * (1 - SIMULATED_SPREAD));
    }

    private void route(OrderEvent event, String venue, VenueQuote quote) {
        event.venue = venue;
        event.routedAsk = quote.ask();
        event.routedBid = quote.bid();
        log.debug("Routed order {} to {} ask={} bid={}", event.orderId, venue, quote.ask(), quote.bid());
    }
}
