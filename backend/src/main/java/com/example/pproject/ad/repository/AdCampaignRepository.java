package com.example.pproject.ad.repository;

import com.example.pproject.ad.entity.AdCampaignEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdCampaignRepository extends JpaRepository<AdCampaignEntity, Long> {

        // 기업별 광고 캠페인 목록
        @Query("SELECT a FROM AdCampaignEntity a WHERE a.employerId = :employerId ORDER BY a.createdAt DESC")
        Page<AdCampaignEntity> findByEmployerIdAndNotDeleted(@Param("employerId") Long employerId, Pageable pageable);

        // 활성 상태인 광고 찾기 (Status='ACTIVE', 기간 내)
        @Query("SELECT a FROM AdCampaignEntity a WHERE a.status = 'ACTIVE' " +
                        "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
                        "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP)")
        Page<AdCampaignEntity> findActiveAds(Pageable pageable);

        // ID로 캠페인 조회
        @Query("SELECT a FROM AdCampaignEntity a WHERE a.id = :id")
        Optional<AdCampaignEntity> findByIdAndNotDeleted(@Param("id") Long id);

        // 특정 job_id로 활성 상태(ENDED가 아닌) 캠페인이 존재하는지 확인
        boolean existsByJobIdAndStatusNot(Long jobId, String status);

        // [광고 노출용] 활성 광고를 CPC 입찰가 높은 순으로 조회
        @Query("SELECT a FROM AdCampaignEntity a WHERE a.status = 'ACTIVE' " +
                        "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
                        "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP) " +
                        "ORDER BY a.cpcBid DESC")
        Page<AdCampaignEntity> findActiveAdsOrderByCpcDesc(Pageable pageable);

        /**
         * [유사도 기반 광고 매칭 - Native Query]
         * 
         * 사용자 이력서와 채용공고 간의 유사도를 계산하여 관련성 높은 광고를 반환합니다.
         * 
         * [쿼리 구조 설명]
         * 
         * 1. WITH max_bid AS (...)
         * → CTE(Common Table Expression): 현재 활성 광고 중 최대 입찰가를 미리 계산
         * → 입찰가 정규화(0~1)에 사용
         * → COALESCE: NULL이면 1로 대체 (0으로 나누기 방지)
         * 
         * 2. scored_ads AS (...)
         * → 각 광고에 대해 유사도와 하이브리드 스코어를 계산
         * 
         * - jp.embedding <=> :userEmbedding
         * → pgvector의 코사인 거리 연산자 (0=동일, 2=정반대)
         * → "1 - 거리"로 유사도(0~1)로 변환
         * 
         * - ac.cpc_bid::float / mb.max_cpc
         * → 입찰가를 0~1 사이로 정규화
         * → ::float 는 PostgreSQL 타입 캐스팅 (정수→실수)
         * 
         * - CROSS JOIN max_bid mb
         * → max_bid CTE의 단일 행을 모든 광고 행에 결합
         * 
         * 3. WHERE similarity >= :minSimilarity
         * → 유사도가 기준(0.5) 미만인 광고는 제외
         * → 사용자와 관련 없는 광고 노출 방지
         * 
         * 4. ORDER BY hybrid_score DESC LIMIT :limit
         * → 하이브리드 스코어 높은 순으로 정렬 후 상위 N개만 반환
         * 
         * @param userEmbedding 사용자 이력서의 embedding 벡터 (문자열 형태: "[0.1, 0.2, ...]")
         * @param minSimilarity 최소 유사도 기준 (예: 0.5)
         * @param limit         가져올 광고 개수
         * @return Object[]: campaign_id, job_id, employer_id, title, cpc_bid,
         *         similarity, hybrid_score
         */
        @Query(value = """
                        -- [CTE 1] 최대 입찰가 계산 (정규화용)
                        WITH max_bid AS (
                            SELECT COALESCE(MAX(ac.cpc_bid), 1) as max_cpc
                            FROM ad_campaign ac
                            WHERE ac.status = 'ACTIVE'
                        ),
                        -- [CTE 2] 각 광고에 유사도/스코어 계산
                        scored_ads AS (
                            SELECT
                                ac.campaign_id,
                                jp.job_id,
                                ac.employer_id,
                                jp.title,
                                ac.cpc_bid,
                                -- 코사인 유사도 계산: 1 - 코사인거리 (0=동일, 1=무관, 2=반대)
                                1 - (jp.embedding <=> CAST(:userEmbedding AS vector)) AS similarity,
                                -- 하이브리드 스코어 = 유사도(70%) + 정규화된 입찰가(30%)
                                (1 - (jp.embedding <=> CAST(:userEmbedding AS vector))) * 0.7
                                    + (ac.cpc_bid::float / mb.max_cpc) * 0.3 AS hybrid_score
                            FROM ad_campaign ac
                            -- 광고 캠페인과 채용공고 연결
                            JOIN job_posting jp ON ac.job_id = jp.job_id
                            -- max_bid CTE 결합 (정규화용)
                            CROSS JOIN max_bid mb
                            WHERE ac.status = 'ACTIVE'
                                AND (ac.start_at IS NULL OR ac.start_at <= NOW())
                                AND (ac.end_at IS NULL OR ac.end_at >= NOW())
                                -- embedding이 없는 채용공고는 제외
                                AND jp.embedding IS NOT NULL
                        )
                        -- [최종] 유사도 필터링 + 스코어 정렬
                        SELECT campaign_id, job_id, employer_id, title, cpc_bid, similarity, hybrid_score
                        FROM scored_ads
                        WHERE similarity >= :minSimilarity
                        ORDER BY hybrid_score DESC
                        LIMIT :limit
                        """, nativeQuery = true)
        List<Object[]> findActiveAdsWithSimilarity(
                        @Param("userEmbedding") String userEmbedding,
                        @Param("minSimilarity") double minSimilarity,
                        @Param("limit") int limit);
}
