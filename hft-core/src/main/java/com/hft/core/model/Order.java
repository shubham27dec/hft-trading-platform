package com.hft.core.model;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderStatus;
import com.hft.core.enums.OrderType;
import lombok.Data;

@Data
public class Order {
    private String orderId;
    private String clientOrderId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private long quantity;
    private double limitPrice;
    private OrderStatus status;
    private String accountId;
    private long submittedAt;
    private long filledAt;
}
