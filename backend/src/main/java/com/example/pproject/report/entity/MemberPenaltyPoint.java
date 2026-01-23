package com.example.pproject.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_penalty_point", indexes = {
        @Index(name = "idx_member_id", columnList = "member_id"),
        @Index(name = "idx_report_id", columnList = "report_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberPenaltyPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long penaltyId;

    @Column(nullable = false)
    private Long memberId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(nullable = false)
    private Integer points;  // 경고 점수

    @Column(length = 200, nullable = false)
    private String reason;  // 경고 사유

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
