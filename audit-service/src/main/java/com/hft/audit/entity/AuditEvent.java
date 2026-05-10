package com.hft.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_events", schema = "audit")
@Data
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;       // "FILL" or "REJECTION"

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "account_id")
    private String accountId;

    private String symbol;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "event_timestamp")
    private long eventTimestamp;

    @Column(name = "recorded_at")
    private long recordedAt;
}
