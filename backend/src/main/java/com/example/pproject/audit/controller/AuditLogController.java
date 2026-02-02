package com.example.pproject.audit.controller;

import com.example.pproject.audit.dto.AuditLogFilterRequest;
import com.example.pproject.audit.dto.AuditLogResponse;
import com.example.pproject.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('APPROVEADMIN', 'MASTER')") // 클래스 레벨 권한 - 승인관리자, 마스터만 접근 가능
public class AuditLogController {

    private final AuditLogService auditLogService;

    // ADM-ADT-001: 로그 목록 조회
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @ModelAttribute AuditLogFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("감사 로그 목록 조회: actorId={}, targetType={}, action={}, startDate={}, endDate={}",
                filterRequest.getActorMemberId(), filterRequest.getTargetType(),
                filterRequest.getAction(), filterRequest.getStartDate(), filterRequest.getEndDate());

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getAuditLogs(filterRequest, pageable));
    }

    // ADM-ADT-002: 로그 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponse> getAuditLog(@PathVariable Long id) {
        log.info("감사 로그 상세 조회: id={}", id);
        return ResponseEntity.ok(auditLogService.getAuditLog(id));
    }
}