package com.example.pproject.ad.service;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.SourceType;
import com.example.pproject.ad.dto.AdClickEventCreateDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.entity.AdClickEventEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import com.example.pproject.ad.repository.AdClickEventRepository;
import com.example.pproject.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 광고 클릭 이벤트 처리 서비스
 * - 클릭 기록 + 예산 차감(과금) 로직을 담당합니다.
 * 
 * [중복 클릭 방지 정책]
 * - 같은 사용자(memberId)가 같은 광고(campaignId)를 10분 내에 재클릭 시 무시
 * - 비로그인 사용자(memberId == null)는 clickKey로만 중복 체크
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdClickService {

    private final AdClickEventRepository adClickEventRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final WalletService walletService;

    // 중복 클릭 방지 시간 윈도우 (10분)
    private static final int CLICK_DEDUP_MINUTES = 10;

    /**
     * 광고 클릭 이벤트 기록 및 과금 처리
     * 
     * 1. 중복 클릭 체크 (10분 시간 윈도우)
     * 2. 캠페인 유효성 확인 (존재 여부, ACTIVE 상태)
     * 3. 기업 지갑에서 CPC만큼 차감
     * 4. 클릭 이벤트 저장
     */
    @Transactional
    public void trackClick(AdClickEventCreateDTO dto) {
        Long campaignId = dto.getCampaignId();
        Long memberId = dto.getMemberId();

        // 1. 중복 클릭 체크
        if (isDuplicateClick(campaignId, memberId, dto.getClickKey())) {
            log.info("Duplicate click ignored. CampaignId: {}, MemberId: {}", campaignId, memberId);
            return;
        }

        // 2. 캠페인 정보 조회
        AdCampaignEntity campaign = adCampaignRepository.findByIdAndNotDeleted(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        // 캠페인이 활성 상태인지 확인
        if (!"ACTIVE".equals(campaign.getStatus())) {
            log.warn("Inactive campaign click ignored. CampaignId: {}, Status: {}", campaign.getId(),
                    campaign.getStatus());
            return;
        }

        // 3. 기업 지갑에서 CPC만큼 차감
        Long employerId = campaign.getEmployerId();
        int cpcBid = campaign.getCpcBid();
        String orderId = "AD_CLICK:" + dto.getClickKey(); // 멱등성 키

        try {
            walletService.useCredit(
                    employerId,
                    RoleType.EMPLOYER,
                    cpcBid,
                    orderId,
                    SourceType.AD_CLICK);
            log.info("Ad click charged. EmployerId: {}, Amount: {}", employerId, cpcBid);
        } catch (IllegalStateException e) {
            // 잔액 부족 시 광고 일시정지
            log.warn("Insufficient balance. Pausing campaign. EmployerId: {}", employerId);
            campaign.setStatus("PAUSED");
            return;
        }

        // 4. 클릭 이벤트 저장
        AdClickEventEntity entity = AdClickEventEntity.builder()
                .campaignId(campaignId)
                .memberId(memberId)
                .clickKey(dto.getClickKey())
                .build();

        adClickEventRepository.save(entity);
        log.info("Ad Click Recorded. CampaignId: {}, MemberId: {}, ClickKey: {}",
                campaignId, memberId, dto.getClickKey());
    }

    /**
     * 중복 클릭 여부 확인
     * 
     * [로그인 사용자] 10분 내 같은 캠페인 클릭 여부 확인
     * [비로그인 사용자] clickKey로만 확인 (기존 방식)
     */
    private boolean isDuplicateClick(Long campaignId, Long memberId, String clickKey) {
        // 로그인 사용자: 10분 시간 윈도우 체크
        if (memberId != null) {
            Instant since = Instant.now().minus(CLICK_DEDUP_MINUTES, ChronoUnit.MINUTES);
            return adClickEventRepository.existsByRecentClick(campaignId, memberId, since);
        }

        // 비로그인 사용자: clickKey로만 체크
        return adClickEventRepository.findByClickKey(clickKey).isPresent();
    }
}
