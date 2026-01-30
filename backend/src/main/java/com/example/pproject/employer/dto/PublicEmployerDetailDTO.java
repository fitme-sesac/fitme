package com.example.pproject.employer.dto;

import lombok.*;

/**
 * 공개 기업 상세 DTO (기업 상세 페이지용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicEmployerDetailDTO {

    private Long employerId;
    private String name;
    private String logoUrl;
    private String industry;
    private String location;
    private Integer employeeCount;
    private String description;
    /** 현재 채용 중인 공고(OPEN) 개수 */
    private int openJobCount;
}
