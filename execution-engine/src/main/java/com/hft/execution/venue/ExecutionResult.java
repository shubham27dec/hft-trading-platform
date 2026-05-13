package com.hft.execution.venue;

public record ExecutionResult(String fillId, double fillPrice, long filledQty) {}
