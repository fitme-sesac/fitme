package com.example.pproject.resume.entity;

import com.example.pproject.Constant.ResumeField;
import com.example.pproject.Constant.ResumeStatus;
import com.example.pproject.Constant.SummaryStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResumeField field;

    @Column(columnDefinition = "TEXT")
    private String content;

    // AI 요약 결과
    @Column(columnDefinition = "TEXT")
    private String summary;

    // [수정] 기술 스택
    @Column(name = "re_stack", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> reStack = new ArrayList<>();

    // [추가] 경력 연차
    @Column(name = "career_years", nullable = false)
    @ColumnDefault("0")
    private Integer careerYears;

    // 학력 정보
    @Column(name = "school")
    private String school;

    @Column(name = "school_state")
    private String schoolState;

    @Column(name = "school_class")
    private String schoolClass;

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private List<Double> embedding;

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

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeCareer> careers = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeProject> projects = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeCertificate> certificates = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeAttachment> attachments = new ArrayList<>();

    @OneToOne(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private ResumeProfile profile;

    @Builder
    public Resume(UserEntity user, String title, ResumeField field, boolean primary, boolean publicOption,
            String tagline, String content, List<String> reStack, Integer careerYears, Long targetJobId,
            String preferenceLocation, String preferenceSalary, String employmentType,
            String school, String schoolState, String schoolClass) {
        this.user = user;
        this.title = title;
        this.field = field;
        this.primary = primary;
        this.publicOption = publicOption;
        this.tagline = tagline;
        this.content = content;
        this.reStack = reStack != null ? reStack : new ArrayList<>();
        this.careerYears = careerYears != null ? careerYears : 0;
        this.targetJobId = targetJobId;
        this.preferenceLocation = preferenceLocation;
        this.preferenceSalary = preferenceSalary;
        this.employmentType = employmentType;
        this.school = school;
        this.schoolState = schoolState;
        this.schoolClass = schoolClass;
        this.status = ResumeStatus.DRAFT;
        this.summaryStatus = SummaryStatus.NONE;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void updateProfile(ResumeProfile profile) {
        this.profile = profile;
        if (profile != null)
            profile.setResume(this);
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void updateInfo(String title, String tagline, String content, Boolean publicOption, ResumeField field,
            String preferenceLocation, String preferenceSalary, String employmentType,
            List<String> reStack, Integer careerYears,
            String school, String schoolState, String schoolClass) {
        if (title != null)
            this.title = title;
        if (tagline != null)
            this.tagline = tagline;
        if (content != null)
            this.content = content;
        if (publicOption != null)
            this.publicOption = publicOption;
        if (field != null)
            this.field = field;
        if (preferenceLocation != null)
            this.preferenceLocation = preferenceLocation;
        if (preferenceSalary != null)
            this.preferenceSalary = preferenceSalary;
        if (employmentType != null)
            this.employmentType = employmentType;

        if (reStack != null)
            this.reStack = reStack; // 리스트 자체를 업데이트
        if (careerYears != null)
            this.careerYears = careerYears;

        if (school != null)
            this.school = school;
        if (schoolState != null)
            this.schoolState = schoolState;
        if (schoolClass != null)
            this.schoolClass = schoolClass;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // AI 분석 결과 업데이트 (Legacy: 기술 스택 포함)
    public void updateAiAnalysis(String summary, List<String> techStack) {
        if (summary != null)
            this.summary = summary;
        if (techStack != null)
            this.reStack = techStack;
        this.summaryStatus = SummaryStatus.COMPLETED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // AI 분석 결과 업데이트 (New: 임베딩 포함)
    public void updateAiAnalysisWithEmbedding(String summary, List<Double> embedding) {
        if (summary != null)
            this.summary = summary;
        if (embedding != null)
            this.embedding = embedding;
        this.summaryStatus = SummaryStatus.COMPLETED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // AI 분석 상태 업데이트
    public void updateSummaryStatus(SummaryStatus status) {
        this.summaryStatus = status;
        this.lastModifiedAt = LocalDateTime.now();
    }
}