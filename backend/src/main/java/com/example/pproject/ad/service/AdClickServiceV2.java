package com.example.pproject.ad.service;

import com.example.pproject.ad.dto.AdClickEventCreateDTO;
import com.example.pproject.ad.entity.AdClickEventEntity;
import com.example.pproject.ad.repository.AdClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * [Phase 2] Redis Guard + Reserved Budget 기반 클릭 처리 서비스
 * - Redis 기반 10분 중복 클릭 방지 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdClickServiceV2 {

    private final AdGuardService adGuardService;
    private final AdClickEventRepository adClickEventRepository;
    private final StringRedisTemplate redisTemplate;

    // 중복 클릭 방지 시간 (10분)
    private static final Duration CLICK_DEDUP_TTL = Duration.ofMinutes(10);
    private static final String CLICK_DEDUP_KEY_PREFIX = "ad:click:dedup:";

    @Transactional
    public void trackClick(AdClickEventCreateDTO dto) {
        Long campaignId = dto.getCampaignId();
        Long memberId = dto.getMemberId();

        // 1. 중복 클릭 체크 (Redis SET NX + TTL)
        if (!isNewClick(campaignId, memberId)) {
            log.info("Duplicate click ignored (10min window). CampaignId: {}, MemberId: {}", campaignId, memberId);
            return;
        }

        // 2. Redis Guard Check + Deduct
        long deductedAmount = adGuardService.reduceBudget(campaignId);

        if (deductedAmount < 0) {
            log.warn("Blocked by Redis Guard (Budget Exhausted or Inactive). CampaignId: {}", campaignId);
            return;
        }

        // 3. Record Event
        AdClickEventEntity entity = AdClickEventEntity.builder()
                .campaignId(campaignId)
                .memberId(memberId)
                .clickKey(dto.getClickKey())
                .cost((int) deductedAmount)
                .build();

        adClickEventRepository.save(entity);
        log.info("Ad Click V2 Recorded. CampaignId: {}, MemberId: {}, Cost: {}", campaignId, memberId, deductedAmount);
    }

    /**
     * Redis를 활용한 클릭 중복 체크 (SET NX + TTL 패턴)
     * 
     * @return true: 신규 클릭 (기록해야 함), false: 중복 클릭 (무시)
     */
    private boolean isNewClick(Long campaignId, Long memberId) {
        // 비로그인 사용자는 중복 체크 불가 -> 항상 기록
        if (memberId == null) {
            return true;
        }

        String key = CLICK_DEDUP_KEY_PREFIX + campaignId + ":" + memberId;

        try {
            // setIfAbsent: 키가 없으면 true 반환 (신규), 있으면 false 반환 (중복)
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", CLICK_DEDUP_TTL);
            return Boolean.TRUE.equals(isNew);
        } catch (Exception e) {
            // Redis 장애 시 DB 폴백 (비관적 접근: 중복으로 처리하지 않고 기록)
            log.warn("Redis unavailable for click dedup. Falling back to record. Key: {}", key, e);
            return true;
        }
    }
}
