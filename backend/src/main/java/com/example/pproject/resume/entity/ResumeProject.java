package com.example.pproject.resume.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "resume_project", indexes = @Index(name = "idx_resume_project_resume_id", columnList = "resume_id"))
public class ResumeProject extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "contribution_pct", precision = 5, scale = 2)
    private BigDecimal contributionPct;

    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    @ColumnDefault("0")
    private Integer sortOrder;

    @Builder
    public ResumeProject(Resume resume, String title, LocalDate startDate, LocalDate endDate,
                         BigDecimal contributionPct, String techStack, String description, Integer sortOrder) {
        this.resume = resume;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.contributionPct = contributionPct;
        this.techStack = techStack;
        this.description = description;
        this.sortOrder = (sortOrder != null) ? sortOrder : 0;
    }

    public void setResume(Resume resume) { this.resume = resume; }
}