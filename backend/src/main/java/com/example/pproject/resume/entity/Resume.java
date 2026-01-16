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
    private String content; // PDF 추출 텍스트

    // 종합 요약 + 첨삭 조언 (구조화된 긴 텍스트)
    @Column(columnDefinition = "TEXT")
    private String summary;

    // 희망 기술 스택
    @Column(name = "re_stack", columnDefinition = "TEXT")
    private String reStack;

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
                  String tagline, String content, String reStack, Long targetJobId,
                  String preferenceLocation, String preferenceSalary, String employmentType) {
        this.user = user;
        this.title = title;
        this.field = field;
        this.primary = primary;
        this.publicOption = publicOption;
        this.tagline = tagline;
        this.content = content;
        this.reStack = reStack;
        this.targetJobId = targetJobId;
        this.preferenceLocation = preferenceLocation;
        this.preferenceSalary = preferenceSalary;
        this.employmentType = employmentType;

        this.status = ResumeStatus.DRAFT;
        this.summaryStatus = SummaryStatus.NONE;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void updateProfile(ResumeProfile profile) {
        this.profile = profile;
        if (profile != null) profile.setResume(this);
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void updateInfo(String title, String tagline, String content, Boolean publicOption, ResumeField field,
                           String preferenceLocation, String preferenceSalary, String employmentType, String reStack) {
        if (title != null) this.title = title;
        if (tagline != null) this.tagline = tagline;
        if (content != null) this.content = content;
        if (publicOption != null) this.publicOption = publicOption;
        if (field != null) this.field = field;
        if (preferenceLocation != null) this.preferenceLocation = preferenceLocation;
        if (preferenceSalary != null) this.preferenceSalary = preferenceSalary;
        if (employmentType != null) this.employmentType = employmentType;
        if (reStack != null) this.reStack = reStack;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // AI 분석 결과 반영
    public void updateAiAnalysis(String summary, String techStack) {
        if (summary != null) this.summary = summary;
        if (techStack != null) this.reStack = techStack;
        this.summaryStatus = SummaryStatus.COMPLETED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * AI에게 제공할 이력서 원문 데이터 생성
     */
    public String makePromptText(String jobDescriptionTarget) {
        StringBuilder sb = new StringBuilder();

        sb.append("[지원자 기본 정보]\n");
        sb.append("이력서 제목: ").append(this.title).append("\n");
        sb.append("희망 직무: ").append(this.field).append("\n");
        sb.append("한줄 소개(Tagline): ").append(this.tagline != null ? this.tagline : "없음").append("\n");

        sb.append("\n[기술 스택 현황]\n");
        sb.append("본인 희망/주력 기술: ").append(this.reStack != null ? this.reStack : "작성 안 함").append("\n");

        sb.append("\n[경력 및 경험 상세]\n");
        if (this.careers != null && !this.careers.isEmpty()) {
            sb.append("[경력]\n");
            for (ResumeCareer c : this.careers) {
                sb.append(String.format("- %s (%s, %s): %s\n", c.getCompanyName(), c.getDepartment(), c.getRole(), c.getStartDate()));
            }
        }

        if (this.projects != null && !this.projects.isEmpty()) {
            sb.append("\n[프로젝트 경험]\n");
            for (ResumeProject p : this.projects) {
                sb.append(String.format("- %s (기술: %s): %s (기여도: %s%%)\n",
                        p.getTitle(), p.getTechStack(), p.getDescription(), p.getContributionPct()));
            }
        }

        if (this.certificates != null && !this.certificates.isEmpty()) {
            sb.append("\n[자격증]\n");
            for (ResumeCertificate c : this.certificates) {
                sb.append(String.format("- %s (%s)\n", c.getName(), c.getIssuer()));
            }
        }

        sb.append("\n[이력서 본문 (PDF 내용)]\n");
        sb.append(this.content != null ? this.content : "내용 없음").append("\n");

        sb.append("\n[채용 매칭 조건]\n");
        sb.append("희망 지역: ").append(this.preferenceLocation).append("\n");
        sb.append("희망 연봉: ").append(this.preferenceSalary).append("\n");
        sb.append("고용 형태: ").append(this.employmentType).append("\n");

        sb.append("\n[타겟 공고 내용 (분석 기준)]\n");
        if (jobDescriptionTarget != null && !jobDescriptionTarget.isBlank()) {
            sb.append(jobDescriptionTarget).append("\n");
        } else {
            sb.append("특정 공고 없음. (해당 직무의 일반적인 채용 트렌드를 기준으로 분석할 것)\n");
        }

        return sb.toString();
    }
}