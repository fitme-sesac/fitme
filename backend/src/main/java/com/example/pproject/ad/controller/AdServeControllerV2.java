package com.example.pproject.ad.controller;

import com.example.pproject.ad.dto.AdServeResponseDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.service.AdCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [V2] Redis 기반 광고 노출 API
 * - nGrinder 부하 테스트용 (V1 vs V2 성능 비교)
 * - Redis Active Set을 필터로 사용하여 불필요한 DB 조회를 최소화함.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/ad/serve")
@RequiredArgsConstructor
public class AdServeControllerV2 {

    private final AdCampaignService adCampaignService;

    /**
     * [비로그인 사용자용] 입찰가 높은 순 (Redis Filtered)
     */
    @GetMapping
    public ResponseEntity<List<AdServeResponseDTO>> getAdsToServe(
            @RequestParam(defaultValue = "5") int limit) {

        int safeLimit = Math.min(limit, 10);

        // V2: Redis Active Set 기반 조회
        Page<AdCampaignEntity> activeAds = adCampaignService.getActiveAdsForServing(
                PageRequest.of(0, safeLimit));

        List<AdServeResponseDTO> result = activeAds.getContent().stream()
                .map(AdServeResponseDTO::fromEntity)
                .toList();

        log.debug("Ad serve request (V2/Redis/Anonymous). Returned {} ads", result.size());

        return ResponseEntity.ok(result);
    }

    /**
     * [로그인 사용자용] 유사도 기반 매칭 (Redis Filtered)
     */
    @GetMapping("/match")
    public ResponseEntity<List<AdServeResponseDTO>> getMatchedAds(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "5") int limit) {

        int safeLimit = Math.min(limit, 10);

        // V2: Redis Active Set 기반 유사도 매칭
        List<AdServeResponseDTO> result = adCampaignService.getAdsForMember(memberId, safeLimit);

        log.debug("Ad serve request (V2/Redis/Matched). MemberId: {}, Returned {} ads", memberId, result.size());

        return ResponseEntity.ok(result);
    }
}
