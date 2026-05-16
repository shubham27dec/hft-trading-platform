package com.hft.orderentry.service;

import tools.jackson.databind.ObjectMapper;
import com.hft.core.model.Tick;
import com.hft.orderentry.client.AlpacaQuoteClient;
import com.hft.orderentry.client.AlpacaSnapshotEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataSchedulerTest {

    @Mock private AlpacaQuoteClient alpacaQuoteClient;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private ValueOperations<String, String> valueOps;

    private MarketDataScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MarketDataScheduler(alpacaQuoteClient, redisTemplate, objectMapper, "AAPL,TSLA");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void refreshQuotes_noLiveTicks_callsAlpacaSnapshots() {
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(0L);
        when(alpacaQuoteClient.getSnapshots(anySet())).thenReturn(Map.of());

        scheduler.refreshQuotes();

        verify(alpacaQuoteClient).getSnapshots(argThat(s -> s.contains("AAPL") && s.contains("TSLA")));
    }

    @Test
    void refreshQuotes_liveTicksActive_skipsAlpacaCall() {
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(redisTemplate.getExpire(eq("quote:AAPL"), any())).thenReturn(4L);

        scheduler.refreshQuotes();

        verifyNoInteractions(alpacaQuoteClient);
    }

    @Test
    void refreshQuotes_discoversNewSymbolFromRedis() {
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("quote:NVDA"));
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(0L);
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.captor();
        when(alpacaQuoteClient.getSnapshots(captor.capture())).thenReturn(Map.of());

        scheduler.refreshQuotes();

        assertTrue(captor.getValue().contains("NVDA"));
    }

    @Test
    void refreshQuotes_alpacaThrows_doesNotPropagate() {
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(0L);
        when(alpacaQuoteClient.getSnapshots(anySet())).thenThrow(new RuntimeException("timeout"));

        assertDoesNotThrow(() -> scheduler.refreshQuotes());
    }

    @Test
    void cacheEntry_validEntry_writesToRedis() throws Exception {
        when(objectMapper.writeValueAsString(any(Tick.class))).thenReturn("{\"symbol\":\"AAPL\"}");

        scheduler.cacheEntry("AAPL", buildEntry(150.5, 149.5));

        verify(valueOps).set(eq("quote:AAPL"), anyString(), any());
    }

    @Test
    void cacheEntry_nullLatestQuote_skipsWrite() {
        AlpacaSnapshotEntry entry = new AlpacaSnapshotEntry();
        entry.setLatestQuote(null);

        scheduler.cacheEntry("AAPL", entry);

        verifyNoInteractions(valueOps);
    }

    @Test
    void cacheEntry_zeroAsk_fallsBackToTradePrice() throws Exception {
        AlpacaSnapshotEntry entry = buildEntry(0, 149.5);
        AlpacaSnapshotEntry.Trade trade = new AlpacaSnapshotEntry.Trade();
        trade.setP(151.0);
        entry.setLatestTrade(trade);
        ArgumentCaptor<Tick> captor = ArgumentCaptor.forClass(Tick.class);
        when(objectMapper.writeValueAsString(captor.capture())).thenReturn("{}");

        scheduler.cacheEntry("AAPL", entry);

        assertEquals(151.0, captor.getValue().getAskPrice(), 0.001);
    }

    @Test
    void cacheEntry_objectMapperThrows_doesNotPropagate() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("serialize error"));

        assertDoesNotThrow(() -> scheduler.cacheEntry("AAPL", buildEntry(150.0, 149.0)));
    }

    private AlpacaSnapshotEntry buildEntry(double ask, double bid) {
        AlpacaSnapshotEntry entry = new AlpacaSnapshotEntry();
        AlpacaSnapshotEntry.Quote quote = new AlpacaSnapshotEntry.Quote();
        quote.setAp(ask);
        quote.setBp(bid);
        entry.setLatestQuote(quote);
        return entry;
    }
}
