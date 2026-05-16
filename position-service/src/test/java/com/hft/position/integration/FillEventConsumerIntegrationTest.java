package com.hft.position.integration;

import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.position.service.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class FillEventConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @MockitoBean private PositionService positionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fillEvent_publishedToKafka_triggersPositionUpdate() throws Exception {
        OrderFilledEvent event = new OrderFilledEvent();
        event.setFillId("fill-1");
        event.setOrderId("order-1");
        event.setSymbol("AAPL");
        event.setSide(OrderSide.BUY);
        event.setFilledQty(100);
        event.setFillPrice(150.0);
        event.setAccountId("acc-001");
        event.setFilledAt(System.currentTimeMillis() * 1000);

        kafkaTemplate.send("orders.filled", objectMapper.writeValueAsString(event));

        // Verify PositionService.applyFill() was called within 5 seconds
        verify(positionService, timeout(10000).times(1)).applyFill(any(OrderFilledEvent.class));
    }
}
