package com.hft.orderentry.controller;

import tools.jackson.databind.ObjectMapper;
import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderStatus;
import com.hft.core.enums.OrderType;
import com.hft.orderentry.dto.OrderRequest;
import com.hft.orderentry.dto.OrderResponse;
import com.hft.orderentry.repository.TraderAccountRepository;
import com.hft.orderentry.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"orders.submitted"})
@DirtiesContext
class OrderControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @MockitoBean private OrderService orderService;
    @MockitoBean private TraderAccountRepository traderAccountRepository;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "test-account-001")
    void submitOrder_validRequest_returns202WithBody() throws Exception {
        OrderResponse stubResponse = OrderResponse.builder()
                .orderId("order-uuid-123")
                .clientOrderId("client-1")
                .symbol("AAPL")
                .status(OrderStatus.SUBMITTED)
                .submittedAt(System.currentTimeMillis())
                .build();
        when(orderService.submitOrder(any())).thenReturn(stubResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").value("order-uuid-123"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @WithMockUser
    void submitOrder_missingRequiredFields_returns400() throws Exception {
        String incompleteJson = "{\"clientOrderId\":\"c1\",\"quantity\":100}";

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void submitOrder_zeroQuantity_returns400() throws Exception {
        OrderRequest badRequest = OrderRequest.builder()
                .clientOrderId("client-1").symbol("AAPL")
                .side(OrderSide.BUY).type(OrderType.MARKET)
                .quantity(0).limitPrice(0)
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitOrder_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    private OrderRequest validRequest() {
        return OrderRequest.builder()
                .clientOrderId("client-1")
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(100)
                .limitPrice(0)
                .build();
    }
}
