package com.hft.execution.venue;

import com.hft.execution.event.OrderEvent;

public interface ExecutionVenue {
    String name();
    VenueQuote getQuote(String symbol);
    ExecutionResult execute(OrderEvent event);
}
