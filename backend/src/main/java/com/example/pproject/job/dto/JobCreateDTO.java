package com.example.pproject.job.dto;

import lombok.*;

/**
 * 채용공고 생성 DTO (ERD 기준)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCreateDTO {

    private String title;
    private String description;
    private String status; // DRAFT, OPEN

    // ERD 기준 필드
    private String location;
    private Long salaryText;  // BIGINT - 연봉 (원 단위)

    // ✅ text[] 대응
    private String[] stack;

    private String requiredQuestions; // JSON 문자열
}
