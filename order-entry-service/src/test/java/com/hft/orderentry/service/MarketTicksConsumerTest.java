package com.hft.orderentry.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketTicksConsumerTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MarketTicksConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new MarketTicksConsumer(redisTemplate, objectMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void onTick_validMessage_writesQuoteToRedis() {
        String msg = "{\"symbol\":\"AAPL\",\"bid\":149.0,\"ask\":150.0,\"last\":149.5,\"volume\":1000,\"ts\":123}";
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        consumer.onTick(msg);

        verify(valueOps).set(eq("quote:AAPL"), anyString(), any());
    }

    @Test
    void onTick_nullSymbol_noRedisWrite() {
        consumer.onTick("{\"bid\":149.0}");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void onTick_invalidJson_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onTick("not-json"));
    }
}
