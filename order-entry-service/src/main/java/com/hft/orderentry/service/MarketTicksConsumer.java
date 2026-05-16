package com.hft.orderentry.service;

import tools.jackson.databind.ObjectMapper;
import com.hft.core.model.Tick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketTicksConsumer {

    private static final Duration LIVE_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "market.ticks", groupId = "market-data-writer")
    public void onTick(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.readValue(message, Map.class);
            String symbol = (String) raw.get("symbol");
            if (symbol == null) return;

            Tick tick = new Tick();
            tick.setSymbol(symbol);
            tick.setBidPrice(toDouble(raw.get("bid")));
            tick.setAskPrice(toDouble(raw.get("ask")));
            tick.setLastPrice(toDouble(raw.get("last")));
            tick.setVolume(toLong(raw.get("volume")));
            tick.setTimestamp(toLong(raw.get("ts")));

            redisTemplate.opsForValue().set(
                    OrderService.QUOTE_CACHE_PREFIX + symbol,
                    objectMapper.writeValueAsString(tick),
                    LIVE_TTL);
        } catch (Exception e) {
            log.warn("Failed to process market tick: {}", e.getMessage());
        }
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return 0;
    }

    private long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        return 0;
    }
}
