package com.example.pproject.proposal.entity;

import com.example.pproject.Constant.ProposalStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

/**
 * 기업이 구직자에게 보내는 포지션 제안 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@Table(name = "talent_proposal", uniqueConstraints = {
        @UniqueConstraint(name = "uq_talent_proposal", columnNames = {"employer_id", "candidate_id", "job_id"})
})
public class TalentProposal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proposal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private EmployerEntity employer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private UserEntity candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private JobEntity job; // 특정 공고에 대한 제안 (nullable - 일반 스카우트 제안일 수도 있음)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @ColumnDefault("'PENDING'")
    private ProposalStatus status;

    @Column(name = "title", nullable = false, length = 200)
    private String title; // 제안 제목

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // 제안 메시지

    @Column(name = "offered_salary", length = 100)
    private String offeredSalary; // 제안 연봉

    @Column(name = "offered_position", length = 100)
    private String offeredPosition; // 제안 포지션

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 제안 만료일

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt; // 구직자가 확인한 시간

    @Column(name = "responded_at")
    private LocalDateTime respondedAt; // 응답한 시간

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage; // 구직자 응답 메시지

    @Builder
    public TalentProposal(EmployerEntity employer, UserEntity candidate, JobEntity job,
                          String title, String message, String offeredSalary,
                          String offeredPosition, LocalDateTime expiresAt) {
        this.employer = employer;
        this.candidate = candidate;
        this.job = job;
        this.title = title;
        this.message = message;
        this.offeredSalary = offeredSalary;
        this.offeredPosition = offeredPosition;
        this.expiresAt = expiresAt;
        this.status = ProposalStatus.PENDING;
    }

    public void markAsViewed() {
        if (this.status == ProposalStatus.PENDING) {
            this.status = ProposalStatus.VIEWED;
            this.viewedAt = LocalDateTime.now();
        }
    }

    public void accept(String responseMessage) {
        this.status = ProposalStatus.ACCEPTED;
        this.responseMessage = responseMessage;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject(String responseMessage) {
        this.status = ProposalStatus.REJECTED;
        this.responseMessage = responseMessage;
        this.respondedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = ProposalStatus.CANCELED;
    }

    public void expire() {
        if (this.status == ProposalStatus.PENDING || this.status == ProposalStatus.VIEWED) {
            this.status = ProposalStatus.EXPIRED;
        }
    }

    public boolean isExpired() {
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
    }
}
