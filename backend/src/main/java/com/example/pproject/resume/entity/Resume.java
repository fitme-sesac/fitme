package com.example.pproject.resume.entity;

import com.example.pproject.common.entity.BaseSoftDeleteEntity;
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

    @Column(name = "target_job_id")
    private Long targetJobId;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResumeField field;

    @Lob
    private String summary; //  AI 요약

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private List<Double> embedding;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", nullable = false, length = 20)
    @ColumnDefault("'NONE'")
    private SummaryStatus summaryStatus; //  AI 요약 상태

    @Column(name = "preference_location", length = 80)
    private String preferenceLocation;

    @Column(name = "preference_salary", length = 80)
    private String preferenceSalary;

    @Column(name = "employment_type", length = 80)
    private String employmentType;

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

    // 임베딩 업데이트 메서드
    public void updateEmbedding(List<Double> embeddingVector) {
        this.embedding = embeddingVector;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void requestAiAnalysis(Long targetJobId) {
        this.targetJobId = targetJobId;
        this.summaryStatus = SummaryStatus.PENDING;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void completeAiAnalysis(String summary) {
        this.summary = summary;
        this.summaryStatus = SummaryStatus.COMPLETED;
    }
}