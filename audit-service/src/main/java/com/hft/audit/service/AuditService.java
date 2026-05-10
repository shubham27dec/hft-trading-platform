package com.hft.audit.service;

import com.hft.audit.entity.AuditEvent;
import com.hft.audit.repository.AuditEventRepository;
import com.hft.core.event.OrderFilledEvent;
import com.hft.core.event.OrderRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;

    public void recordFill(OrderFilledEvent event) {
        AuditEvent record = new AuditEvent();
        record.setEventType("FILL");
        record.setOrderId(event.getOrderId());
        record.setAccountId(event.getAccountId());
        record.setSymbol(event.getSymbol());
        record.setDetails(String.format("fillId=%s,qty=%d,price=%.2f,side=%s",
                event.getFillId(), event.getFilledQty(), event.getFillPrice(), event.getSide()));
        record.setEventTimestamp(event.getFilledAt());
        record.setRecordedAt(System.currentTimeMillis());
        repository.save(record);
    }

    public void recordRejection(OrderRejectedEvent event) {
        AuditEvent record = new AuditEvent();
        record.setEventType("REJECTION");
        record.setOrderId(event.getOrderId());
        record.setAccountId(event.getAccountId());
        record.setSymbol(event.getSymbol());
        record.setDetails("reason=" + event.getReason());
        record.setEventTimestamp(event.getRejectedAt());
        record.setRecordedAt(System.currentTimeMillis());
        repository.save(record);
    }

    public List<AuditEvent> getByAccount(String accountId) {
        return repository.findByAccountIdOrderByEventTimestampDesc(accountId);
    }

    public List<AuditEvent> getByOrder(String orderId) {
        return repository.findByOrderIdOrderByEventTimestampDesc(orderId);
    }
}
