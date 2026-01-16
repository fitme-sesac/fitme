package com.example.pproject.resume.dto;

import com.example.pproject.Constant.*;
import com.example.pproject.resume.entity.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ResumeResponse {

    // 기본 정보
    private Long id;
    private String title;
    private String tagline;
    private Boolean primary;
    private Boolean publicOption;
    private ResumeStatus status;
    private LocalDateTime lastModifiedAt;

    // 추가 정보
    private Long targetJobId; // 타겟 공고 ID
    private String content; // 자기소개서 내용
    private ResumeField field; // 구분
    private String summary; // AI 요약 결과
    private SummaryStatus summaryStatus; // AI 상태

    // 희망 조건 및 스택
    private String preferenceLocation;
    private String preferenceSalary;
    private String employmentType;
    private String reStack;

    // 자식 리스트
    private ProfileDto profile;
    private List<CareerDto> careers;
    private List<ProjectDto> projects;
    private List<CertificateDto> certificates;
    private List<LinkDto> links;
    private List<AttachmentDto> attachments;

    public static ResumeResponse from(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .title(resume.getTitle())
                .tagline(resume.getTagline())
                .primary(resume.isPrimary())
                .publicOption(resume.isPublicOption())
                .status(resume.getStatus())
                .lastModifiedAt(resume.getLastModifiedAt())
                .targetJobId(resume.getTargetJobId())
                .content(resume.getContent())
                .field(resume.getField())
                .summary(resume.getSummary())
                .summaryStatus(resume.getSummaryStatus())
                .preferenceLocation(resume.getPreferenceLocation())
                .preferenceSalary(resume.getPreferenceSalary())
                .employmentType(resume.getEmploymentType())
                .reStack(resume.getReStack())

                // null 체크 및 리스트 변환
                .profile(resume.getProfile() != null ? ProfileDto.from(resume.getProfile()) : null)
                .careers(resume.getCareers().stream().map(CareerDto::from).collect(Collectors.toList()))
                .projects(resume.getProjects().stream().map(ProjectDto::from).collect(Collectors.toList()))
                .certificates(resume.getCertificates().stream().map(CertificateDto::from).collect(Collectors.toList()))
                .links(resume.getLinks().stream().map(LinkDto::from).collect(Collectors.toList()))
                .attachments(resume.getAttachments().stream().map(AttachmentDto::from).collect(Collectors.toList()))
                .build();
    }

    @Getter @Builder public static class ProfileDto {
        private String address; private String photoUrl;
        public static ProfileDto from(ResumeProfile p) { return ProfileDto.builder().address(p.getAddress()).photoUrl(p.getPhotoUrl()).build(); }
    }
    @Getter @Builder public static class CareerDto {
        private Long id; private String companyName; private String department; private String role;
        private LocalDate startDate; private LocalDate endDate; private Boolean current; private Boolean verified;
        public static CareerDto from(ResumeCareer c) { return CareerDto.builder().id(c.getId()).companyName(c.getCompanyName())
                .department(c.getDepartment()).role(c.getRole()).startDate(c.getStartDate()).endDate(c.getEndDate())
                .current(c.getCurrent()).verified(c.getVerified()).build(); }
    }
    @Getter @Builder public static class ProjectDto {
        private Long id; private String title; private LocalDate startDate; private LocalDate endDate;
        private BigDecimal contributionPct; private String techStack; private String description; private Integer sortOrder;
        public static ProjectDto from(ResumeProject p) { return ProjectDto.builder().id(p.getId()).title(p.getTitle())
                .startDate(p.getStartDate()).endDate(p.getEndDate()).contributionPct(p.getContributionPct())
                .techStack(p.getTechStack()).description(p.getDescription()).sortOrder(p.getSortOrder()).build(); }
    }
    @Getter @Builder public static class CertificateDto {
        private Long id; private String name; private String issuer; private LocalDate acquisitionDate; private Boolean verified;
        public static CertificateDto from(ResumeCertificate c) { return CertificateDto.builder().id(c.getId()).name(c.getName())
                .issuer(c.getIssuer()).acquisitionDate(c.getAcquisitionDate()).verified(c.getVerified()).build(); }
    }
    @Getter @Builder public static class LinkDto {
        private Long id; private LinkType linkType; private String url;
        public static LinkDto from(ResumeLink l) { return LinkDto.builder().id(l.getId()).linkType(l.getLinkType()).url(l.getUrl()).build(); }
    }
    @Getter @Builder public static class AttachmentDto {
        private Long id; private String fileUrl; private String fileName; private ScanStatus scanStatus; private String aiDescription;
        public static AttachmentDto from(ResumeAttachment a) { return AttachmentDto.builder().id(a.getId()).fileUrl(a.getFileUrl())
                .fileName(a.getFileName()).scanStatus(a.getScanStatus()).aiDescription(a.getAiDescription()).build(); }
    }
}