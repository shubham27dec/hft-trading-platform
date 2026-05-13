package com.hft.orderentry.controller;

import com.hft.core.model.Tick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @GetMapping("/quotes")
    public List<Tick> quotes() {
        Set<String> keys = redis.keys("quote:*");
        if (keys == null || keys.isEmpty()) return List.of();
        return keys.stream()
                .map(k -> redis.opsForValue().get(k))
                .filter(Objects::nonNull)
                .map(this::parse)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Tick::getSymbol))
                .toList();
    }

    private Tick parse(String json) {
        try {
            return objectMapper.readValue(json, Tick.class);
        } catch (Exception e) {
            log.warn("Failed to parse cached tick: {}", e.getMessage());
            return null;
        }
    }
}
