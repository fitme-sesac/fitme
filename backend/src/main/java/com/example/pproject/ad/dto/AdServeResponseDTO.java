package com.example.pproject.ad.dto;

import com.example.pproject.ad.entity.AdCampaignEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 광고 노출 응답 DTO (통합)
 * 
 * [비로그인 사용자]
 * - similarity, hybridScore, jobTitle = null
 * - 입찰가 순으로 정렬
 * 
 * [로그인 사용자]
 * - similarity, hybridScore, jobTitle = 실제 값
 * - 하이브리드 스코어 순으로 정렬
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdServeResponseDTO {

    private Long campaignId; // 클릭 추적 시 필요
    private Long jobId; // 채용공고 상세 페이지 링크용
    private Long employerId; // 기업 정보 조회용
    private String jobTitle; // 채용공고 제목 (유사도 매칭 시)
    private Integer cpcBid; // 입찰가
    private Double similarity; // 유사도 (0.0 ~ 1.0), 비로그인 시 null
    private Double hybridScore; // 하이브리드 스코어, 비로그인 시 null
    private String clickKey; // 중복 클릭 방지용 고유키

    /**
     * Entity → DTO 변환 (비로그인 사용자용)
     * - similarity, hybridScore는 null
     */
    public static AdServeResponseDTO fromEntity(AdCampaignEntity entity) {
        return AdServeResponseDTO.builder()
                .campaignId(entity.getId())
                .jobId(entity.getJobId())
                .employerId(entity.getEmployerId())
                .jobTitle(null) // 비로그인 시에는 별도 조회 안 함
                .cpcBid(entity.getCpcBid())
                .similarity(null)
                .hybridScore(null)
                .clickKey(generateClickKey(entity.getId()))
                .build();
    }

    /**
     * Native Query 결과 → DTO 변환 (로그인 사용자용)
     * Object[]: campaign_id, job_id, employer_id, title, cpc_bid, similarity,
     * hybrid_score
     */
    public static AdServeResponseDTO fromQueryResult(Object[] row) {
        return AdServeResponseDTO.builder()
                .campaignId(((Number) row[0]).longValue())
                .jobId(((Number) row[1]).longValue())
                .employerId(((Number) row[2]).longValue())
                .jobTitle((String) row[3])
                .cpcBid(((Number) row[4]).intValue())
                .similarity(((Number) row[5]).doubleValue())
                .hybridScore(((Number) row[6]).doubleValue())
                .clickKey(generateClickKey(((Number) row[0]).longValue()))
                .build();
    }

    private static String generateClickKey(Long campaignId) {
        return "c" + campaignId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
