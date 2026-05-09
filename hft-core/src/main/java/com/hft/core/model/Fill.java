package com.hft.core.model;

import com.hft.core.enums.OrderSide;
import lombok.Data;

@Data
public class Fill {
    private String fillId;
    private String orderId;
    private String symbol;
    private OrderSide side;
    private long filledQty;
    private double fillPrice;
    private long filledAt;
}
