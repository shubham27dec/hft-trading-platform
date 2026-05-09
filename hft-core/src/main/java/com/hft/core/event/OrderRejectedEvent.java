package com.hft.core.event;

import lombok.Data;

@Data
public class OrderRejectedEvent {
    private String orderId;
    private String symbol;
    private String accountId;
    private String reason;
    private long rejectedAt;
}
