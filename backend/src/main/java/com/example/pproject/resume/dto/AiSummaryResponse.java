package com.example.pproject.resume.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Python AI Worker의 /resumes/generate-summary 응답 DTO
 * Python SummaryResponse와 1:1 매핑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryResponse {

    /**
     * 화면 표시용 요약 (to_formatted_string)
     */
    private String summary;

    /**
     * 1536차원 임베딩 벡터
     */
    private List<Double> embedding;

    /**
     * 임베딩에 사용된 텍스트 (디버깅용)
     */
    @JsonProperty("embedding_text")
    private String embeddingText;

    /**
     * 평가 메타데이터 (score, similarity, history)
     */
    @JsonProperty("eval_info")
    private Map<String, Object> evalInfo;

    /**
     * 응답 성공 여부 확인
     */
    public boolean isSuccess() {
        return summary != null && embedding != null && !embedding.isEmpty();
    }
}
