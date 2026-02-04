package com.example.pproject.ad.repository;

import com.example.pproject.ad.entity.AdImpressionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AdImpressionEventRepository extends JpaRepository<AdImpressionEventEntity, Long> {

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndOccurredAtBetween(Long campaignId, Instant start, Instant end);
}
