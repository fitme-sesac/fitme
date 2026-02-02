package com.example.pproject.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report")
@Getter // 👈 이게 있어야 getTitle()이 자동 생성됩니다.
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private Long reporterMemberId;

    @Column(length = 30, nullable = false)
    private String targetType;

    @Column(name = "target_job_id")
    private Long targetJobId;

    @Column(name = "target_member_id")
    private Long targetMemberId;

    @Column(length = 50, nullable = false)
    private String reasonCode;

    @Column(columnDefinition = "TEXT")
    private String reasonDetail;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "OPEN";
        }
    }
}