package com.hft.execution.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.execution.event.TickEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class TickKafkaPublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TickKafkaPublisher.class);
    private static final String TOPIC = "market.ticks";
    private static final long THROTTLE_MS = 500;

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Long> lastPublished = new ConcurrentHashMap<>();

    public TickKafkaPublisher(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "50");
        this.producer = new KafkaProducer<>(props);
    }

    public void publish(TickEvent event) {
        if (event.symbol == null) return;
        long now = System.currentTimeMillis();
        Long last = lastPublished.get(event.symbol);
        if (last != null && (now - last) < THROTTLE_MS) return;
        lastPublished.put(event.symbol, now);
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "symbol", event.symbol,
                    "bid", event.bid,
                    "ask", event.ask,
                    "last", event.last,
                    "volume", event.volume,
                    "ts", now
            ));
            producer.send(new ProducerRecord<>(TOPIC, event.symbol, json));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize tick for {}: {}", event.symbol, e.getMessage());
        }
    }

    @Override
    public void close() {
        producer.flush();
        producer.close();
    }
}
