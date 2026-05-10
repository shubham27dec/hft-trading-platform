package com.hft.riskdashboard.integration;

import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.riskdashboard.service.RiskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class FillConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @MockitoBean private RiskService riskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fillEvent_publishedToKafka_triggersRiskUpdate() throws Exception {
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

        verify(riskService, timeout(5000).times(1)).processFill(any(OrderFilledEvent.class));
    }
}
