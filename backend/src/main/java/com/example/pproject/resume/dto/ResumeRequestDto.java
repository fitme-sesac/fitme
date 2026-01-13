package com.example.pproject.resume.dto;

import com.example.pproject.Constant.LinkType;
import com.example.pproject.Constant.ResumeField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ResumeRequestDto {

    private String title;
    private String tagline;
    private ResumeField field;
    private boolean primary;
    private boolean publicOption;

    private Long targetJobId;
    private String content;

    private String preferenceLocation;
    private String preferenceSalary;
    private String employmentType;


    private ProfileDto profile;
    private List<CareerDto> careers;
    private List<ProjectDto> projects;
    private List<CertificateDto> certificates;
    private List<LinkDto> links;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProfileDto {
        private String address;
        private String photoUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CareerDto {
        private String companyName;
        private String department;
        private String role;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProjectDto {
        private String title;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
        private String techStack;
        private Integer sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CertificateDto {
        private String name;
        private String issuer;
        private LocalDate acquisitionDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LinkDto {
        private LinkType linkType;
        private String url;
    }
}