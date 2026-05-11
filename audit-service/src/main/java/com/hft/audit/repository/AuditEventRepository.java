package com.hft.audit.repository;

import com.hft.audit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByAccountIdOrderByEventTimestampDesc(String accountId);
    List<AuditEvent> findByOrderIdOrderByEventTimestampDesc(String orderId);
}
