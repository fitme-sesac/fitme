package com.example.pproject.application.entity;

import com.example.pproject.Constant.ApplicationStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@Table(name = "job_application", uniqueConstraints = {
        @UniqueConstraint(name = "uq_job_application", columnNames = {"job_id", "member_id"})
})
public class JobApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private UserEntity member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @ColumnDefault("'SUBMITTED'")
    private ApplicationStatus status;

    @Column(columnDefinition = "JSONB")
    private String answers; // 사전 질문 답변 (JSON 문자열)

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "contact_disclosed_at")
    private LocalDateTime contactDisclosedAt;

    @Builder
    public JobApplication(JobEntity job, UserEntity member, Resume resume, String answers) {
        this.job = job;
        this.member = member;
        this.resume = resume;
        this.answers = answers;
        this.status = ApplicationStatus.SUBMITTED;
        this.appliedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = ApplicationStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }
}