package com.hft.audit.kafka;

import com.hft.core.event.OrderRejectedEvent;
import com.hft.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RejectAuditConsumer {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.rejected", groupId = "audit-service")
    public void consume(String message) {
        try {
            OrderRejectedEvent event = objectMapper.readValue(message, OrderRejectedEvent.class);
            auditService.recordRejection(event);
        } catch (Exception e) {
            log.error("Failed to audit rejection event: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
