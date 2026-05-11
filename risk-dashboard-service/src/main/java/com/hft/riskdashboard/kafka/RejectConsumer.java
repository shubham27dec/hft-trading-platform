package com.hft.riskdashboard.kafka;

import com.hft.core.event.OrderRejectedEvent;
import com.hft.riskdashboard.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RejectConsumer {

    private final RiskService riskService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.rejected", groupId = "risk-dashboard-service")
    public void consume(String message) {
        try {
            OrderRejectedEvent event = objectMapper.readValue(message, OrderRejectedEvent.class);
            riskService.processRejection(event);
        } catch (Exception e) {
            log.error("Failed to process rejected event: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
