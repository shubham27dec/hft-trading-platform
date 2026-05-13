package com.hft.orderentry.service;

import tools.jackson.databind.ObjectMapper;
import com.hft.core.enums.OrderStatus;
import com.hft.core.event.OrderSubmittedEvent;
import com.hft.core.model.Tick;
import com.hft.orderentry.client.AlpacaQuoteClient;
import com.hft.orderentry.client.AlpacaSnapshotEntry;
import com.hft.orderentry.dto.OrderRequest;
import com.hft.orderentry.dto.OrderResponse;
import com.hft.orderentry.kafka.OrderKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    static final String QUOTE_CACHE_PREFIX = "quote:";
    private static final Duration QUOTE_TTL = Duration.ofMillis(500);

    private final OrderKafkaProducer kafkaProducer;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AlpacaQuoteClient alpacaQuoteClient;

    public OrderResponse submitOrder(OrderRequest request) {
        String accountId = SecurityContextHolder.getContext().getAuthentication().getName();
        String orderId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis() * 1_000;

        double quotePrice = resolveQuote(request.getSymbol());

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
                .quotePrice(quotePrice)
                .build();
    }

    private double resolveQuote(String symbol) {
        String cached = redisTemplate.opsForValue().get(QUOTE_CACHE_PREFIX + symbol);
        if (cached != null) {
            try {
                Tick tick = objectMapper.readValue(cached, Tick.class);
                double mid = (tick.getBidPrice() + tick.getAskPrice()) / 2.0;
                log.debug("Quote for {} from cache — mid={}", symbol, mid);
                return mid;
            } catch (Exception e) {
                log.warn("Could not parse cached quote for {}: {}", symbol, e.getMessage());
            }
        }

        log.warn("No fresh quote in cache for {} — falling back to Alpaca snapshot", symbol);
        return fetchSnapshotsAndWarmCache(symbol);
    }

    private double fetchSnapshotsAndWarmCache(String symbol) {
        Set<String> cachedKeys = redisTemplate.keys(QUOTE_CACHE_PREFIX + "*");
        Set<String> symbols = cachedKeys == null ? Set.of(symbol) :
                cachedKeys.stream()
                        .map(k -> k.replace(QUOTE_CACHE_PREFIX, ""))
                        .collect(Collectors.toSet());
        symbols = new java.util.HashSet<>(symbols);
        symbols.add(symbol);

        Map<String, AlpacaSnapshotEntry> snapshots = alpacaQuoteClient.getSnapshots(symbols);

        snapshots.forEach((sym, entry) -> {
            if (entry.getLatestQuote() == null) return;
            try {
                Tick tick = new Tick();
                tick.setSymbol(sym);
                tick.setBidPrice(entry.getLatestQuote().getBp());
                tick.setAskPrice(entry.getLatestQuote().getAp());
                tick.setBidSize(entry.getLatestQuote().getBs());
                tick.setAskSize(entry.getLatestQuote().getAs());
                redisTemplate.opsForValue().set(QUOTE_CACHE_PREFIX + sym,
                        objectMapper.writeValueAsString(tick), QUOTE_TTL);
            } catch (Exception e) {
                log.warn("Failed to warm cache for {}: {}", sym, e.getMessage());
            }
        });

        AlpacaSnapshotEntry entry = snapshots.get(symbol);
        if (entry == null || entry.getLatestQuote() == null) {
            throw new com.hft.orderentry.exception.QuoteUnavailableException(symbol);
        }
        return (entry.getLatestQuote().getAp() + entry.getLatestQuote().getBp()) / 2.0;
    }

    record QuoteResult(double price) {}
}
