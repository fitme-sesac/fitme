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

    // member_id 컬럼과 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 200)
    private String tagline;

    // 기본값 false 설정
    @Column(name = "is_primary", nullable = false)
    @ColumnDefault("false")
    private boolean isPrimary;

    @Column(name = "is_public", nullable = false)
    @ColumnDefault("false")
    private boolean isPublic;

    // Enum: ResumeStatus (DRAFT, ACTIVE 등)
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @ColumnDefault("'DRAFT'")
    private ResumeStatus status = ResumeStatus.DRAFT;

    // Enum: ResumeField (RESUME, PORTFOLIO, INTRO)
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ResumeField field;

    @Column(columnDefinition = "TEXT")
    private String content;

    // --- AI 관련 필드 ---
    @Column(columnDefinition = "TEXT")
    private String summary;

    // Enum: SummaryStatus (NONE, PENDING, COMPLETED...)
    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", length = 20, nullable = false)
    @ColumnDefault("'NONE'")
    private SummaryStatus summaryStatus = SummaryStatus.NONE;

    // DB의 vector(1536) 컬럼 매핑
    // Hibernate 6 이상에서는 @JdbcTypeCode(SqlTypes.VECTOR)를 사용합니다.
    @Column(name = "embedding", columnDefinition = "vector")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private List<Double> embedding;

    // --- 취업 선호 정보 ---
    @Column(name = "target_job_id")
    private Long targetJobId;

    @Column(name = "preference_location", length = 80)
    private String preferenceLocation;

    @Column(name = "preference_salary", length = 80)
    private String preferenceSalary;

    @Column(name = "employment_type", length = 80)
    private String employmentType;

    // --- 날짜 정보 (자동 관리) ---
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

    // 첨부파일 (1:N)
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeAttachment> attachments = new ArrayList<>();

    // --- 생성자 (필수 필드 위주) ---
    public Resume(UserEntity user, String title, String content, ResumeField field) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.field = field;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    // --- 편의 메서드 ---
    public void addAttachment(ResumeAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setResume(this);
    }
}