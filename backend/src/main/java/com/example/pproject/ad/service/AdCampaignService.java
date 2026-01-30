package com.example.pproject.ad.service;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.ad.dto.AdCampaignCreateDTO;
import com.example.pproject.ad.dto.AdCampaignResponseDTO;
import com.example.pproject.ad.dto.AdCampaignUpdateDTO;
import com.example.pproject.ad.dto.AdServeResponseDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import com.example.pproject.ad.repository.AdClickEventRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdCampaignService {

    private final AdCampaignRepository adCampaignRepository;
    private final EmployerRepository employerRepository;
    private final AdClickEventRepository adClickEventRepository; // [Restored]
    private final ResumeRepository resumeRepository; // [Restored]
    private final WalletService walletService; // [Restored]
    private final AdGuardService adGuardService;

    private static final double MIN_SIMILARITY_THRESHOLD = 0.0;

    // =================================================================================
    // [SECTION 1: Admin & CRUD Manager]
    // 광고 캠페인의 생명주기를 관리하며, DB 상태 변경 시 Redis 캐시(AdGuard)를 동기화합니다.
    // =================================================================================

    /**
     * 광고 캠페인 생성
     * [검증] 기업 존재 여부, 중복 채용공고 광고 금지, 일일 예산 이상의 잔액 확인
     */
    @Transactional
    public AdCampaignResponseDTO createCampaign(AdCampaignCreateDTO dto) {
        if (!employerRepository.existsById(dto.getEmployerId())) {
            throw new IllegalArgumentException("존재하지 않는 기업입니다. Employer ID: " + dto.getEmployerId());
        }

        if (adCampaignRepository.existsByJobIdAndStatusNot(dto.getJobId(), "ENDED")) {
            throw new IllegalArgumentException("이미 해당 채용공고에 대한 활성 광고 캠페인이 존재합니다. Job ID: " + dto.getJobId());
        }

        Wallet wallet = walletService.getMyWallet(dto.getEmployerId(), RoleType.EMPLOYER);
        if (wallet.getBalance() < dto.getDailyBudget()) {
            throw new IllegalArgumentException(
                    String.format("잔액이 부족합니다. 현재 잔액: %d원, 필요 금액(일일 예산): %d원",
                            wallet.getBalance(), dto.getDailyBudget()));
        }

        Instant startAt = toStartOfDay(dto.getStartDate());
        Instant endAt = toEndOfDay(dto.getEndDate());

        AdCampaignEntity entity = AdCampaignEntity.builder()
                .employerId(dto.getEmployerId())
                .jobId(dto.getJobId())
                .cpcBid(dto.getCpcBid())
                .dailyBudget(dto.getDailyBudget())
                .startAt(startAt)
                .endAt(endAt)
                .status("ACTIVE")
                .build();

        AdCampaignEntity saved = adCampaignRepository.save(entity);
        log.info("광고 캠페인 생성 완료. CampaignId: {}, EmployerId: {}, JobId: {}",
                saved.getId(), saved.getEmployerId(), saved.getJobId());

        return AdCampaignResponseDTO.fromEntity(saved);
    }

    /**
     * 캠페인 정보 수정 (입찰가, 예산, 기간 등)
     * [Sync] Redis 캐시에 중요 정보(CPC, Budget)를 즉시 동기화합니다.
     */
    @Transactional
    public AdCampaignResponseDTO updateCampaign(Long id, AdCampaignUpdateDTO dto) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found. ID: " + id));

        if (dto.getCpcBid() != null) {
            entity.setCpcBid(dto.getCpcBid());
            adGuardService.updateCpc(entity.getId(), entity.getCpcBid()); // Redis 동기화
        }
        if (dto.getDailyBudget() != null) {
            entity.setDailyBudget(dto.getDailyBudget());
            adGuardService.updateBudget(entity.getId(), entity.getDailyBudget()); // Redis 동기화
        }
        if (dto.getStartDate() != null) {
            entity.setStartAt(toStartOfDay(dto.getStartDate()));
        }
        if (dto.getEndDate() != null) {
            entity.setEndAt(toEndOfDay(dto.getEndDate()));
        }

        log.info("광고 캠페인 수정 완료. CampaignId: {}", id);
        return AdCampaignResponseDTO.fromEntity(entity);
    }

    /**
     * 캠페인 상태 변경 (ACTIVE, PAUSED, ENDED)
     * [Sync] ACTIVE 시 Redis 노출 목록에 추가, 그 외에는 즉시 제거하여 노출을 중단합니다.
     */
    @Transactional
    public AdCampaignResponseDTO updateStatus(Long id, String status) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found. ID: " + id));

        entity.setStatus(status);
        adGuardService.updateStatus(id, status); // [Important] Redis Active Set 동기화

        return AdCampaignResponseDTO.fromEntity(entity);
    }

    public Page<AdCampaignResponseDTO> getCampaignsByEmployer(Long employerId, Pageable pageable) {
        return adCampaignRepository.findByEmployerIdAndNotDeleted(employerId, pageable)
                .map(AdCampaignResponseDTO::fromEntity);
    }

    public AdCampaignResponseDTO getCampaign(Long id) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found or deleted. ID: " + id));
        return AdCampaignResponseDTO.fromEntity(entity);
    }

    // =================================================================================
    // [SECTION 2: V3 Optimized - Hybrid Architecture]
    // 현재 사용 중인 가장 발전된 방식. DB의 Vector Index와 Redis의 고속 검증을 결합함.
    // =================================================================================

    /**
     * [V3] 맞춤형 매칭 광고 조회
     * <ol>
     * <li><b>Recall:</b> PostgreSQL의 Vector index(HNSW)를 타서 유사도 높은 '후보' 30개를 순수
     * DB에서 빠르게 뽑음.</li>
     * <li><b>Guard:</b> 뽑힌 30개 후보의 ID만 Redis에 Pipeline으로 물어봐서 "현재 돈(예산)이 있는지"
     * 검증.</li>
     * <li><b>Filter:</b> 돈 있는 광고만 최종 반환.</li>
     * </ol>
     */
    public List<AdServeResponseDTO> getAdsForMember(Long memberId, int limit) {
        // 0. 사용자 이력서 조회 (임베딩만 조회하여 최적화)
        Optional<String> embeddingOpt = resumeRepository.findEmbeddingByUserId(memberId);

        if (embeddingOpt.isEmpty()) {
            // 이력서가 없는 경우 입찰가 기반(V2 Serving)으로 Fallback
            Page<AdCampaignEntity> activeAds = getActiveAdsForServing(PageRequest.of(0, limit));
            return activeAds.getContent().stream().map(AdServeResponseDTO::fromEntity).toList();
        }

        String userEmbedding = embeddingOpt.get();
        int recallLimit = 100; // [Recall] 후보군을 넉넉하게 조회 (HNSW Index 사용)

        // 1단계: DB에서 유사도 높은 후보군 추출 (Sorted by Similarity)
        // [Optimized] maxDistance = 1.0 - minSimilarity
        double maxDistance = 1.0 - MIN_SIMILARITY_THRESHOLD;
        List<Object[]> candidates = adCampaignRepository.findTopAdsBySimilarity(
                userEmbedding, maxDistance, recallLimit, 5000.0);

        // 2단계: Redis Pipeline을 이용한 고속 예산 검증 (N+1 문제 해결)
        List<Long> campaignIdsToCheck = candidates.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();
        java.util.Map<Long, Boolean> activeStatusMap = adGuardService.checkActiveBatch(campaignIdsToCheck);

        // 3단계: 검증 통과한 광고만 수집 + [Re-Rank] Hybrid Score 정렬
        List<AdServeResponseDTO> result = new java.util.ArrayList<>();
        for (Object[] row : candidates) {
            Long campaignId = ((Number) row[0]).longValue();
            if (Boolean.TRUE.equals(activeStatusMap.get(campaignId))) {
                result.add(AdServeResponseDTO.fromQueryResult(row));
            }
        }

        // [Re-Rank] DB는 유사도 순으로 줬으므로, 최종적으로 Hybrid Score(CPC 반영)로 재정렬 필요
        result.sort((a, b) -> Double.compare(b.getHybridScore(), a.getHybridScore()));

        // 요청된 개수만큼 자르기 (Pagination)
        if (result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    // =================================================================================
    // [SECTION 3: V2 Legacy - Redis Filtered Architecture]
    // Redis에서 활성 ID 목록을 먼저 가져오고, DB에서 'IN' 절로 필터링하는 방식.
    // =================================================================================

    /**
     * [V2 Serving] 입찰가 순 광고 노출 (비로그인용 등)
     * Redis에서 현재 활성 상태인 ID 목록(상위 5,000개)을 먼저 가져와서 DB에 던짐.
     */
    public Page<AdCampaignEntity> getActiveAdsForServing(Pageable pageable) {
        java.util.Set<String> activeIdsStr = adGuardService.getActiveCampaignIds();
        if (activeIdsStr == null || activeIdsStr.isEmpty())
            return Page.empty(pageable);

        Long[] activeIds = activeIdsStr.stream().map(Long::valueOf).toArray(Long[]::new);
        return adCampaignRepository.findActiveAdsByIdsOrderByCpcDesc(activeIds, pageable);
    }

    /**
     * [V2 Legacy] Redis ID List Anti-pattern (벤치마크 비교용)
     * Redis에서 10만 개의 ID를 가져와서 DB 쿼리의 'WHERE id IN (...)'에 넣는 방식.
     * 활성 광고가 많아지면 쿼리 요청 패킷이 너무 커져서 성능이 급락함.
     */
    public List<Object[]> getAdsForMemberV2(Long memberId, int limit) {
        java.util.Set<String> activeIdsStr = adGuardService.getActiveCampaignIds();
        if (activeIdsStr == null || activeIdsStr.isEmpty())
            return java.util.Collections.emptyList();

        // [Performance Fix] 30만 개를 쿼리에 다 넣으면 터지므로 30,000개로 제한 (실험적 수치)
        Long[] activeIds = activeIdsStr.stream()
                .limit(30000)
                .map(Long::valueOf)
                .toArray(Long[]::new);

        Optional<String> embeddingOpt = resumeRepository.findEmbeddingByUserId(memberId);
        if (embeddingOpt.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // [Optimized] V2 Legacy (Redis Filtered)
        String embeddingStr = embeddingOpt.get();
        double maxDistance = 1.0 - MIN_SIMILARITY_THRESHOLD;

        return adCampaignRepository.findActiveAdsWithSimilarityAndIds(activeIds, embeddingStr, maxDistance,
                limit, 5000.0);
    }

    // =================================================================================
    // [SECTION 4: V1 Legacy - Pure Database Logic]
    // Redis 없이 오직 DB 트랜잭션과 쿼리에만 의존함. 정합성은 높으나 성능 확장에 한계가 있음.
    // =================================================================================

    /**
     * [V1 Legacy] 순수 DB 기반 조회
     */
    public Page<AdCampaignEntity> getActiveAdsForServingLegacy(Pageable pageable) {
        return adCampaignRepository.findActiveAdsOrderByCpcDesc(pageable);
    }

    /**
     * [V1 Legacy] 순수 DB 기반 유사도 매칭
     * 매 요청마다 DB 전체를 훑으며 벡터 거리를 계산함.
     */
    public List<AdServeResponseDTO> getAdsForMemberLegacy(Long memberId, int limit) {
        Optional<String> embeddingOpt = resumeRepository.findEmbeddingByUserId(memberId);

        if (embeddingOpt.isEmpty()) {
            Page<AdCampaignEntity> activeAds = adCampaignRepository
                    .findActiveAdsOrderByCpcDesc(PageRequest.of(0, limit));
            return activeAds.getContent().stream().map(AdServeResponseDTO::fromEntity).toList();
        }

        // [Optimized] V1 Legacy (Pure DB)
        double maxDistance = 1.0 - MIN_SIMILARITY_THRESHOLD;
        List<Object[]> matchResults = adCampaignRepository.findActiveAdsWithSimilarity(
                embeddingOpt.get(), maxDistance, limit, 5000.0);

        return matchResults.stream().map(AdServeResponseDTO::fromQueryResult).toList();
    }

    // =================================================================================
    // [SECTION 5: Helpers]
    // =================================================================================

    private Instant toStartOfDay(LocalDate date) {
        if (date == null)
            return null;
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant toEndOfDay(LocalDate date) {
        if (date == null)
            return null;
        return date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * [트랜잭션 분리] 캠페인 일시정지 (잔액 부족 등 예외 상황 처리용)
     * - 호출한 쪽 트랜잭션이 롤백되어도 이 변경사항은 커밋되어야 함.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void pauseCampaignInNewTx(Long campaignId) {
        log.warn("Pausing campaign due to exception/insufficient funds. ID: {}", campaignId);
        AdCampaignEntity campaign = adCampaignRepository.findById(campaignId)
                .orElse(null);
        if (campaign != null) {
            campaign.setStatus("PAUSED");
            // Dirty checking에 의해 자동 저장됨
        }
    }
}
