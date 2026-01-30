package com.example.pproject.employer.dto;

import lombok.*;

/**
 * 공개 기업 목록용 DTO (채용 중인 기업 카드)
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
    /** 현재 채용 중인 공고(OPEN) 개수 */
    private int openJobCount;
}
