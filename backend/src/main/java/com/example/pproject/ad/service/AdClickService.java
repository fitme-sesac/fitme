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

/**
 * 광고 클릭 이벤트 처리 서비스
 * - 클릭 기록 + 예산 차감(과금) 로직을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdClickService {

    private final AdClickEventRepository adClickEventRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final WalletService walletService;

    /**
     * 광고 클릭 이벤트 기록 및 과금 처리
     * 
     * 1. 중복 클릭 체크 (clickKey)
     * 2. 캠페인 유효성 확인 (존재 여부, ACTIVE 상태)
     * 3. 기업 지갑에서 CPC만큼 차감
     * 4. 클릭 이벤트 저장
     */
    @Transactional
    public void trackClick(AdClickEventCreateDTO dto) {
        // 1. 이미 처리된 클릭인지 확인 (Idempotency)
        if (adClickEventRepository.findByClickKey(dto.getClickKey()).isPresent()) {
            log.info("Duplicate click event ignored. Key: {}", dto.getClickKey());
            return;
        }

        // 2. 캠페인 정보 조회
        AdCampaignEntity campaign = adCampaignRepository.findByIdAndNotDeleted(dto.getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다. ID: " + dto.getCampaignId()));

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
                .campaignId(dto.getCampaignId())
                .memberId(dto.getMemberId())
                .clickKey(dto.getClickKey())
                // ledgerId는 WalletService 내부에서 처리되므로 여기서는 null
                .build();

        adClickEventRepository.save(entity);
        log.info("Ad Click Recorded. CampaignId: {}, ClickKey: {}", dto.getCampaignId(), dto.getClickKey());
    }
}
