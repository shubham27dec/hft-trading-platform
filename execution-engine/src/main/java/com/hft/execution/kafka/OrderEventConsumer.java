package com.hft.execution.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.core.event.OrderSubmittedEvent;
import com.hft.execution.event.OrderEvent;
import com.lmax.disruptor.RingBuffer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class OrderEventConsumer implements Runnable, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final String TOPIC = "orders.submitted";

    private final KafkaConsumer<String, String> consumer;
    private final RingBuffer<OrderEvent> ringBuffer;
    private final ObjectMapper mapper;
    private volatile boolean running = true;

    public OrderEventConsumer(String bootstrapServers, RingBuffer<OrderEvent> ringBuffer) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "execution-engine");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        this.consumer = new KafkaConsumer<>(props);
        this.ringBuffer = ringBuffer;
        this.mapper = new ObjectMapper();
        consumer.subscribe(List.of(TOPIC));
    }

    @Override
    public void run() {
        log.info("OrderEventConsumer started, polling {}", TOPIC);
        while (running) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10));
            for (ConsumerRecord<String, String> record : records) {
                try {
                    OrderSubmittedEvent submitted = mapper.readValue(record.value(), OrderSubmittedEvent.class);
                    long sequence = ringBuffer.next();
                    try {
                        OrderEvent event = ringBuffer.get(sequence);
                        event.reset();
                        event.orderId = submitted.getOrderId();
                        event.clientOrderId = submitted.getClientOrderId();
                        event.symbol = submitted.getSymbol();
                        event.side = submitted.getSide();
                        event.type = submitted.getType();
                        event.quantity = submitted.getQuantity();
                        event.limitPrice = submitted.getLimitPrice();
                        event.accountId = submitted.getAccountId();
                        event.submittedAt = submitted.getSubmittedAt();
                    } finally {
                        ringBuffer.publish(sequence);
                    }
                    consumer.commitSync(Map.of(
                            new org.apache.kafka.common.TopicPartition(record.topic(), record.partition()),
                            new org.apache.kafka.clients.consumer.OffsetAndMetadata(record.offset() + 1)));
                } catch (Exception e) {
                    log.error("Failed to process Kafka record: {}", e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void close() {
        stop();
        consumer.close();
    }
}
