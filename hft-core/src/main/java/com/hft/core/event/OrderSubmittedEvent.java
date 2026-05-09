package com.hft.core.event;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import lombok.Data;

@Data
public class OrderSubmittedEvent {
    private String orderId;
    private String clientOrderId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private long quantity;
    private double limitPrice;
    private String accountId;
    private long submittedAt;
}
