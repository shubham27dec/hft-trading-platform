package com.hft.execution.venue;

public record VenueQuote(double ask, double bid) {
    public double mid() {
        return (ask + bid) / 2.0;
    }
}
