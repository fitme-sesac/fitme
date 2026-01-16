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
    private String preferenceLocation;
    private String preferenceSalary;
    private String employmentType;
    private String reStack;
    private Long targetJobId;

    @Valid
    private ProfileDto profile;

    @Valid
    private List<CareerDto> careers;

    @Valid
    private List<ProjectDto> projects;

    @Valid
    private List<CertificateDto> certificates;

    @Valid
    private List<LinkDto> links;

    @Valid
    private List<AttachmentDto> attachments;

    public Resume toEntity(UserEntity user) {
        return Resume.builder()
                .user(user)
                .title(this.title)
                .field(this.field)
                .primary(this.primary != null ? this.primary : false)
                .publicOption(this.publicOption != null ? this.publicOption : false)
                .tagline(this.tagline)
                .content(this.content)
                .preferenceLocation(this.preferenceLocation)
                .preferenceSalary(this.preferenceSalary)
                .employmentType(this.employmentType)
                .reStack(this.reStack)
                .targetJobId(this.targetJobId)
                .build();
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class ProfileDto {

        private String address;
        private String photoUrl;

        public ResumeProfile toEntity(Resume resume) {
            return ResumeProfile.builder().resume(resume).address(this.address).photoUrl(this.photoUrl).build();
        }
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class CareerDto {

        @NotBlank(message = "회사명은 필수입니다.")
        private String companyName;

        @NotBlank(message = "부서명은 필수입니다.")
        private String department;

        @NotBlank(message = "직무/역할은 필수입니다.")
        private String role;

        @NotNull(message = "근무 시작일은 필수입니다.")
        private LocalDate startDate;

        private LocalDate endDate;
        private Boolean current;
        private Boolean verified;

        public ResumeCareer toEntity(Resume resume) {
            return ResumeCareer.builder().resume(resume).companyName(companyName).department(department).role(role)
                    .startDate(startDate).endDate(endDate).current(current).verified(verified).build();
        }
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class ProjectDto {

        @NotBlank(message = "프로젝트명은 필수입니다.")
        private String title;

        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal contributionPct;
        private String techStack;
        private String description;
        private Integer sortOrder;

        public ResumeProject toEntity(Resume resume) {
            return ResumeProject.builder().resume(resume).title(title).startDate(startDate).endDate(endDate)
                    .contributionPct(contributionPct).techStack(techStack).description(description).sortOrder(sortOrder).build();
        }
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class CertificateDto {

        @NotBlank(message = "자격증명은 필수입니다.")
        private String name;

        @NotBlank(message = "발행기관은 필수입니다.")
        private String issuer;

        @NotNull(message = "취득일은 필수입니다.")
        private LocalDate acquisitionDate;

        private Boolean verified;

        public ResumeCertificate toEntity(Resume resume) {
            return ResumeCertificate.builder().resume(resume).name(name).issuer(issuer)
                    .acquisitionDate(acquisitionDate).verified(verified).build();
        }
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class LinkDto {
        @NotNull(message = "링크 타입은 필수입니다.")
        private LinkType linkType;

        @NotBlank(message = "URL은 필수입니다.")
        private String url;

        public ResumeLink toEntity(Resume resume) {
            return ResumeLink.builder().resume(resume).linkType(linkType).url(url).build();
        }
    }

    @Getter @Setter @NoArgsConstructor @ToString
    public static class AttachmentDto {
        @NotBlank(message = "파일 경로는 필수입니다.")
        private String fileUrl;

        @NotBlank(message = "파일명은 필수입니다.")
        private String fileName;

        private String mimeType;
        private Long fileSize;

        public ResumeAttachment toEntity(Resume resume) {
            return ResumeAttachment.builder()
                    .resume(resume)
                    .fileUrl(fileUrl)
                    .fileName(fileName)
                    .mimeType(mimeType)
                    .fileSize(fileSize)
                    .scanStatus(ScanStatus.PENDING) // 기본값 설정
                    .build();
        }
    }
}