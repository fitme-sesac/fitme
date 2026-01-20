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
    private String salaryText;
    private String stack;
    private String requiredQuestions;
}
