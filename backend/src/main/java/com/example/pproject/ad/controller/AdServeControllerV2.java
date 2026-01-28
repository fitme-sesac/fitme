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
 * [V2: 광고 송출 시스템 컨트롤러]
 * <p>
 * 이 컨트롤러는 광고 노출(Serving) 및 개인화 매칭(Matching)을 담당합니다.
 * V1(DB Direct)부터 V3(Hybrid)까지의 모든 버전을 제공하여 성능 비교 및 학습이 가능하도록 구성되었습니다.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/ad/serve")
@RequiredArgsConstructor
public class AdServeControllerV2 {

        private final AdCampaignService adCampaignService;

        // =================================================================================
        // [SECTION 1: V3 Optimized - Hybrid Architecture (현행 최적화 버전)]
        // DB의 Vector Search와 Redis의 Guard(예산 체크)를 결합한 고성능 아키텍처
        // =================================================================================

        /**
         * [V3] 개인화 광고 매칭
         * - 유저의 이력서 Embedding을 기반으로 가장 적합한 광고를 추천합니다.
         * - 핵심: DB에서 후보군 추출(Recall) 후 Redis에서 고속 검증(Guard).
         */
        @GetMapping("/match")
        public ResponseEntity<List<AdServeResponseDTO>> getMatchedAds(
                        @RequestParam Long memberId,
                        @RequestParam(defaultValue = "5") int limit) {

                int safeLimit = Math.min(limit, 10);
                List<AdServeResponseDTO> result = adCampaignService.getAdsForMember(memberId, safeLimit);

                log.debug("Ad serve request (V3 Optimized). MemberId: {}, Returned {} ads", memberId, result.size());
                return ResponseEntity.ok(result);
        }

        /**
         * [V3] 일반 광고 노출 (입찰가 순)
         * - 별도의 개인화 로직 없이, 현재 활성화된 광고 중 입찰가(Score)가 높은 순으로 노출합니다.
         * - Redis ZSet을 직접 조회하므로 응답 속도가 매우 빠릅니다.
         */
        @GetMapping
        public ResponseEntity<List<AdServeResponseDTO>> getAdsToServe(
                        @RequestParam(defaultValue = "5") int limit) {

                int safeLimit = Math.min(limit, 10);
                Page<AdCampaignEntity> activeAds = adCampaignService
                                .getActiveAdsForServing(PageRequest.of(0, safeLimit));

                List<AdServeResponseDTO> result = activeAds.getContent().stream()
                                .map(AdServeResponseDTO::fromEntity)
                                .toList();

                return ResponseEntity.ok(result);
        }

        // =================================================================================
        // [SECTION 2: V2 Legacy - Redis Filtered Architecture]
        // Redis에서 활성 ID 목록을 대량으로 가져와 DB 'IN' 절에 넣는 방식 (성능 안티패턴)
        // =================================================================================

        /**
         * [V2 Legacy] 개인화 광고 매칭
         * - 문제점: 활성 광고가 많아질수록 DB 쿼리의 파라미터가 비대해져 성능이 급격히 저하됨.
         */
        @GetMapping("/match/v2-legacy")
        public ResponseEntity<List<AdServeResponseDTO>> getMatchedAdsLegacyV2(
                        @RequestParam Long memberId,
                        @RequestParam(defaultValue = "5") int limit) {

                int safeLimit = Math.min(limit, 10);
                List<Object[]> matchResults = adCampaignService.getAdsForMemberV2(memberId, safeLimit);

                List<AdServeResponseDTO> result = matchResults.stream()
                                .map(AdServeResponseDTO::fromQueryResult)
                                .toList();

                return ResponseEntity.ok(result);
        }
}
