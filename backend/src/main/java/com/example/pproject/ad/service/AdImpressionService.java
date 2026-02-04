package com.example.pproject.ad.service;

import com.example.pproject.ad.dto.AdServeResponseDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.entity.AdImpressionEventEntity;
import com.example.pproject.ad.repository.AdImpressionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdImpressionService {

    private final AdImpressionEventRepository adImpressionEventRepository;
    private final StringRedisTemplate redisTemplate;

    // 노출 중복 방지 시간 (10분)
    private static final Duration IMPRESSION_DEDUP_TTL = Duration.ofMinutes(10);
    // Redis Key Prefix
    private static final String IMPRESSION_KEY_PREFIX = "ad:impression:";

    /**
     * 광고 노출 기록 (DTO 리스트 기반) - Redis 중복 방지 적용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackImpressions(List<AdServeResponseDTO> ads, Long memberId) {
        if (ads == null || ads.isEmpty())
            return;

        // 중복 필터링: Redis에 없는 광고만 DB에 저장
        List<AdImpressionEventEntity> newImpressions = ads.stream()
                .filter(ad -> isNewImpression(ad.getCampaignId(), memberId))
                .map(ad -> AdImpressionEventEntity.builder()
                        .campaignId(ad.getCampaignId())
                        .memberId(memberId)
                        .occurredAt(Instant.now())
                        .build())
                .toList();

        if (!newImpressions.isEmpty()) {
            adImpressionEventRepository.saveAll(newImpressions);
            log.debug("Recorded {} new impressions (filtered from {}) for memberId: {}",
                    newImpressions.size(), ads.size(), memberId);
        }
    }

    /**
     * 광고 노출 기록 (Entity 리스트 기반) - Redis 중복 방지 적용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackImpressionsFromEntities(List<AdCampaignEntity> ads, Long memberId) {
        if (ads == null || ads.isEmpty())
            return;

        // 중복 필터링: Redis에 없는 광고만 DB에 저장
        List<AdImpressionEventEntity> newImpressions = ads.stream()
                .filter(ad -> isNewImpression(ad.getId(), memberId))
                .map(ad -> AdImpressionEventEntity.builder()
                        .campaignId(ad.getId())
                        .memberId(memberId)
                        .occurredAt(Instant.now())
                        .build())
                .toList();

        if (!newImpressions.isEmpty()) {
            adImpressionEventRepository.saveAll(newImpressions);
            log.debug("Recorded {} new impressions (filtered from {}) for memberId: {}",
                    newImpressions.size(), ads.size(), memberId);
        }
    }

    /**
     * Redis를 활용한 노출 중복 체크 (SET NX + TTL 패턴)
     * 
     * @return true: 신규 노출 (기록해야 함), false: 중복 노출 (기록 생략)
     */
    private boolean isNewImpression(Long campaignId, Long memberId) {
        // 비로그인 사용자는 중복 체크 불가 -> 항상 기록
        if (memberId == null) {
            return true;
        }

        String key = buildRedisKey(campaignId, memberId);

        try {
            // SET NX (키가 없을 때만 설정) + TTL 10분
            // setIfAbsent: 키가 없으면 true 반환 (신규), 있으면 false 반환 (중복)
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", IMPRESSION_DEDUP_TTL);

            return Boolean.TRUE.equals(isNew);
        } catch (Exception e) {
            // Redis 장애 시 Fallback: 일단 기록 (서비스 중단 방지)
            log.warn("Redis unavailable for impression dedup. Falling back to record. Key: {}", key, e);
            return true;
        }
    }

    /**
     * Redis Key 생성: ad:impression:{campaignId}:{memberId}
     */
    private String buildRedisKey(Long campaignId, Long memberId) {
        return IMPRESSION_KEY_PREFIX + campaignId + ":" + memberId;
    }
}
