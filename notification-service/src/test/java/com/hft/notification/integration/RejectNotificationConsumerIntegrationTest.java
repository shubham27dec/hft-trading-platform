package com.hft.notification.integration;

import com.hft.core.event.OrderRejectedEvent;
import com.hft.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class RejectNotificationConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @MockitoBean private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectEvent_publishedToKafka_triggersNotification() throws Exception {
        OrderRejectedEvent event = new OrderRejectedEvent();
        event.setOrderId("order-1");
        event.setSymbol("AAPL");
        event.setAccountId("acc-001");
        event.setReason("INSUFFICIENT_FUNDS");
        event.setRejectedAt(System.currentTimeMillis() * 1000);

        kafkaTemplate.send("orders.rejected", objectMapper.writeValueAsString(event));

        verify(notificationService, timeout(10000).times(1)).notifyRejection(any(OrderRejectedEvent.class));
    }
}
