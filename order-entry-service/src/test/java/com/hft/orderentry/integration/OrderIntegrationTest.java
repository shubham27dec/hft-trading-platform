package com.hft.orderentry.integration;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import com.hft.orderentry.dto.OrderRequest;
import com.hft.orderentry.dto.OrderResponse;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort private int port;
    @Autowired private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void submitOrder_fullStack_publishesEventToKafka() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .clientOrderId("integration-test-1")
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(100)
                .limitPrice(0)
                .build();

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        ResponseEntity<OrderResponse> response = restClient.post()
                .uri("/api/orders")
                .header("X-API-Key", "test-api-key-001")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(OrderResponse.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AAPL", response.getBody().getSymbol());
        assertNotNull(response.getBody().getOrderId());

        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "orders.submitted");

        ConsumerRecord<String, String> consumed = KafkaTestUtils.getSingleRecord(
                consumer, "orders.submitted");

        assertNotNull(consumed.value());
        assertTrue(consumed.value().contains("AAPL"));
        assertTrue(consumed.value().contains("integration-test-1"));
        consumer.close();
    }
}
