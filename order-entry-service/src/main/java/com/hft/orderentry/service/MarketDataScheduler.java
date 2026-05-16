package com.hft.orderentry.service;

import tools.jackson.databind.ObjectMapper;
import com.hft.core.model.Tick;
import com.hft.orderentry.client.AlpacaQuoteClient;
import com.hft.orderentry.client.AlpacaSnapshotEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MarketDataScheduler {

    private static final Duration DISPLAY_TTL = Duration.ofSeconds(5);
    private static final long LIVE_THRESHOLD_SECONDS = 2;

    // Grows over time: starts from config, gains symbols as Redis discovers them.
    private final Set<String> knownSymbols = ConcurrentHashMap.newKeySet();

    private final AlpacaQuoteClient alpacaQuoteClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MarketDataScheduler(
            AlpacaQuoteClient alpacaQuoteClient,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${watched.symbols:AAPL,TSLA,NVDA,MSFT,AMZN,GOOGL,META,AMD,NFLX,SPY}") String initialSymbols) {
        this.alpacaQuoteClient = alpacaQuoteClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        Arrays.stream(initialSymbols.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .forEach(knownSymbols::add);
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshQuotes() {
        // Discover any symbols that appeared in Redis but aren't tracked yet
        Set<String> redisKeys = redisTemplate.keys(OrderService.QUOTE_CACHE_PREFIX + "*");
        if (redisKeys != null) {
            redisKeys.stream()
                     .map(k -> k.replace(OrderService.QUOTE_CACHE_PREFIX, ""))
                     .forEach(knownSymbols::add);
        }

        if (knownSymbols.isEmpty()) return;

        // If live ticks are flowing from Kafka consumer, skip the REST call
        if (liveTicksActive(knownSymbols)) return;

        log.debug("No live ticks — falling back to Alpaca REST snapshots for {} symbols", knownSymbols.size());
        try {
            Map<String, AlpacaSnapshotEntry> snapshots = alpacaQuoteClient.getSnapshots(knownSymbols);
            snapshots.forEach((sym, entry) -> cacheEntry(sym, entry));
        } catch (Exception e) {
            log.warn("Market data REST refresh failed: {}", e.getMessage());
        }
    }

    void cacheEntry(String sym, AlpacaSnapshotEntry entry) {
        if (entry.getLatestQuote() == null) return;
        try {
            Tick tick = new Tick();
            tick.setSymbol(sym);
            tick.setBidPrice(entry.getLatestQuote().getBp());
            double ask = entry.getLatestQuote().getAp();
            if (ask <= 0 && entry.getLatestTrade() != null && entry.getLatestTrade().getP() > 0) {
                ask = entry.getLatestTrade().getP();
            }
            tick.setAskPrice(ask);
            tick.setBidSize(entry.getLatestQuote().getBs());
            tick.setAskSize(entry.getLatestQuote().getAs());
            if (entry.getLatestTrade() != null) {
                tick.setLastPrice(entry.getLatestTrade().getP());
                tick.setVolume(entry.getDailyBar() != null ? entry.getDailyBar().getV() : 0L);
            }
            redisTemplate.opsForValue().set(
                    OrderService.QUOTE_CACHE_PREFIX + sym,
                    objectMapper.writeValueAsString(tick),
                    DISPLAY_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache quote for {}: {}", sym, e.getMessage());
        }
    }

    private boolean liveTicksActive(Set<String> symbols) {
        for (String sym : symbols) {
            Long ttl = redisTemplate.getExpire(OrderService.QUOTE_CACHE_PREFIX + sym, TimeUnit.SECONDS);
            if (ttl != null && ttl > LIVE_THRESHOLD_SECONDS) return true;
        }
        return false;
    }
}
