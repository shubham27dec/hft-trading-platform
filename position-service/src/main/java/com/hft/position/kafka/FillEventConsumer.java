package com.hft.position.kafka;

import com.hft.core.event.OrderFilledEvent;
import com.hft.position.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillEventConsumer {

    private final PositionService positionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.filled", groupId = "position-service")
    public void consume(String message) {
        try {
            OrderFilledEvent event = objectMapper.readValue(message, OrderFilledEvent.class);
            positionService.applyFill(event);
        } catch (Exception e) {
            log.error("Failed to process fill event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
