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
    @Query("SELECT a FROM AdCampaignEntity a WHERE a.employerId = :employerId AND a.status != 'DELETED' ORDER BY a.createdAt DESC")
    Page<AdCampaignEntity> findByEmployerIdAndNotDeleted(@Param("employerId") Long employerId, Pageable pageable);

    // 활성 상태인 광고 찾기 (Status='ACTIVE', 기간 내)
    @Query("SELECT a FROM AdCampaignEntity a WHERE a.status = 'ACTIVE' " +
            "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
            "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP)")
    Page<AdCampaignEntity> findActiveAds(Pageable pageable);

    // ID로 캠페인 조회
    @Query("SELECT a FROM AdCampaignEntity a WHERE a.id = :id AND a.status != 'DELETED'")
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
     * <b>성능 최적화 (Recall CTE):</b> 전체 임베딩을 스캔하지 않고, 인덱스를 활용해
     * 유사도가 높은 상위 200개(Recall)만 먼저 추려낸 뒤, 정밀 스코어링을 수행합니다.
     * </p>
     */
    @Query(value = """
            WITH user_vec AS (
                SELECT CAST(:userEmbedding AS vector) as u_vec
            ),
            recall AS (
                -- 1단계: 유사도 상위 200개만 빠르게 추출 (Index 활용)
                SELECT
                    jp.job_id,
                    jp.embedding,
                    jp.title,
                    (jp.embedding <=> uv.u_vec) AS dist
                FROM job_posting jp
                CROSS JOIN user_vec uv
                WHERE jp.embedding IS NOT NULL
                ORDER BY jp.embedding <=> uv.u_vec
                LIMIT 200
            )
            -- 2단계: 추출된 후보군에 대해 CPC 고려하여 하이브리드 점수 계산
            SELECT
                ac.campaign_id,
                r.job_id,
                ac.employer_id,
                r.title,
                ac.cpc_bid,
                1 - r.dist AS similarity,
                (1 - r.dist) * 0.7 + (ac.cpc_bid::float / :maxCpc) * 0.3 AS hybrid_score
            FROM recall r
            JOIN ad_campaign ac ON ac.job_id = r.job_id
            WHERE ac.status = 'ACTIVE'
                AND (ac.start_at IS NULL OR ac.start_at <= NOW())
                AND (ac.end_at IS NULL OR ac.end_at >= NOW())
                AND r.dist <= :maxDistance
            ORDER BY r.dist ASC, hybrid_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findActiveAdsWithSimilarity(
            @Param("userEmbedding") String userEmbedding,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit,
            @Param("maxCpc") double maxCpc);

    // =================================================================================
    // [V2: Redis Filtered Logic]
    // Redis에서 활성 광고 ID 목록(ids)을 먼저 가져온 후, DB에서는 해당 ID들에 대해서만 조회하는 방식입니다.
    // =================================================================================

    // [광고 노출용 - V2] Redis Active Set 기반 조회 (ID 필터링 + CPC 정렬)
    @Query(value = "SELECT * FROM ad_campaign a WHERE a.campaign_id = ANY(:ids) AND a.status = 'ACTIVE' " +
            "ORDER BY a.cpc_bid DESC", nativeQuery = true)
    Page<AdCampaignEntity> findActiveAdsByIdsOrderByCpcDesc(@Param("ids") Long[] ids, Pageable pageable);

    @Query(value = "SELECT * FROM ad_campaign a WHERE a.campaign_id = ANY(:ids) AND a.status = 'ACTIVE' " +
            "ORDER BY a.cpc_bid DESC", nativeQuery = true)
    List<AdCampaignEntity> findActiveAdsByIdsOrderByCpcDescList(@Param("ids") Long[] ids);

    /**
     * [개인화 광고 매칭 - V2 Legacy]
     *
     * Redis에서 가져온 대량의 ID 목록(ids)을 '필터'로 사용하여 DB에서 유사도를 계산합니다.
     * ID가 매우 많을 경우(10만 개 이상), ANY(:ids) 절에 너무 많은 파라미터가 들어가 성능 저하가 발생할 수 있습니다.
     */
    @Query(value = """
            SELECT
                ac.campaign_id,
                jp.job_id,
                ac.employer_id,
                jp.title,
                ac.cpc_bid,
                1 - (jp.embedding <=> CAST(:userEmbedding AS vector)) AS similarity,
                (1 - (jp.embedding <=> CAST(:userEmbedding AS vector))) * 0.7 + (ac.cpc_bid::float / :maxCpc) * 0.3 AS hybrid_score
            FROM ad_campaign ac
            JOIN job_posting jp ON ac.job_id = jp.job_id
            WHERE ac.campaign_id = ANY(:ids)
                AND ac.status = 'ACTIVE'
                AND jp.embedding IS NOT NULL
                AND (jp.embedding <=> CAST(:userEmbedding AS vector)) <= :maxDistance
            ORDER BY jp.embedding <=> CAST(:userEmbedding AS vector) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findActiveAdsWithSimilarityAndIds(
            @Param("ids") Long[] ids,
            @Param("userEmbedding") String userEmbedding,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit,
            @Param("maxCpc") double maxCpc);

    // =================================================================================
    // [V3: Hybrid Architecture - Current Best Practice]
    // DB에서 상위 유사도 후보군(Recall)을 빠르게 가져온 후, Redis(Lua Script)로 예산 필터링을 수행합니다.
    // 즉, V1과 유사한 '전체 범위 검색'이지만, Recall 단계를 최적화하여 사용합니다.
    // =================================================================================

    /**
     * [V3 Hybrid - Recall Phase]
     * Redis 필터링 없이 DB에서 순수 유사도 기반으로 상위 N개를 조회합니다. (Recall CTE 적용)
     * 이후 Application Layer에서 Redis Guard를 통해 예산을 검증합니다.
     */
    @Query(value = """
            WITH user_vec AS (
                SELECT CAST(:userEmbedding AS vector) as u_vec
            ),
            recall AS (
                -- Index Scan을 유도하여 빠르게 상위 유사도 후보를 추출
                SELECT
                    jp.job_id,
                    jp.embedding,
                    jp.title,
                    (jp.embedding <=> uv.u_vec) AS dist
                FROM job_posting jp
                CROSS JOIN user_vec uv
                WHERE jp.embedding IS NOT NULL
                ORDER BY jp.embedding <=> uv.u_vec
                LIMIT 200
            )
            SELECT
                ac.campaign_id,
                r.job_id,
                ac.employer_id,
                r.title,
                ac.cpc_bid,
                1 - r.dist AS similarity,
                (1 - r.dist) * 0.7 + (ac.cpc_bid::float / :maxCpc) * 0.3 AS hybrid_score
            FROM recall r
            JOIN ad_campaign ac ON ac.job_id = r.job_id
            WHERE ac.status = 'ACTIVE'
                AND (ac.start_at IS NULL OR ac.start_at <= NOW())
                AND (ac.end_at IS NULL OR ac.end_at >= NOW())
                AND r.dist <= :maxDistance
            ORDER BY r.dist ASC, hybrid_score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopAdsBySimilarity(
            @Param("userEmbedding") String userEmbedding,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit,
            @Param("maxCpc") double maxCpc);
}
