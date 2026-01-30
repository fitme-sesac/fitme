package com.example.pproject.job.dto;

import lombok.*;

import java.util.List;

/**
 * 채용공고 필터 옵션 DTO
 * - 프론트엔드에서 필터 드롭다운에 표시할 옵션들
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobFilterOptionsDTO {
    
    // 사용 가능한 기술 스택 목록
    private List<String> stacks;
    
    // 사용 가능한 지역 목록 (시/도 단위)
    private List<String> locations;
    
    /** 정렬된 포지션 카테고리 목록 (프론트엔드, 백엔드, 풀스택 - 필터/LLM 호출용) */
    private List<String> positions;
    
    // 경력 필터 옵션
    private List<ExperienceOption> experienceOptions;
    
    /**
     * 경력 필터 옵션
     */
    @Getter
    @AllArgsConstructor
    public static class ExperienceOption {
        private String label;   // 표시 라벨 (예: "신입", "1-3년")
        private Integer minExp; // 최소 경력
        private Integer maxExp; // 최대 경력 (null이면 무제한)
        
        public static ExperienceOption of(String label, Integer min, Integer max) {
            return new ExperienceOption(label, min, max);
        }
    }
    
    /**
     * 기본 경력 필터 옵션 생성
     */
    public static List<ExperienceOption> getDefaultExperienceOptions() {
        return List.of(
                ExperienceOption.of("신입/무관", 0, 0),
                ExperienceOption.of("1-3년", 1, 3),
                ExperienceOption.of("3-5년", 3, 5),
                ExperienceOption.of("5-10년", 5, 10),
                ExperienceOption.of("10년 이상", 10, null)
        );
    }
}
