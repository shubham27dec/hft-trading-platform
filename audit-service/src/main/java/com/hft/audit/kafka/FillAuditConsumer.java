package com.hft.audit.kafka;

import com.hft.core.event.OrderFilledEvent;
import com.hft.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillAuditConsumer {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.filled", groupId = "audit-service")
    public void consume(String message) {
        try {
            OrderFilledEvent event = objectMapper.readValue(message, OrderFilledEvent.class);
            auditService.recordFill(event);
        } catch (Exception e) {
            log.error("Failed to audit fill event: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
