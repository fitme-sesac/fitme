package com.example.pproject.employer.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerProfileDTO {
    
    private String employerUid;
    private String companyName;
    private String businessRegistrationNumber;
    private String representativeName;
    private String companyAddress;
    private String companyPhone;
    private String companyWebsite;
    private String industry;
    private Integer employeeCount;
    private String companyDescription;
    private String logoUrl;
    private String status;
}
