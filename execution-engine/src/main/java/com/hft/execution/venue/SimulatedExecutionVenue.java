package com.hft.execution.venue;

import com.hft.execution.event.OrderEvent;

import java.util.UUID;

public class SimulatedExecutionVenue implements ExecutionVenue {

    private static final double SPREAD_PCT = 0.0001; // 0.01% spread

    private final ExecutionVenue quoteSource;

    public SimulatedExecutionVenue(ExecutionVenue quoteSource) {
        this.quoteSource = quoteSource;
    }

    @Override
    public String name() {
        return "SIMULATED";
    }

    @Override
    public VenueQuote getQuote(String symbol) {
        VenueQuote real = quoteSource.getQuote(symbol);
        // Widen the spread slightly — simulated venue is always slightly worse
        double ask = real.ask() * (1 + SPREAD_PCT);
        double bid = real.bid() * (1 - SPREAD_PCT);
        return new VenueQuote(ask, bid);
    }

    @Override
    public ExecutionResult execute(OrderEvent event) {
        double fillPrice = event.side.name().equals("BUY") ? event.routedAsk : event.routedBid;
        return new ExecutionResult(UUID.randomUUID().toString(), fillPrice, event.quantity);
    }
}
