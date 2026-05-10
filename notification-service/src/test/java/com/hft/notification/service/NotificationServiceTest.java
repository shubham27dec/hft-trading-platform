package com.hft.notification.service;

import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.event.OrderRejectedEvent;
import com.hft.notification.dto.TradeNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private NotificationService service;

    @Test
    void notifyFill_sendsToCorrectAccountTopic() {
        service.notifyFill(fill("acc-001", "AAPL", 100, 150.0));

        verify(messagingTemplate).convertAndSend(
                eq("/topic/account/acc-001"),
                org.mockito.ArgumentMatchers.any(TradeNotification.class));
    }

    @Test
    void notifyFill_payloadContainsFillDetails() {
        service.notifyFill(fill("acc-001", "AAPL", 100, 150.0));

        ArgumentCaptor<TradeNotification> captor = ArgumentCaptor.forClass(TradeNotification.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/account/acc-001"), captor.capture());

        TradeNotification n = captor.getValue();
        assertEquals(TradeNotification.Type.FILL, n.getType());
        assertEquals("acc-001", n.getAccountId());
        assertEquals("AAPL", n.getSymbol());
        assertTrue(n.getMessage().contains("AAPL"));
        assertTrue(n.getMessage().contains("150.00"));
    }

    @Test
    void notifyRejection_sendsRejectionWithReason() {
        OrderRejectedEvent event = new OrderRejectedEvent();
        event.setAccountId("acc-001");
        event.setSymbol("TSLA");
        event.setReason("INSUFFICIENT_FUNDS");
        event.setOrderId("ord-1");

        service.notifyRejection(event);

        ArgumentCaptor<TradeNotification> captor = ArgumentCaptor.forClass(TradeNotification.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/account/acc-001"), captor.capture());

        TradeNotification n = captor.getValue();
        assertEquals(TradeNotification.Type.REJECTION, n.getType());
        assertTrue(n.getMessage().contains("INSUFFICIENT_FUNDS"));
    }

    private OrderFilledEvent fill(String accountId, String symbol, long qty, double price) {
        OrderFilledEvent e = new OrderFilledEvent();
        e.setAccountId(accountId);
        e.setSymbol(symbol);
        e.setSide(OrderSide.BUY);
        e.setFilledQty(qty);
        e.setFillPrice(price);
        e.setFillId("fill-" + System.nanoTime());
        return e;
    }
}
