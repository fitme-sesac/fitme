package com.example.pproject.ad.repository;

import com.example.pproject.ad.entity.AdClickEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AdClickEventRepository extends JpaRepository<AdClickEventEntity, Long> {

    // 중복 클릭 방지를 위해 clickKey로 조회
    Optional<AdClickEventEntity> findByClickKey(String clickKey);

    // 특정 캠페인의 클릭 수 조회
    long countByCampaignId(Long campaignId);

    /**
     * [10분 시간 윈도우 중복 체크]
     * - 같은 사용자가 같은 광고를 10분 내에 클릭했는지 확인
     * - 있으면 true (중복), 없으면 false
     */
    @Query("SELECT COUNT(e) > 0 FROM AdClickEventEntity e " +
            "WHERE e.campaignId = :campaignId " +
            "AND e.memberId = :memberId " +
            "AND e.occurredAt >= :since")
    boolean existsByRecentClick(
            @Param("campaignId") Long campaignId,
            @Param("memberId") Long memberId,
            @Param("since") Instant since);

    /**
     * [비로그인 사용자용 - IP 또는 세션 기반 체크가 필요하면 추가]
     * 현재는 memberId가 null이면 중복 체크 스킵
     */
}
