package com.hft.notification.service;

import com.hft.core.event.OrderFilledEvent;
import com.hft.core.event.OrderRejectedEvent;
import com.hft.notification.dto.TradeNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyFill(OrderFilledEvent event) {
        TradeNotification n = new TradeNotification();
        n.setType(TradeNotification.Type.FILL);
        n.setAccountId(event.getAccountId());
        n.setSymbol(event.getSymbol());
        n.setMessage(String.format("FILLED %d %s @ $%.2f",
                event.getFilledQty(), event.getSymbol(), event.getFillPrice()));
        n.setTimestamp(System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/account/" + event.getAccountId(), n);
    }

    public void notifyRejection(OrderRejectedEvent event) {
        TradeNotification n = new TradeNotification();
        n.setType(TradeNotification.Type.REJECTION);
        n.setAccountId(event.getAccountId());
        n.setSymbol(event.getSymbol());
        n.setMessage("REJECTED: " + event.getReason());
        n.setTimestamp(System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/account/" + event.getAccountId(), n);
    }
}
