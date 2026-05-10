package com.hft.orderentry.kafka;

import com.hft.core.event.OrderSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private static final String TOPIC = "orders.submitted";

    private final KafkaTemplate<String, OrderSubmittedEvent> kafkaTemplate;

    public void publish(OrderSubmittedEvent event) {
        kafkaTemplate.send(TOPIC, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order {} to Kafka: {}", event.getOrderId(), ex.getMessage());
                    } else {
                        log.debug("Published order {} to partition {}", event.getOrderId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
