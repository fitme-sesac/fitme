package com.example.pproject.resume.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 서버로부터 받는 이력서 분석/첨삭 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResumeResponse {

    @JsonProperty("resume_id")
    private Long resumeId;

    @JsonProperty("status")
    private String status; // "SUCCESS", "FAILED", "PARTIAL"

    @JsonProperty("summary")
    private String summary; // AI가 생성한 이력서 요약

    @JsonProperty("suggestions")
    private List<Suggestion> suggestions; // 첨삭 제안 목록

    @JsonProperty("improved_content")
    private String improvedContent; // 개선된 본문 내용

    @JsonProperty("skill_recommendations")
    private List<String> skillRecommendations; // 추천 기술 스택

    @JsonProperty("match_analysis")
    private MatchAnalysis matchAnalysis; // 채용공고 매칭 분석 결과

    @JsonProperty("error_message")
    private String errorMessage; // 실패 시 에러 메시지

    /**
     * 첨삭 제안 항목
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String section; // "project", "career", "content", "skills"
        private String type; // "add", "modify", "remove", "enhance"
        private String original; // 원본 텍스트
        private String suggested; // 제안 텍스트
        private String reason; // 제안 이유
        private int priority; // 우선순위 (1: 높음, 5: 낮음)
    }

    /**
     * 채용공고 매칭 분석 결과
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchAnalysis {
        @JsonProperty("match_score")
        private Double matchScore; // 매칭 점수 (0-100)

        @JsonProperty("matched_skills")
        private List<String> matchedSkills; // 매칭된 기술 스택

        @JsonProperty("missing_skills")
        private List<String> missingSkills; // 부족한 기술 스택

        @JsonProperty("highlight_points")
        private List<String> highlightPoints; // 강조할 포인트

        @JsonProperty("improvement_areas")
        private List<String> improvementAreas; // 개선이 필요한 부분
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
