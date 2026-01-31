package com.example.pproject.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이력서 기반 채용공고 추천 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRecommendationDTO {

    private Long jobId;
    private Long employerId;
    private String title;
    private String location;
    private String[] stack;
    private Long salaryText;
    private String summary;
    private String status;
    private Double similarity;

    /**
     * Native Query 결과에서 DTO 생성
     * 쿼리 컬럼 순서: job_id, employer_id, title, location, stack (comma string),
     * salary_text, summary,
     * status, similarity
     */
    public static JobRecommendationDTO fromQueryResult(Object[] row) {
        String stackStr = (String) row[4];
        String[] stackArray = (stackStr != null && !stackStr.isEmpty())
                ? stackStr.split(",")
                : new String[0];

        return JobRecommendationDTO.builder()
                .jobId(((Number) row[0]).longValue())
                .employerId(((Number) row[1]).longValue())
                .title((String) row[2])
                .location((String) row[3])
                .stack(stackArray)
                .salaryText(parseSalary(row[5]))
                .summary((String) row[6])
                .status((String) row[7])
                .similarity(parseSimilarity(row[8]))
                .build();
    }

    private static Long parseSalary(Object value) {
        if (value == null)
            return null;
        if (value instanceof Number)
            return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                // 쉼표 등이 섞여있을 수 있으므로 제거 후 파싱 시도
                String strVal = ((String) value).replaceAll("[^0-9]", "");
                return strVal.isEmpty() ? null : Long.parseLong(strVal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Double parseSimilarity(Object value) {
        if (value == null)
            return 0.0;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return 0.0;
    }
}
