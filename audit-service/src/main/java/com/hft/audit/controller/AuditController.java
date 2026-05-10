package com.hft.audit.controller;

import com.hft.audit.entity.AuditEvent;
import com.hft.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/{accountId}")
    public ResponseEntity<List<AuditEvent>> getByAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(auditService.getByAccount(accountId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<AuditEvent>> getByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(auditService.getByOrder(orderId));
    }
}
