package com.hft.core.event;

import com.hft.core.enums.OrderSide;
import lombok.Data;

@Data
public class OrderFilledEvent {
    private String fillId;
    private String orderId;
    private String symbol;
    private OrderSide side;
    private long filledQty;
    private double fillPrice;
    private String accountId;
    private long filledAt;
}
