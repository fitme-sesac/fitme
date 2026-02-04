package com.example.pproject.application.dto;

import com.example.pproject.Constant.ApplicationStatus;
import com.example.pproject.application.entity.JobApplication;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JobApplicationResponse {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private Long resumeId;
    private String resumeTitle;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime viewedAt;
    
    // 기업 정보 (프론트엔드 표시용)
    private String companyName;
    private String companyLogo;
    private String location;
    private String salary;

    public static JobApplicationResponse from(JobApplication app) {
        return JobApplicationResponse.builder()
                .applicationId(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .resumeId(app.getResume().getId())
                .resumeTitle(app.getResume().getTitle())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .viewedAt(app.getViewedAt())
                .location(app.getJob().getLocation())
                .salary(app.getJob().getSalaryText())
                .build();
    }
    
    /**
     * 기업 정보 포함 변환
     */
    public static JobApplicationResponse from(JobApplication app, String companyName, String companyLogo) {
        return JobApplicationResponse.builder()
                .applicationId(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .resumeId(app.getResume().getId())
                .resumeTitle(app.getResume().getTitle())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .viewedAt(app.getViewedAt())
                .companyName(companyName)
                .companyLogo(companyLogo)
                .location(app.getJob().getLocation())
                .salary(app.getJob().getSalaryText())
                .build();
    }
}