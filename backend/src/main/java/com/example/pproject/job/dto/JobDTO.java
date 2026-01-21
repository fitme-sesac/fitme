package com.example.pproject.job.dto;

import lombok.*;

/**
 * 채용공고 조회 DTO (ERD 기준)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDTO {
    
    private Long jobId;
    private String jobUid;
    
    private String title;
    private String description;
    private String summary;  // 자동 생성된 요약
    private String status;
    
    // ERD 기준 필드
    private String location;
    private String salaryText;
    private String stack;
    
    private int viewCount;
    private int applicationCount;
    
    private String createdAt;
    private String updatedAt;
    
    // 기업 정보 (조인)
    private String companyName;
    private String companyLogoUrl;
    
    // 매칭 정보 (로그인한 지원자에게만 표시)
    private JobMatchInfoDTO matchInfo;
}
