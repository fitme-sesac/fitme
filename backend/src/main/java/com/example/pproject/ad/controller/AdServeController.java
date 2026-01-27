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
 * 광고 노출 API
 * - 프론트엔드가 "지금 보여줄 광고"를 받아가는 엔드포인트
 * 
 * [두 가지 엔드포인트]
 * 1. GET /serve : 비로그인 → 입찰가 높은 순
 * 2. GET /serve/match : 로그인 → 유사도+입찰가 하이브리드 순
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ad/serve")
@RequiredArgsConstructor
public class AdServeController {

        private final AdCampaignService adCampaignService;

        /**
         * [비로그인 사용자용] 입찰가 높은 순으로 광고 노출
         */
        @GetMapping
        public ResponseEntity<List<AdServeResponseDTO>> getAdsToServe(
                        @RequestParam(defaultValue = "5") int limit) {

                int safeLimit = Math.min(limit, 10);

                Page<AdCampaignEntity> activeAds = adCampaignService.getActiveAdsForServingLegacy(
                                PageRequest.of(0, safeLimit));

                List<AdServeResponseDTO> result = activeAds.getContent().stream()
                                .map(AdServeResponseDTO::fromEntity)
                                .toList();

                log.info("Ad serve request (V1/Legacy/Anonymous). Returned {} ads", result.size());

                return ResponseEntity.ok(result);
        }

        /**
         * [로그인 사용자용] 유사도 기반 광고 매칭
         * - Service에서 이력서 조회 + 유사도 계산 처리
         */
        @GetMapping("/match")
        public ResponseEntity<List<AdServeResponseDTO>> getMatchedAds(
                        @RequestParam Long memberId,
                        @RequestParam(defaultValue = "5") int limit) {

                int safeLimit = Math.min(limit, 10);

                // Service에서 모든 로직 처리 (Legacy DB Only)
                List<AdServeResponseDTO> result = adCampaignService.getAdsForMemberLegacy(memberId, safeLimit);

                log.info("Ad serve request (V1/Legacy/Matched). MemberId: {}, Returned {} ads", memberId,
                                result.size());

                return ResponseEntity.ok(result);
        }
}
