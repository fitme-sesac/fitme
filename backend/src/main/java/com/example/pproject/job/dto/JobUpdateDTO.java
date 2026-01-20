package com.example.pproject.job.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobUpdateDTO {
    
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
}
