package com.example.pproject.faq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FAQ 목록 조회 요청 (검색, 필터링)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqSearchRequest {

    private String keyword;        // 검색 키워드 (질문/답변)

    private Boolean isPublic;      // 공개 여부로 필터링

    @Builder.Default
    private Integer page = 0;      // 페이지 번호 (0부터 시작)

    @Builder.Default
    private Integer size = 10;     // 한 페이지당 항목 수

    @Builder.Default
    private String sort = "createdAt,desc"; // 정렬 (기본: 최신순)
}
