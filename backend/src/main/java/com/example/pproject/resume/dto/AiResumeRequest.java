package com.example.pproject.resume.dto;

import com.example.pproject.Constant.ResumeField;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.entity.ResumeAttachment;
import com.example.pproject.resume.entity.ResumeCareer;
import com.example.pproject.resume.entity.ResumeProject;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class AiResumeRequest {

    @JsonProperty("resume_id")
    private Long resumeId;

    @JsonProperty("basic_info")
    private BasicInfo basicInfo;

    private String content;

    @JsonProperty("projects")
    private List<ProjectInfo> projects;

    @JsonProperty("careers")
    private List<CareerInfo> careers;

    @JsonProperty("file_links")
    private List<String> fileLinks;

    @JsonProperty("summary_type")
    private String summaryType;

    @JsonProperty("include_reasoning")
    private Boolean includeReasoning;

    // ★ 채용공고 정보 추가 (채용공고에 맞춤 첨삭 시 사용)
    @JsonProperty("target_job")
    private TargetJobInfo targetJob;

    @Getter
    @Builder
    public static class BasicInfo {
        private String title;
        private String tagline;

        @JsonProperty("re_stack")
        private List<String> reStack;

        private ResumeField field;
        private Preference preference;
    }

    @Getter
    @Builder
    public static class Preference {
        private String location;
        private String salary;

        @JsonProperty("employment_type")
        private String employmentType;
    }

    @Getter
    @Builder
    public static class ProjectInfo {
        @JsonProperty("project_name")
        private String projectName;

        @JsonProperty("start_date")
        private String startDate;

        @JsonProperty("end_date")
        private String endDate;

        @JsonProperty("total_tech_stack")
        private List<String> totalTechStack;

        private String contribution; // 엔티티에 텍스트 필드가 없으면 null 혹은 description 일부 매핑
        private String description;
    }

    @Getter
    @Builder
    public static class CareerInfo {
        @JsonProperty("company_name")
        private String companyName;

        private String role;

        @JsonProperty("start_date")
        private String startDate;

        @JsonProperty("end_date")
        private String endDate;

        private String description;
    }

    /**
     * 타겟 채용공고 정보 (AI가 채용공고에 맞춤 첨삭을 위해 사용)
     */
    @Getter
    @Builder
    public static class TargetJobInfo {
        @JsonProperty("job_id")
        private Long jobId;

        private String title;
        private String description;

        @JsonProperty("required_skills")
        private List<String> requiredSkills;

        private String location;

        @JsonProperty("salary_text")
        private String salaryText;

        @JsonProperty("company_name")
        private String companyName;
    }

    /**
     * 이력서만 사용하는 기본 변환 (기존 호환성 유지)
     */
    public static AiResumeRequest from(Resume resume, String summaryType) {
        return from(resume, summaryType, null, null);
    }

    /**
     * 채용공고 정보를 포함한 변환 (맞춤 첨삭용)
     */
    public static AiResumeRequest from(Resume resume, String summaryType, 
                                       Long jobId, String jobTitle, String jobDescription, 
                                       List<String> requiredSkills, String location, 
                                       String salaryText, String companyName) {
        TargetJobInfo targetJob = TargetJobInfo.builder()
                .jobId(jobId)
                .title(jobTitle)
                .description(jobDescription)
                .requiredSkills(requiredSkills)
                .location(location)
                .salaryText(salaryText)
                .companyName(companyName)
                .build();
        return from(resume, summaryType, targetJob, null);
    }

    /**
     * 메인 변환 메서드 (내부용)
     */
    private static AiResumeRequest from(Resume resume, String summaryType, 
                                        TargetJobInfo targetJob, Void unused) {
        // 날짜 포맷터 (YYYY.MM)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM");

        // 1. BasicInfo 생성
        BasicInfo basicInfo = BasicInfo.builder()
                .title(resume.getTitle())
                .tagline(resume.getTagline())
                .reStack(resume.getReStack() != null ? resume.getReStack() : Collections.emptyList())
                .field(resume.getField())
                .preference(Preference.builder()
                        .location(resume.getPreferenceLocation())
                        .salary(resume.getPreferenceSalary())
                        .employmentType(resume.getEmploymentType())
                        .build())
                .build();

        // 2. Project 리스트 변환
        List<ProjectInfo> projectList = resume.getProjects().stream()
                .map(p -> ProjectInfo.builder()
                        .projectName(p.getTitle())
                        .startDate(formatDate(p.getStartDate(), formatter))
                        .endDate(formatDate(p.getEndDate(), formatter))
                        .totalTechStack(parseTechStack(p.getTechStack())) // 콤마 문자열 -> 리스트
                        .description(p.getDescription())
                        // contribution 필드는 Entity에 없으므로, 필요하다면 description을 쓰거나 비워둠
                        .contribution(null)
                        .build())
                .collect(Collectors.toList());

        // 3. Career 리스트 변환
        List<CareerInfo> careerList = resume.getCareers().stream()
                .map(c -> CareerInfo.builder()
                        .companyName(c.getCompanyName())
                        .role(c.getRole())
                        .startDate(formatDate(c.getStartDate(), formatter))
                        .endDate(formatDate(c.getEndDate(), formatter))
                        // description 필드가 ResumeCareer에 없다면 null
                        .description(null)
                        .build())
                .collect(Collectors.toList());

        // 4. 파일 링크
        List<String> fileUrls = resume.getAttachments().stream()
                .map(ResumeAttachment::getFileUrl)
                .collect(Collectors.toList());

        return AiResumeRequest.builder()
                .resumeId(resume.getId())
                .basicInfo(basicInfo)
                .content(resume.getContent())
                .projects(projectList)
                .careers(careerList)
                .fileLinks(fileUrls)
                .summaryType(summaryType)
                .includeReasoning(true)
                .targetJob(targetJob) // 채용공고 정보 포함
                .build();
    }

    // 날짜 포맷팅 헬퍼 메소드
    private static String formatDate(LocalDate date, DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : null;
    }

    private static List<String> parseTechStack(String techStack) {
        if (techStack == null || techStack.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(techStack.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}