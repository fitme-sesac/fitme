package com.example.pproject.job.dto;

import lombok.*;

/**
 * 채용공고 수정 DTO (ERD 기준)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobUpdateDTO {
    
    private String title;
    private String description;
    private String status;
    
    // ERD 기준 필드
    private String location;
    private String salaryText;  // VARCHAR(80) - 급여 정보
    private String stack;
    private Integer requiredExperience;  // 요구 경력 (0: 신입/무관)
    private Integer recruitmentCapacity; // 모집 정원
    private String requiredQuestions;
}
