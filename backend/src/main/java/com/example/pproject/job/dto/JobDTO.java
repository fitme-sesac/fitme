package com.example.pproject.job.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDTO {
    
    private String jobUid;
    private String title;
    private String description;
    private String requirements;
    private String preferredQualifications;
    
    private String jobType;
    private String experienceLevel;
    private String educationLevel;
    
    private Integer salaryMin;
    private Integer salaryMax;
    private Boolean salaryNegotiable;
    
    private String workLocation;
    private Boolean remoteWorkAvailable;
    
    private String postingStartDate;
    private String postingEndDate;
    private Boolean isAlwaysRecruiting;
    
    private String status;
    private Integer viewCount;
    private Integer applicationCount;
    
    private String createdAt;
    private String updatedAt;
    
    // 기업 정보 (조회 시 포함)
    private String companyName;
    private String companyLogoUrl;
}
