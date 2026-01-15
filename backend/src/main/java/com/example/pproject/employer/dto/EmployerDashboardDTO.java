package com.example.pproject.employer.dto;

import lombok.*;

import java.util.List;

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
        private String jobUid;
        private String title;
        private String status;
        private int applicationCount;
        private int viewCount;
        private String createdAt;
    }
}
