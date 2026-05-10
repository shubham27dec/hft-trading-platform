package com.hft.orderentry.service;

import tools.jackson.databind.ObjectMapper;
import com.hft.core.enums.OrderStatus;
import com.hft.core.event.OrderSubmittedEvent;
import com.hft.core.model.Tick;
import com.hft.orderentry.dto.OrderRequest;
import com.hft.orderentry.dto.OrderResponse;
import com.hft.orderentry.kafka.OrderKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String QUOTE_CACHE_PREFIX = "quote:";

    private final OrderKafkaProducer kafkaProducer;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OrderResponse submitOrder(OrderRequest request) {
        String accountId = SecurityContextHolder.getContext().getAuthentication().getName();
        String orderId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis() * 1_000; // epoch micros

        checkQuoteFreshness(request.getSymbol());

        OrderSubmittedEvent event = new OrderSubmittedEvent();
        event.setOrderId(orderId);
        event.setClientOrderId(request.getClientOrderId());
        event.setSymbol(request.getSymbol());
        event.setSide(request.getSide());
        event.setType(request.getType());
        event.setQuantity(request.getQuantity());
        event.setLimitPrice(request.getLimitPrice());
        event.setAccountId(accountId);
        event.setSubmittedAt(now);

        kafkaProducer.publish(event);

        return OrderResponse.builder()
                .orderId(orderId)
                .clientOrderId(request.getClientOrderId())
                .symbol(request.getSymbol())
                .status(OrderStatus.SUBMITTED)
                .submittedAt(now)
                .build();
    }

    private void checkQuoteFreshness(String symbol) {
        String cached = redisTemplate.opsForValue().get(QUOTE_CACHE_PREFIX + symbol);
        if (cached == null) {
            log.warn("No fresh quote found for symbol {} — proceeding without quote validation", symbol);
            return;
        }
        try {
            Tick tick = objectMapper.readValue(cached, Tick.class);
            log.debug("Quote for {} — bid={} ask={}", symbol, tick.getBidPrice(), tick.getAskPrice());
        } catch (Exception e) {
            log.warn("Could not parse cached quote for {}: {}", symbol, e.getMessage());
        }
    }
}
