package com.hft.execution.event;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;

public class OrderEvent {

    // Populated by Kafka consumer from OrderSubmittedEvent
    public String orderId;
    public String clientOrderId;
    public String symbol;
    public OrderSide side;
    public OrderType type;
    public long quantity;
    public double limitPrice;
    public String accountId;
    public long submittedAt;

    // Set by RiskCheckHandler
    public boolean riskPassed;
    public String rejectionReason;

    // Set by RoutingHandler
    public String venue; // "ALPACA" or "SIMULATED"
    public double routedAsk;
    public double routedBid;

    // Set by ExecutionHandler
    public String fillId;
    public double fillPrice;
    public long filledQty;
    public long filledAt;
    public boolean filled;

    public void copyFrom(OrderEvent src) {
        this.orderId = src.orderId;
        this.clientOrderId = src.clientOrderId;
        this.symbol = src.symbol;
        this.side = src.side;
        this.type = src.type;
        this.quantity = src.quantity;
        this.limitPrice = src.limitPrice;
        this.accountId = src.accountId;
        this.submittedAt = src.submittedAt;
        this.riskPassed = src.riskPassed;
        this.rejectionReason = src.rejectionReason;
        this.venue = src.venue;
        this.routedAsk = src.routedAsk;
        this.routedBid = src.routedBid;
        this.fillId = src.fillId;
        this.fillPrice = src.fillPrice;
        this.filledQty = src.filledQty;
        this.filledAt = src.filledAt;
        this.filled = src.filled;
    }

    public void reset() {
        orderId = null;
        clientOrderId = null;
        symbol = null;
        side = null;
        type = null;
        quantity = 0;
        limitPrice = 0;
        accountId = null;
        submittedAt = 0;
        riskPassed = false;
        rejectionReason = null;
        venue = null;
        routedAsk = 0;
        routedBid = 0;
        fillId = null;
        fillPrice = 0;
        filledQty = 0;
        filledAt = 0;
        filled = false;
    }
}
