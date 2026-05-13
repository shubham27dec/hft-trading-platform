package com.hft.orderentry.controller;

import com.hft.orderentry.client.AlpacaQuoteClient;
import com.hft.orderentry.repository.TraderAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"orders.submitted"})
@DirtiesContext
class MarketDataControllerTest {

    @Autowired private WebApplicationContext wac;
    private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;
    @MockitoBean private TraderAccountRepository traderAccountRepository;
    @MockitoBean private AlpacaQuoteClient alpacaQuoteClient;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    void quotes_nullKeys_returnsEmptyArray() throws Exception {
        when(stringRedisTemplate.keys("quote:*")).thenReturn(null);

        mockMvc.perform(get("/api/market/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void quotes_emptyKeys_returnsEmptyArray() throws Exception {
        when(stringRedisTemplate.keys("quote:*")).thenReturn(Set.of());

        mockMvc.perform(get("/api/market/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void quotes_withCachedTick_returnsSortedTicks() throws Exception {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.keys("quote:*")).thenReturn(Set.of("quote:TSLA", "quote:AAPL"));
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("quote:AAPL")).thenReturn(
                "{\"symbol\":\"AAPL\",\"bidPrice\":150.0,\"askPrice\":150.2,\"lastPrice\":150.1," +
                "\"bidSize\":100,\"askSize\":100,\"volume\":5000,\"timestamp\":0}");
        when(ops.get("quote:TSLA")).thenReturn(
                "{\"symbol\":\"TSLA\",\"bidPrice\":200.0,\"askPrice\":200.5,\"lastPrice\":200.2," +
                "\"bidSize\":50,\"askSize\":50,\"volume\":3000,\"timestamp\":0}");

        mockMvc.perform(get("/api/market/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].bidPrice").value(150.0))
                .andExpect(jsonPath("$[1].symbol").value("TSLA"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void quotes_malformedJson_isSkipped() throws Exception {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.keys("quote:*")).thenReturn(Set.of("quote:BAD"));
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("quote:BAD")).thenReturn("not-valid-json");

        mockMvc.perform(get("/api/market/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
