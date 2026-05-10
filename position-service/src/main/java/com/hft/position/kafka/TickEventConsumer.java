package com.hft.position.kafka;

import com.hft.core.model.Tick;
import com.hft.position.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TickEventConsumer {

    private final PositionService positionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "market.ticks", groupId = "position-service-ticks")
    public void consume(String message) {
        try {
            Tick tick = objectMapper.readValue(message, Tick.class);
            positionService.applyTick(tick);
        } catch (Exception e) {
            log.warn("Failed to process tick: {}", e.getMessage());
        }
    }
}
