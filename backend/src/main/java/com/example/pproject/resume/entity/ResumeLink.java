package com.example.pproject.resume.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.Constant.LinkType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "resume_link", indexes = @Index(name = "idx_resume_link_resume_id", columnList = "resume_id"))
public class ResumeLink extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private LinkType linkType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    public void setResume(Resume resume) { this.resume = resume; }
}