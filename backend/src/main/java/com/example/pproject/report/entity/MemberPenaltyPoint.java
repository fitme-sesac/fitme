package com.example.pproject.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_penalty_point") // ✅ 여기도 indexes 삭제!
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
    private Integer points;

    @Column(length = 200, nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}