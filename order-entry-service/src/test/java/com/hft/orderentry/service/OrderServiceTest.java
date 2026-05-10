package com.hft.orderentry.service;

import tools.jackson.databind.ObjectMapper;
import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderStatus;
import com.hft.core.enums.OrderType;
import com.hft.core.event.OrderSubmittedEvent;
import com.hft.core.model.Tick;
import com.hft.orderentry.dto.OrderRequest;
import com.hft.orderentry.dto.OrderResponse;
import com.hft.orderentry.kafka.OrderKafkaProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderKafkaProducer kafkaProducer;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private ValueOperations<String, String> valueOps;
    @InjectMocks private OrderService orderService;

    @BeforeEach
    void setupSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-account-001", null, List.of())
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitOrder_noQuoteInCache_stillPublishesAndReturnsSubmitted() {
        when(valueOps.get(anyString())).thenReturn(null);

        OrderResponse response = orderService.submitOrder(buildRequest("client-1", "AAPL", OrderSide.BUY, OrderType.MARKET, 100, 0));

        assertNotNull(response.getOrderId());
        assertEquals("client-1", response.getClientOrderId());
        assertEquals("AAPL", response.getSymbol());
        assertEquals(OrderStatus.SUBMITTED, response.getStatus());
        assertTrue(response.getSubmittedAt() > 0);
        verify(kafkaProducer).publish(any(OrderSubmittedEvent.class));
    }

    @Test
    void submitOrder_quoteFreshInCache_publishesAndReturnsSubmitted() throws Exception {
        String tickJson = "{\"symbol\":\"AAPL\",\"bidPrice\":150.0,\"askPrice\":150.05}";
        when(valueOps.get("quote:AAPL")).thenReturn(tickJson);
        Tick tick = new Tick();
        when(objectMapper.readValue(tickJson, Tick.class)).thenReturn(tick);

        OrderResponse response = orderService.submitOrder(buildRequest("client-2", "AAPL", OrderSide.SELL, OrderType.LIMIT, 50, 150.0));

        assertEquals(OrderStatus.SUBMITTED, response.getStatus());
        verify(kafkaProducer).publish(any(OrderSubmittedEvent.class));
    }

    @Test
    void submitOrder_kafkaEventCarriesCorrectFields() {
        when(valueOps.get(anyString())).thenReturn(null);

        orderService.submitOrder(buildRequest("client-3", "TSLA", OrderSide.BUY, OrderType.MARKET, 10, 0));

        ArgumentCaptor<OrderSubmittedEvent> captor = ArgumentCaptor.forClass(OrderSubmittedEvent.class);
        verify(kafkaProducer).publish(captor.capture());

        OrderSubmittedEvent event = captor.getValue();
        assertEquals("TSLA", event.getSymbol());
        assertEquals("test-account-001", event.getAccountId());
        assertEquals("client-3", event.getClientOrderId());
        assertEquals(10, event.getQuantity());
        assertNotNull(event.getOrderId());
    }

    @Test
    void submitOrder_eachCallGeneratesUniqueOrderId() {
        when(valueOps.get(anyString())).thenReturn(null);
        OrderRequest req = buildRequest("c1", "AAPL", OrderSide.BUY, OrderType.MARKET, 1, 0);

        String id1 = orderService.submitOrder(req).getOrderId();
        String id2 = orderService.submitOrder(req).getOrderId();

        assertNotEquals(id1, id2);
    }

    private OrderRequest buildRequest(String clientOrderId, String symbol,
                                       OrderSide side, OrderType type,
                                       long qty, double limitPrice) {
        return OrderRequest.builder()
                .clientOrderId(clientOrderId).symbol(symbol)
                .side(side).type(type).quantity(qty).limitPrice(limitPrice)
                .build();
    }
}
