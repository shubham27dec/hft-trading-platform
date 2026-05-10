package com.hft.notification.kafka;

import com.hft.core.event.OrderRejectedEvent;
import com.hft.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RejectNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.rejected", groupId = "notification-service")
    public void consume(String message) {
        try {
            OrderRejectedEvent event = objectMapper.readValue(message, OrderRejectedEvent.class);
            notificationService.notifyRejection(event);
        } catch (Exception e) {
            log.error("Failed to process rejection notification: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
