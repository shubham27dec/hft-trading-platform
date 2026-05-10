package com.hft.audit.controller;

import com.hft.audit.entity.AuditEvent;
import com.hft.audit.service.AuditService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"orders.filled", "orders.rejected"})
@DirtiesContext
class AuditControllerTest {

    @Autowired private WebApplicationContext wac;
    @MockitoBean private AuditService auditService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void getByAccount_returnsAuditEvents() throws Exception {
        AuditEvent e = new AuditEvent();
        e.setEventType("FILL");
        e.setAccountId("acc-001");
        e.setSymbol("AAPL");
        e.setOrderId("ord-1");

        when(auditService.getByAccount("acc-001")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/audit/acc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("FILL"))
                .andExpect(jsonPath("$[0].accountId").value("acc-001"))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    void getByOrder_returnsAuditEvents() throws Exception {
        AuditEvent e = new AuditEvent();
        e.setEventType("REJECTION");
        e.setOrderId("ord-1");
        e.setAccountId("acc-001");

        when(auditService.getByOrder("ord-1")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/audit/order/ord-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("REJECTION"))
                .andExpect(jsonPath("$[0].orderId").value("ord-1"));
    }

    @Test
    void getByAccount_empty_returnsEmptyList() throws Exception {
        when(auditService.getByAccount("acc-empty")).thenReturn(List.of());

        mockMvc.perform(get("/api/audit/acc-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
