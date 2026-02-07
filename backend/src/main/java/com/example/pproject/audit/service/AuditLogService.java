package com.example.pproject.audit.service;

import com.example.pproject.audit.dto.AuditLogFilterRequest;
import com.example.pproject.audit.dto.AuditLogResponse;
import com.example.pproject.audit.entity.AuditLog;
import com.example.pproject.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper; // JSON 변환용

    // 1. 로그 목록 조회
    public Page<AuditLogResponse> getAuditLogs(AuditLogFilterRequest request, Pageable pageable) {
        Specification<AuditLog> spec = AuditLogRepository.getFilter(request);
        return auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::from);
    }

    // 2. 로그 상세 조회
    public AuditLogResponse getAuditLog(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 로그입니다. ID: " + id));
        return AuditLogResponse.from(log);
    }

    // =================================================================
    // 3. [공통] 로그 기록 메서드 (다른 Service에서 호출)
    // Transactional Propagation.REQUIRES_NEW: 원래 작업이 실패해도 로그는 남길지,
    // 아니면 같이 롤백할지 결정. 여기서는 기본(REQUIRED) 사용.
    // =================================================================
    @Transactional
    public void logAction(Long actorId, String targetType, Long targetId, String action,
                          Object beforeDataObj, Object afterDataObj, String clientIp) {
        try {
            String beforeJson = beforeDataObj != null ? objectMapper.writeValueAsString(beforeDataObj) : null;
            String afterJson = afterDataObj != null ? objectMapper.writeValueAsString(afterDataObj) : null;

            AuditLog auditLog = AuditLog.builder()
                    .actorMemberId(actorId)
                    .targetType(targetType)
                    .targetId(targetId)
                    .action(action)
                    .beforeData(beforeJson)
                    .afterData(afterJson)
                    .clientIp(clientIp)
                    .build();

            auditLogRepository.save(auditLog);

        } catch (JsonProcessingException e) {
            log.error("Audit Log JSON converting error", e);
        }
    }
}