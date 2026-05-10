package com.hft.riskdashboard.controller;

import com.hft.riskdashboard.model.RiskMetrics;
import com.hft.riskdashboard.service.RiskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"orders.filled", "orders.rejected"})
@DirtiesContext
class RiskControllerTest {

    @Autowired private WebApplicationContext wac;
    @MockitoBean private RiskService riskService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void getSnapshot_returnsMetricsForAccount() throws Exception {
        RiskMetrics m = new RiskMetrics();
        m.setAccountId("acc-001");
        m.setFillCount(5);
        m.setRejectCount(1);
        m.setGrossExposure(75000.0);
        m.setHaltActive(false);

        when(riskService.getSnapshot("acc-001")).thenReturn(m);

        mockMvc.perform(get("/api/risk/acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acc-001"))
                .andExpect(jsonPath("$.fillCount").value(5))
                .andExpect(jsonPath("$.rejectCount").value(1))
                .andExpect(jsonPath("$.grossExposure").value(75000.0))
                .andExpect(jsonPath("$.haltActive").value(false));
    }

    @Test
    void getAllSnapshots_returnsAllAccounts() throws Exception {
        RiskMetrics m1 = new RiskMetrics();
        m1.setAccountId("acc-001");
        RiskMetrics m2 = new RiskMetrics();
        m2.setAccountId("acc-002");

        when(riskService.getAllSnapshots()).thenReturn(List.of(m1, m2));

        mockMvc.perform(get("/api/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void simulatePartition_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/risk/simulate-partition"))
                .andExpect(status().isOk());

        verify(riskService).simulatePartition();
    }

    @Test
    void restore_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/risk/restore"))
                .andExpect(status().isOk());

        verify(riskService).restore();
    }
}
