package com.example.pproject.resume.dto;

import com.example.pproject.Constant.ResumeField;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.user.entity.UserEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ResumeRequestDto {

    private Long userId;
    private String title;
    private String tagline;
    private Boolean isPrimary;
    private Boolean isPublic;
    private ResumeField field;

    private String content;
    private List<String> techStack;

    private Long targetJobId;
    private String preferenceLocation;
    private String preferenceSalary;
    private String employmentType;

    private List<CareerDto> careers;
    private List<ProjectDto> projects;
    private List<CertificateDto> certificates;
    private List<LinkDto> links;
    private List<ProfileDto> profiles;

    private List<AttachmentDto> attachments;

    private String summary;       // AI가 만든 10줄 요약
    private List<Double> embedding; /**
     * Create a Resume entity populated from this DTO and the given user.
     *
     * <p>The resulting Resume includes core fields (title, content, field, tagline),
     * preference fields (target job, location, salary, employment type), and AI-generated
     * data (summary and embedding). Nested collections such as projects, careers,
     * certificates, links, profiles, and attachments are not attached or persisted by this
     * method and must be handled separately.
     *
     * @param user the owner UserEntity to associate with the created Resume
     * @return a Resume entity populated with this DTO's core, preference, and AI fields
     */

    public Resume toEntity(UserEntity user) {
        Resume resume = new Resume(user, this.title, this.content, this.field);

        // 기본 정보 매핑
        resume.setTagline(this.tagline);
        resume.setPrimary(this.isPrimary != null ? this.isPrimary : false);
        resume.setPublic(this.isPublic != null ? this.isPublic : false);

        // 희망 조건 매핑
        resume.setTargetJobId(this.targetJobId);
        resume.setPreferenceLocation(this.preferenceLocation);
        resume.setPreferenceSalary(this.preferenceSalary);
        resume.setEmploymentType(this.employmentType);

        // AI 데이터 매핑
        resume.setSummary(this.summary);
        resume.setEmbedding(this.embedding);

        // 주의: projects, careers 등은 별도 엔티티이므로 Service에서 별도로 저장 로직을 수행해야 함
        return resume;
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class CareerDto {
        private String companyName;   // 회사명
        private String department;    // 부서명
        private String role;          // 직책/역할
        private String description;   // 주요 업무 설명
        private LocalDate startDate;  // 입사일
        private LocalDate endDate;    // 퇴사일 (null이면 재직 중)
        private Boolean isCurrent;    // 재직 중 여부
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class ProjectDto {
        private String title;         // 프로젝트명
        private String description;   // 프로젝트 설명
        private String techStack;     // 사용 기술
        private String role;          // 맡은 역할
        private LocalDate startDate;
        private LocalDate endDate;
        private String projectUrl;    // 프로젝트 데모/깃허브 URL
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class CertificateDto {
        private String name;          // 자격증 명
        private String issuer;        // 발급 기관
        private String score;         // 점수/등급
        private LocalDate acquisitionDate; // 취득일
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class LinkDto {
        private String linkType;
        private String url; // URL 주소
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class ProfileDto {
        private String address;       // 주소
        private String photoUrl;      // 사진 URL
        private String phoneNumber;   // 연락처
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class AttachmentDto {
        private String fileUrl;       // S3 업로드 URL
        private String fileName;      // 원본 파일명
        private Long fileSize;        // 파일 크기
        private String aiDescription; // 파일 내용을 AI가 요약한 텍스트
    }
}