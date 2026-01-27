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

        // 상태별 캠페인 조회
        List<AdCampaignEntity> findByStatus(String status);

        // 기업별 캠페인 조회 (전체)
        List<AdCampaignEntity> findByEmployerId(Long employerId);

        // 특정 job_id로 활성 상태(ENDED가 아닌) 캠페인이 존재하는지 확인
        boolean existsByJobIdAndStatusNot(Long jobId, String status);

        // [Legacy] Preserved for Load Testing / Benchmarking (V1 vs V2 comparison)
        // [광고 노출용] 활성 광고를 CPC 입찰가 높은 순으로 조회
        @Query("SELECT a FROM AdCampaignEntity a WHERE a.status = 'ACTIVE' " +
                        "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
                        "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP) " +
                        "ORDER BY a.cpcBid DESC")
        Page<AdCampaignEntity> findActiveAdsOrderByCpcDesc(Pageable pageable);

        /**
         * [Legacy] Preserved for Load Testing / Benchmarking (V1 vs V2 comparison)
         * 
         * [유사도 기반 광고 매칭 - Native Query]
         * 
         * 사용자 이력서와 채용공고 간의 유사도를 계산하여 관련성 높은 광고를 반환합니다.
         * 
         * [쿼리 구조 설명]
         * ... (생략) ...
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

        // [New] Redis Active Set 기반 조회 (ID 필터링 + CPC 정렬)
        @Query("SELECT a FROM AdCampaignEntity a WHERE a.id IN :ids AND a.status = 'ACTIVE' " +
                        "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
                        "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP) " +
                        "ORDER BY a.cpcBid DESC")
        Page<AdCampaignEntity> findActiveAdsByIdsOrderByCpcDesc(@Param("ids") List<Long> ids, Pageable pageable);

        /**
         * [Native Query with ID Filter]
         * Redis Active Set에 포함된 광고들 중에서만 유사도 매칭을 수행합니다.
         */
        @Query(value = """
                        -- [CTE 1] 최대 입찰가 계산 (정규화용) - 전체 활성 광고 기준이 아닌, 해당 ID들 기준일 수도 있으나 전체 기준이 더 정확함
                        -- 하지만 성능상 ID 필터링된 범위에서 MAX CPC를 구하는 것이 나을 수 있음.
                        -- 여기서는 '전체 활성 광고' 기준을 유지 (Global Max CPC)
                        WITH max_bid AS (
                            SELECT COALESCE(MAX(ac.cpc_bid), 1) as max_cpc
                            FROM ad_campaign ac
                            WHERE ac.status = 'ACTIVE'
                        ),
                        -- [CTE 2] 각 광고에 유사도/스코어 계산 (ID 필터링 추가)
                        scored_ads AS (
                            SELECT
                                ac.campaign_id,
                                jp.job_id,
                                ac.employer_id,
                                jp.title,
                                ac.cpc_bid,
                                1 - (jp.embedding <=> CAST(:userEmbedding AS vector)) AS similarity,
                                (1 - (jp.embedding <=> CAST(:userEmbedding AS vector))) * 0.7
                                    + (ac.cpc_bid::float / mb.max_cpc) * 0.3 AS hybrid_score
                            FROM ad_campaign ac
                            JOIN job_posting jp ON ac.job_id = jp.job_id
                            CROSS JOIN max_bid mb
                            WHERE ac.id IN (:ids) -- [ID Filter]
                                AND ac.status = 'ACTIVE'
                                AND (ac.start_at IS NULL OR ac.start_at <= NOW())
                                AND (ac.end_at IS NULL OR ac.end_at >= NOW())
                                AND jp.embedding IS NOT NULL
                        )
                        -- [최종] 유사도 필터링 + 스코어 정렬
                        SELECT campaign_id, job_id, employer_id, title, cpc_bid, similarity, hybrid_score
                        FROM scored_ads
                        WHERE similarity >= :minSimilarity
                        ORDER BY hybrid_score DESC
                        LIMIT :limit
                        """, nativeQuery = true)
        List<Object[]> findActiveAdsWithSimilarityAndIds(
                        @Param("ids") List<Long> ids,
                        @Param("userEmbedding") String userEmbedding,
                        @Param("minSimilarity") double minSimilarity,
                        @Param("limit") int limit);
}
