package com.example.pproject.resume.dto;

import com.example.pproject.Constant.ResumeField;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.entity.ResumeAttachment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

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

    @JsonProperty("file_links")
    private List<String> fileLinks;

    @JsonProperty("projects")
    private List<String> projects; // 프로젝트 설명 등 텍스트 리스트로 변환 가정

    @JsonProperty("careers")
    private List<String> careers; // 경력 설명 등 텍스트 리스트로 변환 가정

    @JsonProperty("summary_type")
    private String summaryType;

    @Getter
    @Builder
    public static class BasicInfo {
        private String title;
        private String tagline;

        @JsonProperty("re_stack")
        private List<String> reStack; // List로 변환

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

    public static AiResumeRequest from(Resume resume, String summaryType) {
        // 1. 기술 스택 String -> List 변환
        List<String> stackList = (resume.getReStack() != null && !resume.getReStack().isEmpty())
                ? Arrays.stream(resume.getReStack().split(",")) // 콤마로 구분되어 있다고 가정
                .map(String::trim)
                .collect(Collectors.toList())
                : Collections.emptyList();

        // 2. 파일 링크 추출
        List<String> fileUrls = resume.getAttachments().stream()
                .map(ResumeAttachment::getFileUrl)
                .collect(Collectors.toList());

        // 3. Preference 객체 생성
        Preference preference = Preference.builder()
                .location(resume.getPreferenceLocation())
                .salary(resume.getPreferenceSalary())
                .employmentType(resume.getEmploymentType())
                .build();

        // 4. BasicInfo 객체 생성
        BasicInfo basicInfo = BasicInfo.builder()
                .title(resume.getTitle())
                .tagline(resume.getTagline())
                .reStack(stackList)
                .field(resume.getField())
                .preference(preference)
                .build();

        // 5. 프로젝트/경력 내용을 문자열 리스트로 변환
        List<String> projectList = resume.getProjects().stream()
                .map(p -> String.format("%s (%s): %s", p.getTitle(), p.getTechStack(), p.getDescription()))
                .collect(Collectors.toList());

        List<String> careerList = resume.getCareers().stream()
                .map(c -> String.format("%s (%s - %s)", c.getCompanyName(), c.getDepartment(), c.getRole()))
                .collect(Collectors.toList());

        return AiResumeRequest.builder()
                .resumeId(resume.getId())
                .basicInfo(basicInfo)
                .content(resume.getContent())
                .fileLinks(fileUrls)
                .projects(projectList)
                .careers(careerList)
                .summaryType(summaryType)
                .build();
    }
}