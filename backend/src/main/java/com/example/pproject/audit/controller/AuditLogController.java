package com.example.pproject.audit.controller;

import com.example.pproject.audit.dto.AuditLogFilterRequest;
import com.example.pproject.audit.dto.AuditLogResponse;
import com.example.pproject.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    // ADM-ADT-001: 로그 목록 조회
    @GetMapping
    @PreAuthorize("hasRole('APPROVEADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @ModelAttribute AuditLogFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getAuditLogs(filterRequest, pageable));
    }

    // ADM-ADT-002: 로그 상세 조회
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('APPROVEADMIN')")
    public ResponseEntity<AuditLogResponse> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.getAuditLog(id));
    }
}