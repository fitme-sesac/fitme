package com.example.pproject.application.dto;

import com.example.pproject.Constant.InterviewMethod;
import com.example.pproject.Constant.InterviewStage;
import com.example.pproject.Constant.InterviewStatus;
import com.example.pproject.application.entity.InterviewSchedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 면접 일정 응답 DTO
 */
@Getter
@Builder
public class InterviewDTO {

    private Long interviewId;
    private Long applicationId;
    
    // 면접 정보
    private InterviewStage stage;
    private InterviewMethod method;
    private String location;
    private String meetingUrl;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private InterviewStatus status;
    
    // 지원 정보
    private Long jobId;
    private String jobTitle;
    private String companyName;
    
    // 지원자 정보
    private Long candidateId;
    private String candidateName;
    private String resumeTitle;
    
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static InterviewDTO from(InterviewSchedule interview) {
        var application = interview.getApplication();
        var job = application.getJob();
        var member = application.getMember();
        var resume = application.getResume();

        return InterviewDTO.builder()
                .interviewId(interview.getId())
                .applicationId(application.getId())
                .stage(interview.getStage())
                .method(interview.getMethod())
                .location(interview.getLocation())
                .meetingUrl(interview.getMeetingUrl())
                .startAt(interview.getStartAt())
                .endAt(interview.getEndAt())
                .status(interview.getStatus())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .companyName(null)  // employer 조인 필요시 별도 처리
                .candidateId(member.getId().longValue())
                .candidateName(member.getUsername())
                .resumeTitle(resume.getTitle())
                .createdAt(interview.getCreatedAt() != null 
                        ? LocalDateTime.ofInstant(interview.getCreatedAt(), ZoneId.systemDefault()) 
                        : null)
                .build();
    }

    /**
     * Entity -> DTO 변환 (기업명 포함)
     */
    public static InterviewDTO from(InterviewSchedule interview, String companyName) {
        InterviewDTO dto = from(interview);
        return InterviewDTO.builder()
                .interviewId(dto.getInterviewId())
                .applicationId(dto.getApplicationId())
                .stage(dto.getStage())
                .method(dto.getMethod())
                .location(dto.getLocation())
                .meetingUrl(dto.getMeetingUrl())
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .status(dto.getStatus())
                .jobId(dto.getJobId())
                .jobTitle(dto.getJobTitle())
                .companyName(companyName)
                .candidateId(dto.getCandidateId())
                .candidateName(dto.getCandidateName())
                .resumeTitle(dto.getResumeTitle())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}
