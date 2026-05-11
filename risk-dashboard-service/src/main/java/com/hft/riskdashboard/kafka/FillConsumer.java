package com.hft.riskdashboard.kafka;

import com.hft.core.event.OrderFilledEvent;
import com.hft.riskdashboard.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillConsumer {

    private final RiskService riskService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.filled", groupId = "risk-dashboard-service")
    public void consume(String message) {
        try {
            OrderFilledEvent event = objectMapper.readValue(message, OrderFilledEvent.class);
            riskService.processFill(event);
        } catch (Exception e) {
            log.error("Failed to process fill event: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
