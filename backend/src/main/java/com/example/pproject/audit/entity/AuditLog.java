package com.example.pproject.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 누가 (관리자 ID)
    @Column(name = "actor_member_id", nullable = false)
    private Long actorMemberId;

    // 무엇을 (대상 유형: MEMBER, REPORT, NOTICE 등)
    @Column(name = "target_type", nullable = false)
    private String targetType;

    // 대상의 ID
    @Column(name = "target_id")
    private Long targetId;

    // 어떤 행동 (UPDATE, DELETE, CREATE, LOGIN...)
    @Column(name = "action", nullable = false)
    private String action;

    // 변경 전 데이터 (JSON 형태의 문자열)
    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;

    // 변경 후 데이터 (JSON 형태의 문자열)
    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;

    // 접속 IP (선택사항)
    @Column(name = "client_ip")
    private String clientIp;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}