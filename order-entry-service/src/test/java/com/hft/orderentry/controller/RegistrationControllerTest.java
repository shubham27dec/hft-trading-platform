package com.hft.orderentry.controller;

import tools.jackson.databind.ObjectMapper;
import com.hft.orderentry.client.AlpacaQuoteClient;
import com.hft.orderentry.dto.RegistrationRequest;
import com.hft.orderentry.repository.TraderAccountRepository;
import com.hft.orderentry.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
class RegistrationControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @MockitoBean private RegistrationService registrationService;
    @MockitoBean private TraderAccountRepository traderAccountRepository;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;
    @MockitoBean private AlpacaQuoteClient alpacaQuoteClient;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void register_success_returns200WithAccountId() throws Exception {
        when(registrationService.register(any())).thenReturn("abc-123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("newuser", "new@hft.local", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("abc-123"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void register_duplicateUsername_returns400() throws Exception {
        when(registrationService.register(any()))
                .thenThrow(new IllegalArgumentException("Username already taken"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("trader", "dup@hft.local", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    @Test
    void register_serviceError_returns500() throws Exception {
        when(registrationService.register(any()))
                .thenThrow(new RuntimeException("Keycloak unreachable"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("user2", "u2@hft.local", "password123"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("user3", "not-an-email", "password123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("user4", "u4@hft.local", "short"))))
                .andExpect(status().isBadRequest());
    }
}
