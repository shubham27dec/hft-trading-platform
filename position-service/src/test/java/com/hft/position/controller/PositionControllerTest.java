package com.hft.position.controller;

import com.hft.core.model.Position;
import com.hft.position.service.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"orders.filled", "market.ticks"})
@DirtiesContext
class PositionControllerTest {

    @Autowired private WebApplicationContext wac;
    @MockitoBean private PositionService positionService;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void getPositions_returnsListFromService() throws Exception {
        Position p = new Position();
        p.setAccountId("acc-001");
        p.setSymbol("AAPL");
        p.setNetQty(100);
        p.setAvgCostBasis(150.0);
        p.setRealizedPnL(0.0);
        p.setUnrealizedPnL(500.0);

        when(positionService.getPositions("acc-001")).thenReturn(List.of(p));

        mockMvc.perform(get("/api/positions/acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value("acc-001"))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].netQty").value(100))
                .andExpect(jsonPath("$[0].avgCostBasis").value(150.0))
                .andExpect(jsonPath("$[0].unrealizedPnL").value(500.0));
    }

    @Test
    void getPositions_emptyAccount_returnsEmptyList() throws Exception {
        when(positionService.getPositions("acc-empty")).thenReturn(List.of());

        mockMvc.perform(get("/api/positions/acc-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPositions_multiplePositions_returnsAll() throws Exception {
        Position aapl = new Position();
        aapl.setAccountId("acc-001");
        aapl.setSymbol("AAPL");
        aapl.setNetQty(100);

        Position tsla = new Position();
        tsla.setAccountId("acc-001");
        tsla.setSymbol("TSLA");
        tsla.setNetQty(-50);

        when(positionService.getPositions("acc-001")).thenReturn(List.of(aapl, tsla));

        mockMvc.perform(get("/api/positions/acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[1].symbol").value("TSLA"))
                .andExpect(jsonPath("$[1].netQty").value(-50));
    }
}
