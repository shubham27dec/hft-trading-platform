package com.hft.execution.handler;

import com.hft.core.enums.OrderSide;
import com.hft.core.enums.OrderType;
import com.hft.execution.event.OrderEvent;
import com.hft.execution.kafka.FillKafkaProducer;
import com.hft.execution.venue.ExecutionResult;
import com.hft.execution.venue.ExecutionVenue;
import com.hft.execution.wal.ChronicleWAL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionHandlerTest {

    @Mock ExecutionVenue alpaca;
    @Mock ExecutionVenue simulated;
    @Mock ChronicleWAL wal;
    @Mock FillKafkaProducer kafkaProducer;

    private ExecutionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ExecutionHandler(alpaca, simulated, wal, kafkaProducer);
    }

    @Test
    void riskPassed_alpacaVenue_executesAndPublishesFill() {
        when(alpaca.execute(any())).thenReturn(new ExecutionResult("fill-1", 150.05, 100));
        OrderEvent event = buildEvent("ALPACA", true);

        handler.onEvent(event, 0, false);

        assertTrue(event.filled);
        assertEquals("fill-1", event.fillId);
        assertEquals(150.05, event.fillPrice, 0.001);
        verify(wal).append("EXECUTING", event);
        verify(wal).append("FILLED", event);
        verify(kafkaProducer).publishFilled(event);
        verify(kafkaProducer, never()).publishRejected(any());
    }

    @Test
    void riskPassed_simulatedVenue_executesAndPublishesFill() {
        when(simulated.execute(any())).thenReturn(new ExecutionResult("fill-2", 150.10, 100));
        OrderEvent event = buildEvent("SIMULATED", true);

        handler.onEvent(event, 0, false);

        assertTrue(event.filled);
        verify(kafkaProducer).publishFilled(event);
    }

    @Test
    void riskFailed_publishesRejection() {
        OrderEvent event = buildEvent("ALPACA", false);
        event.rejectionReason = "Trading halted";

        handler.onEvent(event, 0, false);

        assertFalse(event.filled);
        verify(wal).append("REJECTED", event);
        verify(kafkaProducer).publishRejected(event);
        verify(kafkaProducer, never()).publishFilled(any());
        verifyNoInteractions(alpaca);
    }

    @Test
    void executionThrows_publishesRejection() {
        when(alpaca.execute(any())).thenThrow(new RuntimeException("network error"));
        OrderEvent event = buildEvent("ALPACA", true);

        handler.onEvent(event, 0, false);

        assertFalse(event.filled);
        assertFalse(event.riskPassed);
        verify(kafkaProducer).publishRejected(event);
    }

    private OrderEvent buildEvent(String venue, boolean riskPassed) {
        OrderEvent event = new OrderEvent();
        event.orderId = "order-1";
        event.clientOrderId = "client-1";
        event.symbol = "AAPL";
        event.side = OrderSide.BUY;
        event.type = OrderType.MARKET;
        event.quantity = 100;
        event.riskPassed = riskPassed;
        event.venue = venue;
        event.routedAsk = 150.10;
        event.routedBid = 149.90;
        return event;
    }
}
