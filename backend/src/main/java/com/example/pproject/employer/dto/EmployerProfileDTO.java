package com.example.pproject.employer.dto;

import lombok.*;

/**
 * 기업 프로필 DTO (ERD 기준)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerProfileDTO {
    
    private Long employerId;
    private String employerUid;
    
    // 기본 정보
    private String name;
    private String logoUrl;
    private String industry;
    private Integer foundedYear;
    private Integer employeeCount;
    private String location;
    
    // 상세 정보
    private String description;
    private String culture;
    private String benefits;
    private String techStack;
    
    // 연락처
    private String contactEmail;
    private String contactPhone;
    private String websiteUrl;
    
    // 상태
    private String status;
    
    // 소속 정보 (employer_member에서)
    private String roleInCompany;
}
