package com.example.pproject.ad.service;

import com.example.pproject.ad.dto.AdClickEventCreateDTO;
import com.example.pproject.ad.entity.AdClickEventEntity;
import com.example.pproject.ad.repository.AdClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Phase 2] Redis Guard + Reserved Budget 기반 클릭 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdClickServiceV2 {

    private final AdGuardService adGuardService;
    private final AdClickEventRepository adClickEventRepository;

    @Transactional
    public void trackClick(AdClickEventCreateDTO dto) {
        Long campaignId = dto.getCampaignId();

        // [Phase 2 Refactoring]
        // DB 조회(AdCampaignRepository)를 제거하고, 모든 검증을 Redis(Unified Hash)에 위임합니다.
        // -> 성능 최적화: DB 트랜잭션 없이 Redis Atomic 연산으로 처리

        // 1. Redis Guard Check + Deduct
        // (내부적으로 Status Check, Budget Check, Active Set Management 모두 수행)
        long deductedAmount = adGuardService.reduceBudget(campaignId);

        if (deductedAmount < 0) {
            // 예산 소진, 비활성 상태, 또는 키 만료 -> 차단
            log.warn("Blocked by Redis Guard (Budget Exhausted or Inactive). CampaignId: {}", campaignId);
            return;
        }

        // 2. Record Event (Async Log)
        // 실제로는 Kafka 등으로 보내는 것이 좋으나, 현재는 DB에 로그만 비동기 성격으로 저장
        AdClickEventEntity entity = AdClickEventEntity.builder()
                .campaignId(campaignId)
                .memberId(dto.getMemberId())
                .clickKey(dto.getClickKey())
                .cost((int) deductedAmount) // 실제 차감된 금액을 과금액으로 기록 (Source of Truth)
                .build();

        adClickEventRepository.save(entity);
        log.info("Ad Click V2 Recorded (Redis Only). CampaignId: {}, Cost: {}", campaignId, deductedAmount);
    }
}
