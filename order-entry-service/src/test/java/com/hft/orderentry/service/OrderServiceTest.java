package com.hft.orderentry.service;

import tools.jackson.databind.ObjectMapper;
import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderStatus;
import com.hft.core.enums.OrderType;
import com.hft.core.event.OrderSubmittedEvent;
import com.hft.core.model.Tick;
import com.hft.orderentry.client.AlpacaQuoteClient;
import com.hft.orderentry.client.AlpacaSnapshotEntry;
import com.hft.orderentry.dto.OrderRequest;
import com.hft.orderentry.dto.OrderResponse;
import com.hft.orderentry.exception.QuoteUnavailableException;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderKafkaProducer kafkaProducer;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private AlpacaQuoteClient alpacaQuoteClient;
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
    void submitOrder_quoteFreshInCache_publishesWithCacheSource() throws Exception {
        String tickJson = "{\"symbol\":\"AAPL\",\"bidPrice\":150.0,\"askPrice\":150.10}";
        when(valueOps.get("quote:AAPL")).thenReturn(tickJson);
        Tick tick = new Tick();
        tick.setBidPrice(150.0);
        tick.setAskPrice(150.10);
        when(objectMapper.readValue(tickJson, Tick.class)).thenReturn(tick);

        OrderResponse response = orderService.submitOrder(buildRequest("client-1", "AAPL", OrderSide.BUY, OrderType.MARKET, 100, 0));

        assertEquals(OrderStatus.SUBMITTED, response.getStatus());
        assertEquals(150.05, response.getQuotePrice(), 0.001);
        verify(kafkaProducer).publish(any(OrderSubmittedEvent.class));
        verifyNoInteractions(alpacaQuoteClient);
    }

    @Test
    void submitOrder_cacheMiss_fallsBackToAlpacaAndPublishes() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(alpacaQuoteClient.getSnapshots(anySet()))
                .thenReturn(Map.of("AAPL", buildSnapshotEntry(150.5, 150.5)));

        OrderResponse response = orderService.submitOrder(buildRequest("client-2", "AAPL", OrderSide.BUY, OrderType.MARKET, 100, 0));

        assertEquals(OrderStatus.SUBMITTED, response.getStatus());
        assertEquals(150.5, response.getQuotePrice(), 0.001);
        verify(kafkaProducer).publish(any(OrderSubmittedEvent.class));
    }

    @Test
    void submitOrder_cacheMissAndAlpacaFails_throwsQuoteUnavailableException() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(alpacaQuoteClient.getSnapshots(anySet())).thenThrow(new QuoteUnavailableException("AAPL"));

        assertThrows(QuoteUnavailableException.class,
                () -> orderService.submitOrder(buildRequest("client-3", "AAPL", OrderSide.BUY, OrderType.MARKET, 100, 0)));

        verify(kafkaProducer, never()).publish(any());
    }

    @Test
    void submitOrder_kafkaEventCarriesCorrectFields() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(alpacaQuoteClient.getSnapshots(anySet()))
                .thenReturn(Map.of("TSLA", buildSnapshotEntry(200.0, 200.0)));

        orderService.submitOrder(buildRequest("client-4", "TSLA", OrderSide.BUY, OrderType.MARKET, 10, 0));

        ArgumentCaptor<OrderSubmittedEvent> captor = ArgumentCaptor.forClass(OrderSubmittedEvent.class);
        verify(kafkaProducer).publish(captor.capture());

        OrderSubmittedEvent event = captor.getValue();
        assertEquals("TSLA", event.getSymbol());
        assertEquals("test-account-001", event.getAccountId());
        assertEquals("client-4", event.getClientOrderId());
        assertEquals(10, event.getQuantity());
        assertNotNull(event.getOrderId());
    }

    @Test
    void submitOrder_eachCallGeneratesUniqueOrderId() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(alpacaQuoteClient.getSnapshots(anySet()))
                .thenReturn(Map.of("AAPL", buildSnapshotEntry(100.0, 100.0)));
        OrderRequest req = buildRequest("c1", "AAPL", OrderSide.BUY, OrderType.MARKET, 1, 0);

        String id1 = orderService.submitOrder(req).getOrderId();
        String id2 = orderService.submitOrder(req).getOrderId();

        assertNotEquals(id1, id2);
    }

    private AlpacaSnapshotEntry buildSnapshotEntry(double ask, double bid) {
        AlpacaSnapshotEntry entry = new AlpacaSnapshotEntry();
        AlpacaSnapshotEntry.Quote quote = new AlpacaSnapshotEntry.Quote();
        quote.setAp(ask);
        quote.setBp(bid);
        entry.setLatestQuote(quote);
        return entry;
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
