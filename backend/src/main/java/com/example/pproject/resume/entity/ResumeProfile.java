package com.example.pproject.resume.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이력서 프로필 엔티티
 * ERD resume_profile 테이블 기준 - created_at/updated_at 컬럼 없음
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "resume_profile")
public class ResumeProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    @Column(length = 200)
    private String address;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Builder
    public ResumeProfile(Resume resume, String address, String photoUrl) {
        this.resume = resume;
        this.address = address;
        this.photoUrl = photoUrl;
    }

    public void setResume(Resume resume) { this.resume = resume; }

    public void updatePhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}