package com.example.pproject.employer.dto;

import lombok.*;

import java.util.List;

/**
 * 기업 대시보드 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerDashboardDTO {
    
    private EmployerProfileDTO profile;
    
    // 통계
    private long activeJobCount;
    private long totalApplicationCount;
    private long totalViewCount;
    
    // 최근 채용공고
    private List<JobSummaryDTO> recentJobs;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobSummaryDTO {
        private Long jobId;
        private String jobUid; // jobId를 문자열로 (기존 API 호환)
        private String title;
        private String status;
        private int applicationCount;
        private int viewCount;
        private String createdAt;
    }
}
