package com.hft.audit.integration;

import com.hft.audit.repository.AuditEventRepository;
import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.event.OrderRejectedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class AuditConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private AuditEventRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearDb() {
        repository.deleteAll();
    }

    @Test
    void fillEvent_publishedToKafka_persistedToDatabase() throws Exception {
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

        await().atMost(5, SECONDS).until(() ->
                !repository.findByOrderIdOrderByEventTimestampDesc("order-1").isEmpty());

        var records = repository.findByOrderIdOrderByEventTimestampDesc("order-1");
        assertEquals(1, records.size());
        assertEquals("FILL", records.get(0).getEventType());
        assertEquals("acc-001", records.get(0).getAccountId());
        assertEquals("AAPL", records.get(0).getSymbol());
    }

    @Test
    void rejectEvent_publishedToKafka_persistedToDatabase() throws Exception {
        OrderRejectedEvent event = new OrderRejectedEvent();
        event.setOrderId("order-2");
        event.setSymbol("TSLA");
        event.setAccountId("acc-001");
        event.setReason("INSUFFICIENT_FUNDS");
        event.setRejectedAt(System.currentTimeMillis() * 1000);

        kafkaTemplate.send("orders.rejected", objectMapper.writeValueAsString(event));

        await().atMost(10, SECONDS).until(() ->
                !repository.findByOrderIdOrderByEventTimestampDesc("order-2").isEmpty());

        var records = repository.findByOrderIdOrderByEventTimestampDesc("order-2");
        assertEquals(1, records.size());
        assertEquals("REJECTION", records.get(0).getEventType());
        assertTrue(records.get(0).getDetails().contains("INSUFFICIENT_FUNDS"));
    }
}
