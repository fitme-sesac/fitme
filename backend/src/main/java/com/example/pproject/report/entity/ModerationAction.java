package com.example.pproject.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_action", indexes = {
        @Index(name = "idx_admin_member_id", columnList = "admin_member_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionId;

    @Column(nullable = false, unique = true)
    private Long reportId;

    @Column(name = "admin_member_id")
    private Long adminMemberId;

    @Column(length = 20, nullable = false)
    private String decision; // ACCEPT, REJECT

    @Column(name = "sanction_level")
    private Integer sanctionLevel; // 제재 수준 (1~5)

    @Column(name = "restrict_days")
    private Integer restrictDays; // 제재 기간 (일)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason; // 판단 사유

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.decidedAt = now;
    }
}