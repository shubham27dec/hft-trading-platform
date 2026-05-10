package com.hft.orderentry.dto;

import com.hft.core.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String orderId;
    private String clientOrderId;
    private String symbol;
    private OrderStatus status;
    private long submittedAt;
}
