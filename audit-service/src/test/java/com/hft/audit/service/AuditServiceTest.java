package com.hft.audit.service;

import com.hft.audit.entity.AuditEvent;
import com.hft.audit.repository.AuditEventRepository;
import com.hft.core.enums.OrderSide;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.event.OrderRejectedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditEventRepository repository;
    @InjectMocks private AuditService service;

    @Test
    void recordFill_savesCorrectEventType() {
        service.recordFill(fill("ord-1", "acc-1", "AAPL", 100, 150.0));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("FILL", captor.getValue().getEventType());
    }

    @Test
    void recordFill_populatesAllFields() {
        service.recordFill(fill("ord-1", "acc-1", "AAPL", 100, 150.0));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent saved = captor.getValue();
        assertEquals("ord-1", saved.getOrderId());
        assertEquals("acc-1", saved.getAccountId());
        assertEquals("AAPL", saved.getSymbol());
        assertTrue(saved.getDetails().contains("qty=100"));
        assertTrue(saved.getDetails().contains("150.00"));
    }

    @Test
    void recordRejection_savesRejectionEventWithReason() {
        OrderRejectedEvent event = new OrderRejectedEvent();
        event.setOrderId("ord-2");
        event.setAccountId("acc-1");
        event.setSymbol("TSLA");
        event.setReason("INSUFFICIENT_FUNDS");

        service.recordRejection(event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("REJECTION", captor.getValue().getEventType());
        assertTrue(captor.getValue().getDetails().contains("INSUFFICIENT_FUNDS"));
    }

    @Test
    void getByAccount_delegatesToRepository() {
        when(repository.findByAccountIdOrderByEventTimestampDesc("acc-1")).thenReturn(List.of());

        service.getByAccount("acc-1");

        verify(repository).findByAccountIdOrderByEventTimestampDesc("acc-1");
    }

    @Test
    void getByOrder_delegatesToRepository() {
        when(repository.findByOrderIdOrderByEventTimestampDesc("ord-1")).thenReturn(List.of());

        service.getByOrder("ord-1");

        verify(repository).findByOrderIdOrderByEventTimestampDesc("ord-1");
    }

    private OrderFilledEvent fill(String orderId, String accountId, String symbol, long qty, double price) {
        OrderFilledEvent e = new OrderFilledEvent();
        e.setOrderId(orderId);
        e.setAccountId(accountId);
        e.setSymbol(symbol);
        e.setSide(OrderSide.BUY);
        e.setFilledQty(qty);
        e.setFillPrice(price);
        e.setFillId("fill-" + System.nanoTime());
        return e;
    }
}
