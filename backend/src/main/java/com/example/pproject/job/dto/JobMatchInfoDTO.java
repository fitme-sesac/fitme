package com.example.pproject.job.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

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
     * [1] 기술 스택 매칭률 (Weight: 50% or 70%)
     * - 지원자의 기술 보유 여부와 숙련도를 고려하여 산출
     * - 계산: (일치하는 스택 수 / 공고 요구 스택 수) * 100
     */
    private int stackMatchRate;

    /**
     * [2] 벡터 유사도 기반 매칭률 (Weight: 30% or 0%)
     * - AI 임베딩 엔진(Vector Index)을 통한 문맥적 유사도
     * - 계산: 코사인 유사도(Cosine Similarity) * 100
     */
    private int vectorMatchRate;

    /**
     * [3] 경력 매칭률 (Weight: 20% or 30%)
     * - 공고의 요구 경력 연수와 지원자의 총 경력 연수 비교
     * - 계산: (보유 경력 / 요구 경력) * 100 (최대 100)
     */
    private int experienceMatchRate;

    /**
     * 요구 경력 (년)
     */
    private Integer requiredExperience;

    /**
     * 보유 경력 (년)
     */
    private Integer candidateExperience;

    /**
     * 종합 매칭률 (0~100)
     * - 상위 3가지 지표를 가중치에 따라 합산한 최종 점수
     * - 공식 A (벡터 有): (스택*0.5) + (벡터*0.3) + (경력*0.2)
     * - 공식 B (벡터 無): (스택*0.7) + (경력*0.3)
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
     * 각 기술별 상세 매칭 정보 (상세 페이지용)
     * - skillName: 기술명
     * - matched: 보유 여부
     * - proficiency: 숙련도 (1-3, 높을수록 많이 사용)
     * - matchScore: 매칭 점수 (0~100)
     */
    private List<SkillMatchDetail> skillDetails;

    /**
     * 지원자의 전체 기술 스택 (디버깅/참고용)
     */
    private List<String> candidateStacks;

    /**
     * 각 기술별 숙련도 (re_stack + tech_stack 중복 시 높은 값)
     * key: 기술명(소문자), value: 숙련도 (1-3)
     */
    private Map<String, Integer> skillProficiencyMap;

    /**
     * @deprecated 종합 매칭률(overallMatchRate) 사용을 권장합니다.
     */
    @Deprecated
    private int matchRate;

    /**
     * 매칭률 기반 레벨 계산
     */
    public static String calculateMatchLevel(int matchRate) {
        if (matchRate >= 80)
            return "EXCELLENT";
        if (matchRate >= 60)
            return "GOOD";
        if (matchRate >= 40)
            return "MODERATE";
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

    /**
     * 기술별 상세 매칭 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillMatchDetail {
        private String skillName; // 기술명
        private boolean matched; // 보유 여부
        private int proficiency; // 숙련도 (1: 기본, 2: 중급, 3: 고급)
        private int matchScore; // 개별 매칭 점수 (0~100)
        private String proficiencyLabel; // 숙련도 라벨 (초급/중급/고급)

        public static SkillMatchDetail of(String skillName, boolean matched, int proficiency) {
            String label = proficiency >= 3 ? "고급" : proficiency >= 2 ? "중급" : matched ? "초급" : "미보유";
            int score = matched ? Math.min(100, proficiency * 33 + 1) : 0;

            return SkillMatchDetail.builder()
                    .skillName(skillName)
                    .matched(matched)
                    .proficiency(proficiency)
                    .matchScore(score)
                    .proficiencyLabel(label)
                    .build();
        }
    }
}
