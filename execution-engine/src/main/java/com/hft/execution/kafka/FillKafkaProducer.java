package com.hft.execution.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.event.OrderRejectedEvent;
import com.hft.execution.event.OrderEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class FillKafkaProducer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FillKafkaProducer.class);
    private static final String FILLED_TOPIC = "orders.filled";
    private static final String REJECTED_TOPIC = "orders.rejected";

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper mapper;

    public FillKafkaProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.producer = new KafkaProducer<>(props);
        this.mapper = new ObjectMapper();
    }

    public void publishFilled(OrderEvent event) {
        try {
            OrderFilledEvent filled = new OrderFilledEvent();
            filled.setOrderId(event.orderId);
            filled.setSymbol(event.symbol);
            filled.setSide(event.side);
            filled.setFillId(event.fillId);
            filled.setFilledQty(event.filledQty);
            filled.setFillPrice(event.fillPrice);
            filled.setAccountId(event.accountId);
            filled.setFilledAt(event.filledAt);
            send(FILLED_TOPIC, event.orderId, filled);
        } catch (Exception e) {
            log.error("Failed to publish fill for order {}: {}", event.orderId, e.getMessage());
        }
    }

    public void publishRejected(OrderEvent event) {
        try {
            OrderRejectedEvent rejected = new OrderRejectedEvent();
            rejected.setOrderId(event.orderId);
            rejected.setSymbol(event.symbol);
            rejected.setAccountId(event.accountId);
            rejected.setReason(event.rejectionReason);
            rejected.setRejectedAt(System.currentTimeMillis() * 1_000);
            send(REJECTED_TOPIC, event.orderId, rejected);
        } catch (Exception e) {
            log.error("Failed to publish rejection for order {}: {}", event.orderId, e.getMessage());
        }
    }

    private void send(String topic, String key, Object value) throws Exception {
        String json = mapper.writeValueAsString(value);
        producer.send(new ProducerRecord<>(topic, key, json));
    }

    @Override
    public void close() {
        producer.flush();
        producer.close();
    }
}
