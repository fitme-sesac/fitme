package com.example.pproject.ad.repository;

import com.example.pproject.ad.entity.AdCampaignEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
