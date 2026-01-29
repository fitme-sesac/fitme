package com.example.pproject.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private Long reporterMemberId;  // 신고자 ID

    @Column(length = 30, nullable = false)
    private String targetType;  // JOB_POSTING, MEMBER, ETC

    @Column(name = "target_job_id")
    private Long targetJobId;

    @Column(name = "target_member_id")
    private Long targetMemberId;

    @Column(length = 50, nullable = false)
    private String reasonCode;  // 신고 사유 코드

    @Column(columnDefinition = "TEXT")
    private String reasonDetail;  // 신고 사유 상세

    @Column(length = 20, nullable = false)
    private String status;  // OPEN, ACCEPTED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;


        if (this.status == null) {
            this.status = "OPEN";
        }
    }
}