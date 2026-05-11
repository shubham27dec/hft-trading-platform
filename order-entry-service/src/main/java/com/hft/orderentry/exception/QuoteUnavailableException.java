package com.hft.orderentry.exception;

public class QuoteUnavailableException extends RuntimeException {

    public QuoteUnavailableException(String symbol) {
        super("Market data unavailable for " + symbol + ". Please try again shortly.");
    }
}
