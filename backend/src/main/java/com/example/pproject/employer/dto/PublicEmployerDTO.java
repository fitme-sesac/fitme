package com.example.pproject.employer.dto;

import lombok.*;

/**
 * 공개 기업 정보 DTO (비로그인 조회용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicEmployerDTO {
    
    private Long employerId;
    private String name;
    private String logoUrl;
    private String industry;
    private String location;
    private Integer employeeCount;
    private String description;
    
    // 현재 채용 중인 공고 수
    private int openJobCount;
}
