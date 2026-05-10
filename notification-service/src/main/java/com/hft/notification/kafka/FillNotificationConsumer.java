package com.hft.notification.kafka;

import com.hft.core.event.OrderFilledEvent;
import com.hft.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.filled", groupId = "notification-service")
    public void consume(String message) {
        try {
            OrderFilledEvent event = objectMapper.readValue(message, OrderFilledEvent.class);
            notificationService.notifyFill(event);
        } catch (Exception e) {
            log.error("Failed to process fill notification: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
