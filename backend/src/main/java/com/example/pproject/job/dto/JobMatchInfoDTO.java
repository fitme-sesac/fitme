package com.example.pproject.job.dto;

import lombok.*;

import java.util.List;

/**
 * 채용공고와 지원자 이력서 간 기술 스택 매칭 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatchInfoDTO {
    
    /**
     * 기술 스택 매칭률 (0~100)
     * 계산: (일치하는 스택 수 / 공고 요구 스택 수) * 100
     */
    private int stackMatchRate;
    
    /**
     * 벡터 유사도 기반 매칭률 (0~100)
     * 계산: 코사인 유사도 * 100
     */
    private int vectorMatchRate;
    
    /**
     * 종합 매칭률 (0~100)
     * 계산: (기술 스택 매칭률 * 0.4) + (벡터 유사도 매칭률 * 0.6)
     */
    private int overallMatchRate;
    
    /**
     * 채용공고에서 요구하는 기술 스택 목록
     */
    private List<String> requiredStacks;
    
    /**
     * 지원자가 보유한 기술 스택 중 공고 요구 스택과 일치하는 것들
     */
    private List<String> matchedStacks;
    
    /**
     * 지원자에게 부족한 기술 스택 (공고 요구 - 지원자 보유)
     */
    private List<String> missingStacks;
    
    /**
     * 매칭 레벨 (UI 표시용)
     * - EXCELLENT: 80% 이상
     * - GOOD: 60% 이상
     * - MODERATE: 40% 이상
     * - LOW: 40% 미만
     */
    private String matchLevel;
    
    /**
     * @deprecated 종합 매칭률(overallMatchRate) 사용을 권장합니다.
     */
    @Deprecated
    private int matchRate;
    
    /**
     * 매칭률 기반 레벨 계산
     */
    public static String calculateMatchLevel(int matchRate) {
        if (matchRate >= 80) return "EXCELLENT";
        if (matchRate >= 60) return "GOOD";
        if (matchRate >= 40) return "MODERATE";
        return "LOW";
    }
    
    /**
     * 종합 매칭률 설정 시 자동으로 레벨과 기존 matchRate도 업데이트
     */
    public void setOverallMatchRate(int overallMatchRate) {
        this.overallMatchRate = overallMatchRate;
        this.matchRate = overallMatchRate; // 하위 호환성
        this.matchLevel = calculateMatchLevel(overallMatchRate);
    }
}
