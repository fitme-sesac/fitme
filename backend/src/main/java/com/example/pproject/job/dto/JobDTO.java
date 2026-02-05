package com.example.pproject.job.dto;

import lombok.*;
import java.util.List;

/**
 * 채용공고 조회 DTO (ERD 기준)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDTO {

    private Long jobId;
    private String jobUid;

    private String title;
    private String description;
    private String summary;  // 자동 생성된 요약
    private String status;

    // ERD 기준 필드
    private String location;
    private String salaryText;    // VARCHAR(80) - 급여 정보
    private String salaryDisplay; // 프론트 표시용 포맷된 문자열
    private String stack;  // job_posting.stack (기술 스택)
    private String position;  // stack 기반 도출: "프론트엔드" | "백엔드" | "풀스택"
    private Integer requiredExperience;  // 요구 경력 (0: 신입/무관)
    private Integer recruitmentCapacity; // 모집 정원

    private int viewCount;
    private int applicationCount;

    private String createdAt;
    private String updatedAt;

    // 기업 정보 (조인)
    private String companyName;
    private String companyLogoUrl;

    // 채용공고 이미지 URL 목록
    private List<String> images;

    // 매칭 정보 (로그인한 지원자에게만 표시)
    private JobMatchInfoDTO matchInfo;
}
