package com.example.pproject.resume.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "resume_project", indexes = @Index(name = "idx_resume_project_resume_id", columnList = "resume_id"))
public class ResumeProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * Creates a ResumeProject initialized with the provided resume, title, date range, description, and technology stack.
     *
     * @param resume     the owning Resume entity; must be non-null when persisted
     * @param title      the project title
     * @param startDate  the project start date
     * @param endDate    the project end date (may be null if ongoing)
     * @param description detailed description of the project
     * @param techStack  technologies used in the project
     */
    public ResumeProject(Resume resume, String title, LocalDate startDate, LocalDate endDate, String description, String techStack) {
        this.resume = resume;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.techStack = techStack;
    }
}