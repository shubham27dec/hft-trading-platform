package com.hft.execution.venue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import com.hft.execution.event.OrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlpacaExecutionVenueTest {

    @Mock HttpClient http;
    @Mock HttpResponse<String> response;

    private AlpacaExecutionVenue venue;

    @BeforeEach
    void setUp() throws Exception {
        venue = new AlpacaExecutionVenue("key", "secret", http, new ObjectMapper());
        lenient().doReturn(response).when(http).send(any(), any());
    }

    @Test
    void name_returnsAlpaca() {
        assertEquals("ALPACA", venue.name());
    }

    @Test
    void getQuote_validResponse_returnsAskBid() throws Exception {
        when(response.body()).thenReturn(
                "{\"AAPL\":{\"latestQuote\":{\"ap\":150.10,\"bp\":149.90}}}");

        VenueQuote quote = venue.getQuote("AAPL");

        assertEquals(150.10, quote.ask(), 0.001);
        assertEquals(149.90, quote.bid(), 0.001);
    }

    @Test
    void getQuote_httpThrows_returnsZeroQuote() throws Exception {
        when(http.send(any(), any())).thenThrow(new RuntimeException("timeout"));

        VenueQuote quote = venue.getQuote("AAPL");

        assertEquals(0, quote.ask());
        assertEquals(0, quote.bid());
    }

    @Test
    void execute_validResponse_returnsFillResult() throws Exception {
        when(response.body()).thenReturn(
                "{\"id\":\"fill-123\",\"filled_avg_price\":\"150.05\",\"filled_qty\":\"100\"}");

        ExecutionResult result = venue.execute(buildEvent());

        assertEquals("fill-123", result.fillId());
        assertEquals(150.05, result.fillPrice(), 0.001);
        assertEquals(100, result.filledQty());
    }

    @Test
    void execute_httpThrows_throwsRuntimeException() throws Exception {
        when(http.send(any(), any())).thenThrow(new RuntimeException("connection refused"));

        assertThrows(RuntimeException.class, () -> venue.execute(buildEvent()));
    }

    private OrderEvent buildEvent() {
        OrderEvent event = new OrderEvent();
        event.orderId = "order-1";
        event.symbol = "AAPL";
        event.side = OrderSide.BUY;
        event.type = OrderType.MARKET;
        event.quantity = 100;
        event.routedAsk = 150.10;
        return event;
    }
}
