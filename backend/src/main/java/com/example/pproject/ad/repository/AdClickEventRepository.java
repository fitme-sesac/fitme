package com.example.pproject.ad.repository;

import com.example.pproject.ad.entity.AdClickEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdClickEventRepository extends JpaRepository<AdClickEventEntity, Long> {

    // 중복 클릭 방지를 위해 clickKey로 조회
    Optional<AdClickEventEntity> findByClickKey(String clickKey);

    // 특정 캠페인의 클릭 수 조회
    long countByCampaignId(Long campaignId);
}
