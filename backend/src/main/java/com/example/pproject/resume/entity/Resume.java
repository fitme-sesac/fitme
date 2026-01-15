package com.example.pproject.resume.entity;

import com.example.pproject.Constant.ResumeField;
import com.example.pproject.Constant.ResumeStatus;
import com.example.pproject.Constant.SummaryStatus;
import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "resume")
public class Resume {

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
    private boolean isPrimary;

    @Column(name = "is_public", nullable = false)
    @ColumnDefault("false")
    private boolean isPublic;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @ColumnDefault("'DRAFT'")
    private ResumeStatus status = ResumeStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ResumeField field; // RESUME, PORTFOLIO, INTRO

    @Column(columnDefinition = "TEXT")
    private String content; // 사용자 원본 자기소개

    // AI 요약 및 임베딩 설계

    @Column(columnDefinition = "TEXT")
    private String summary; // AI가 요약한 10줄 요약본 저장

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", length = 20, nullable = false)
    @ColumnDefault("'NONE'")
    private SummaryStatus summaryStatus = SummaryStatus.NONE; // 요약 진행 상태

    // 요약본(Summary)을 기반으로 생성된 벡터 데이터
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private List<Double> embedding;

    @Column(name = "target_job_id")
    private Long targetJobId;

    @Column(name = "preference_location", length = 80)
    private String preferenceLocation;

    @Column(name = "preference_salary", length = 80)
    private String preferenceSalary;

    @Column(name = "employment_type", length = 80)
    private String employmentType;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeAttachment> attachments = new ArrayList<>();

    /**
     * Create a Resume for the specified user with the given title, content, and field.
     *
     * Sets the resume's lastModifiedAt timestamp to the current time.
     *
     * @param user    the owner of the resume
     * @param title   the resume's title
     * @param content the user's self-introduction or resume content
     * @param field   the resume's categorized field (ResumeField)
     */
    public Resume(UserEntity user, String title, String content, ResumeField field) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.field = field;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Associate a ResumeAttachment with this resume.
     *
     * Adds the given attachment to this resume's attachment list and sets the attachment's
     * resume reference to this instance to maintain bidirectional linkage.
     *
     * @param attachment the ResumeAttachment to associate with this resume
     */
    public void addAttachment(ResumeAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setResume(this);
    }
}