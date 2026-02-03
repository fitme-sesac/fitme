package com.example.pproject.talent.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 인재풀 목록 아이템 DTO
 */
@Data
@Builder
public class TalentListItemDTO {
    private Long id;           // member_id (구직자 PK, 상세 링크용)
    private Long resumeId;     // 이력서 PK (React key 등)
    private String name;
    private String title;      // 이력서 태그라인 또는 직무
    private String summary;
    private String experience; // "3년", "5년" 등
    private String location;
    private String education;
    private List<String> skills;
    private String salary;     // 희망 연봉
    private int matchScore;    // 기본값 또는 추후 매칭 알고리즘
    private String lastUpdated;
    private boolean isNew;     // 최근 7일 이내 수정
}
