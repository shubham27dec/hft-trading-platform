package com.hft.notification.dto;

import lombok.Data;

@Data
public class TradeNotification {

    public enum Type { FILL, REJECTION }

    private Type type;
    private String accountId;
    private String symbol;
    private String message;
    private long timestamp;
}
