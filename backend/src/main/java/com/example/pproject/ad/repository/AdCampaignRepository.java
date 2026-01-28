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

    // =================================================================================
    // [V1: Pure DB Logic - Legacy]
    // Redis 도입 전, 순수 RDB 쿼리로만 광고를 조회하던 방식입니다.
    // 벤치마킹 비교를 위해 보존합니다.
    // =================================================================================

    // [활성 광고 조회 - V1] CPC 입찰가 높은 순
    @Query("SELECT a FROM AdCampaignEntity a WHERE a.status = 'ACTIVE' " +
            "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
            "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP) " +
            "ORDER BY a.cpcBid DESC")
    Page<AdCampaignEntity> findActiveAdsOrderByCpcDesc(Pageable pageable);

    /**
     * [개인화 광고 매칭 - V1] Native Query
     *
     * <p>
     * 사용자 이력서와 채용공고의 유사도 + 입찰가를 하이브리드 계산하여 정렬합니다.
     * </p>
     * <p>
     * <b>성능 한계:</b> 매 요청마다 DB에서 벡터 거리 계산을 수행하므로 트래픽 증가 시 DB CPU 부하가 큽니다.
     * </p>
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

    // =================================================================================
    // [V2: Redis Filtered Logic]
    // Redis에서 활성 광고 ID 목록(ids)을 먼저 가져온 후, DB에서는 해당 ID들에 대해서만 조회하는 방식입니다.
    // =================================================================================

    // [광고 노출용 - V2] Redis Active Set 기반 조회 (ID 필터링 + CPC 정렬)
    // PostgreSQL ANY() 함수를 사용하여 JDBC 파라미터 제한을 우회하고 성능을 최적화합니다.
    @Query(value = "SELECT * FROM ad_campaign a WHERE a.campaign_id = ANY(:ids) AND a.status = 'ACTIVE' " +
            "AND (a.start_at IS NULL OR a.start_at <= CURRENT_TIMESTAMP) " +
            "AND (a.end_at IS NULL OR a.end_at >= CURRENT_TIMESTAMP) " +
            "ORDER BY a.cpc_bid DESC", nativeQuery = true)
    Page<AdCampaignEntity> findActiveAdsByIdsOrderByCpcDesc(@Param("ids") Long[] ids, Pageable pageable);

    /**
     * [개인화 광고 매칭 - V2 Legacy]
     * 
     * Redis에서 가져온 대량의 ID 목록(ids)을 '필터'로 사용하여 DB에서 유사도를 계산합니다.
     * ID가 매우 많을 경우(10만 개 이상), ANY(:ids) 절에 너무 많은 파라미터가 들어가 성능 저하(Parsing Cost)가
     * 발생합니다.
     * (벤치마크 비교용으로 유지)
     */
    @Query(value = """
            WITH max_bid AS (
                SELECT COALESCE(MAX(ac.cpc_bid), 1) as max_cpc
                FROM ad_campaign ac
                WHERE ac.status = 'ACTIVE'
            ),
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
                WHERE ac.campaign_id = ANY(:ids) -- [Array Filter Optimization]
                    AND ac.status = 'ACTIVE'
                    AND (ac.start_at IS NULL OR ac.start_at <= NOW())
                    AND (ac.end_at IS NULL OR ac.end_at >= NOW())
                    AND jp.embedding IS NOT NULL
            )
            SELECT campaign_id, job_id, employer_id, title, cpc_bid, similarity, hybrid_score
            FROM scored_ads
            WHERE similarity >= :minSimilarity
            ORDER BY hybrid_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findActiveAdsWithSimilarityAndIds(
            @Param("ids") Long[] ids,
            @Param("userEmbedding") String userEmbedding,
            @Param("minSimilarity") double minSimilarity,
            @Param("limit") int limit);

    /**
     * [V3 Hybrid - Recall Phase]
     * Redis 필터링 없이 DB에서 순수 유사도 기반으로 상위 N개를 조회합니다.
     * 이후 Application Layer에서 Redis Guard를 통해 예산을 검증합니다.
     */
    @Query(value = """
            WITH max_bid AS (
                SELECT COALESCE(MAX(ac.cpc_bid), 1) as max_cpc
                FROM ad_campaign ac
                WHERE ac.status = 'ACTIVE'
            ),
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
                WHERE ac.status = 'ACTIVE'
                    AND (ac.start_at IS NULL OR ac.start_at <= NOW())
                    AND (ac.end_at IS NULL OR ac.end_at >= NOW())
                    AND jp.embedding IS NOT NULL
            )
            SELECT campaign_id, job_id, employer_id, title, cpc_bid, similarity, hybrid_score
            FROM scored_ads
            WHERE similarity >= :minSimilarity
            ORDER BY hybrid_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopAdsBySimilarity(
            @Param("userEmbedding") String userEmbedding,
            @Param("minSimilarity") double minSimilarity,
            @Param("limit") int limit);
}
