package com.example.pproject.resume.entity;

import com.example.pproject.common.entity.BaseSoftDeleteEntity;
import com.example.pproject.Constant.ResumeField;
import com.example.pproject.Constant.ResumeStatus;
import com.example.pproject.Constant.SummaryStatus;
import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@Table(name = "resume", indexes = {
        @Index(name = "idx_resume_member_status", columnList = "member_id, status"),
        @Index(name = "idx_resume_summary_todo", columnList = "summary_status, updated_at")
})
public class Resume extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resume_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 200)
    private String tagline;

    @Column(name = "is_primary", nullable = false)
    @ColumnDefault("false")
    private boolean primary;

    @Column(name = "is_public", nullable = false)
    @ColumnDefault("false")
    private boolean publicOption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'DRAFT'")
    private ResumeStatus status;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    // AI 요약 & 임베딩 필드
    // 1. 타겟 공고 ID
    @Column(name = "target_job_id")
    private Long targetJobId;

    // 2. 이력서 원문 (AI 분석 대상)
    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResumeField field;

    // 3. AI가 작성해준 요약글
    @Lob
    private String summary;

    // 4. 벡터 임베딩 (PostgreSQL vector 타입, 1536차원)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private List<Double> embedding;

    // 5. AI 처리 상태 (대기중, 완료, 실패 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", nullable = false, length = 20)
    @ColumnDefault("'NONE'")
    private SummaryStatus summaryStatus;

    @Column(name = "preference_location", length = 80)
    private String preferenceLocation;

    @Column(name = "preference_salary", length = 80)
    private String preferenceSalary;

    @Column(name = "employment_type", length = 80)
    private String employmentType;

    // 자식 연관관계
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeCareer> careers = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeProject> projects = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeCertificate> certificates = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeLink> links = new ArrayList<>();

    @OneToOne(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private ResumeProfile profile;

    // 생성자 & 편의 메서드
    @Builder
    public Resume(UserEntity user, String title, ResumeField field, boolean primary) {
        this.user = user;
        this.title = title;
        this.field = field;
        this.primary = primary;
        this.status = ResumeStatus.DRAFT;
        this.summaryStatus = SummaryStatus.NONE;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // 임베딩 값 업데이트 메서드
    public void updateEmbedding(List<Double> embeddingVector) {
        this.embedding = embeddingVector;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // AI 분석 요청 시 상태 변경
    public void requestAiAnalysis(Long targetJobId) {
        this.targetJobId = targetJobId;
        this.summaryStatus = SummaryStatus.PENDING;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // AI 분석 완료 시 결과 저장
    public void completeAiAnalysis(String summary) {
        this.summary = summary;
        this.summaryStatus = SummaryStatus.COMPLETED;
    }
}