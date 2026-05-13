package com.hft.execution.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.execution.event.TickEvent;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedHandler {

    private static final Logger log = LoggerFactory.getLogger(FeedHandler.class);

    private final RingBuffer<TickEvent> tickBuffer;
    private final ObjectMapper mapper;

    public FeedHandler(RingBuffer<TickEvent> tickBuffer) {
        this(tickBuffer, new ObjectMapper());
    }

    FeedHandler(RingBuffer<TickEvent> tickBuffer, ObjectMapper mapper) {
        this.tickBuffer = tickBuffer;
        this.mapper = mapper;
    }

    void handleMessage(String text) {
        try {
            JsonNode root = mapper.readTree(text);
            if (!root.isArray()) return;
            for (JsonNode node : root) {
                String type = node.path("T").asText();
                if ("q".equals(type)) {
                    publishTick(node);
                }
            }
        } catch (Exception e) {
            log.warn("FeedHandler message parse error: {}", e.getMessage());
        }
    }

    private void publishTick(JsonNode node) {
        double ask = node.path("ap").asDouble();
        double bid = node.path("bp").asDouble();
        if (ask <= 0 && bid <= 0) return;

        String symbol = node.path("S").asText();
        long seq = tickBuffer.next();
        try {
            TickEvent event = tickBuffer.get(seq);
            event.symbol = symbol;
            event.ask = ask;
            event.bid = bid;
            event.last = node.path("lp").asDouble();
            event.volume = node.path("ls").asLong();
            event.timestamp = System.nanoTime();
        } finally {
            tickBuffer.publish(seq);
        }
    }
}
