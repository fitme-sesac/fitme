package com.example.pproject.audit.dto;

import com.example.pproject.audit.entity.AuditLog;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long actorMemberId;
    private String targetType;
    private Long targetId;
    private String action;
    private String beforeData; // 프론트에서 JSON.parse 해서 비교
    private String afterData;
    private String clientIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorMemberId(log.getActorMemberId())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .action(log.getAction())
                .beforeData(log.getBeforeData())
                .afterData(log.getAfterData())
                .clientIp(log.getClientIp())
                .createdAt(log.getCreatedAt())
                .build();
    }
}