package com.example.pproject.resume.dto;

import com.example.pproject.Constant.LinkType;
import com.example.pproject.Constant.ResumeField;
import com.example.pproject.Constant.ScanStatus;
import com.example.pproject.resume.entity.*;
import com.example.pproject.user.entity.UserEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ResumeRequest {

    @NotBlank(message = "이력서 제목은 필수입니다.")
    private String title;

    @NotNull(message = "직무 분야는 필수입니다.")
    private ResumeField field;

    private String tagline;
    private Boolean primary;
    private Boolean publicOption;
    private String content;
    private String reStack;

    private Long targetJobId;
    private String preferenceLocation;
    private String preferenceSalary;
    private String employmentType;

    private String school;
    private String schoolState;
    private String schoolClass;

    @Valid private ProfileDto profile;
    @Valid private List<CareerDto> careers;
    @Valid private List<ProjectDto> projects;
    @Valid private List<CertificateDto> certificates;
    @Valid private List<LinkDto> links;
    @Valid private List<AttachmentDto> attachments;

    private String summary;
    private List<Double> embedding;

    public Resume toEntity(UserEntity user) {
        return Resume.builder()
                .user(user)
                .title(this.title)
                .field(this.field)
                .content(this.content)
                .tagline(this.tagline)
                .primary(this.primary != null ? this.primary : false)
                .publicOption(this.publicOption != null ? this.publicOption : false)
                .reStack(this.reStack)
                .targetJobId(this.targetJobId)
                .preferenceLocation(this.preferenceLocation)
                .preferenceSalary(this.preferenceSalary)
                .employmentType(this.employmentType)
                .school(this.school)
                .schoolState(this.schoolState)
                .schoolClass(this.schoolClass)
                .build();
    }

    @Getter @Setter @NoArgsConstructor
    public static class ProfileDto {
        private String address; private String photoUrl;
        public ResumeProfile toEntity(Resume resume) { return ResumeProfile.builder().resume(resume).address(address).photoUrl(photoUrl).build(); }
    }
    @Getter @Setter @NoArgsConstructor
    public static class CareerDto {
        @NotBlank private String companyName;
        @NotBlank private String department;
        @NotBlank private String role;
        @NotNull private LocalDate startDate;
        private LocalDate endDate; private Boolean current; private Boolean verified;
        public ResumeCareer toEntity(Resume resume) { return ResumeCareer.builder().resume(resume).companyName(companyName).department(department).role(role).startDate(startDate).endDate(endDate).current(current).verified(verified).build(); }
    }
    @Getter @Setter @NoArgsConstructor
    public static class ProjectDto {
        @NotBlank private String title;
        private String description; private String techStack;
        private LocalDate startDate; private LocalDate endDate; private BigDecimal contributionPct; private Integer sortOrder;
        public ResumeProject toEntity(Resume resume) { return ResumeProject.builder().resume(resume).title(title).description(description).techStack(techStack).startDate(startDate).endDate(endDate).contributionPct(contributionPct).sortOrder(sortOrder).build(); }
    }
    @Getter @Setter @NoArgsConstructor
    public static class CertificateDto {
        @NotBlank private String name;
        @NotBlank private String issuer;
        @NotNull private LocalDate acquisitionDate; private Boolean verified;
        public ResumeCertificate toEntity(Resume resume) { return ResumeCertificate.builder().resume(resume).name(name).issuer(issuer).acquisitionDate(acquisitionDate).verified(verified).build(); }
    }
    @Getter @Setter @NoArgsConstructor
    public static class LinkDto {
        @NotNull private LinkType linkType; @NotBlank private String url;
        public ResumeLink toEntity(Resume resume) { return ResumeLink.builder().resume(resume).linkType(linkType).url(url).build(); }
    }
    @Getter @Setter @NoArgsConstructor
    public static class AttachmentDto {
        @NotBlank private String fileUrl; @NotBlank private String fileName;
        private String mimeType; private Long fileSize;
        public ResumeAttachment toEntity(Resume resume) { return ResumeAttachment.builder().resume(resume).fileUrl(fileUrl).fileName(fileName).mimeType(mimeType).fileSize(fileSize).scanStatus(ScanStatus.PENDING).build(); }
    }
}