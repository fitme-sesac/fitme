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
                .build();
    }
}