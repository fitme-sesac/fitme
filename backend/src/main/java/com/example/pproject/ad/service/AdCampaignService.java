package com.example.pproject.ad.service;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.ad.dto.AdCampaignCreateDTO;
import com.example.pproject.ad.dto.AdCampaignResponseDTO;
import com.example.pproject.ad.dto.AdCampaignUpdateDTO;
import com.example.pproject.ad.dto.AdServeResponseDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.resume.entity.Resume;
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
    private final ResumeRepository resumeRepository;
    private final WalletService walletService;

    /**
     * 광고주(Employer)가 입력한 정보를 바탕으로 실제 광고를 생성
     * 
     * [검증 순서]
     * 1. employerId가 유효한지 확인 (존재하는 기업인지)
     * 2. 중복 캠페인 체크 (같은 job_id로 활성 광고가 있는지)
     * 3. 잔액 확인 (일일 예산 이상의 잔액이 있는지)
     */
    @Transactional
    public AdCampaignResponseDTO createCampaign(AdCampaignCreateDTO dto) {
        // [1] 기업(Employer) 존재 여부 확인
        if (!employerRepository.existsById(dto.getEmployerId())) {
            throw new IllegalArgumentException("존재하지 않는 기업입니다. Employer ID: " + dto.getEmployerId());
        }

        // [2] 중복 캠페인 체크: 해당 job_id로 이미 활성(ENDED가 아닌) 캠페인이 있으면 에러
        if (adCampaignRepository.existsByJobIdAndStatusNot(dto.getJobId(), "ENDED")) {
            throw new IllegalArgumentException("이미 해당 채용공고에 대한 활성 광고 캠페인이 존재합니다. Job ID: " + dto.getJobId());
        }

        // [3] 잔액 확인: 최소 일일 예산 이상의 잔액이 있어야 광고 생성 가능
        Wallet wallet = walletService.getMyWallet(dto.getEmployerId(), RoleType.EMPLOYER);
        if (wallet.getBalance() < dto.getDailyBudget()) {
            throw new IllegalArgumentException(
                    String.format("잔액이 부족합니다. 현재 잔액: %d원, 필요 금액(일일 예산): %d원",
                            wallet.getBalance(), dto.getDailyBudget()));
        }

        // LocalDate → Instant 변환 (시작일은 00:00:00, 종료일은 23:59:59로 설정)
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

    public Page<AdCampaignResponseDTO> getCampaignsByEmployer(Long employerId, Pageable pageable) {
        return adCampaignRepository.findByEmployerIdAndNotDeleted(employerId, pageable)
                .map(AdCampaignResponseDTO::fromEntity);
    }

    public AdCampaignResponseDTO getCampaign(Long id) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found or deleted. ID: " + id));
        return AdCampaignResponseDTO.fromEntity(entity);
    }

    /**
     * 캠페인 정보 수정 (부분 업데이트)
     * - null인 필드는 수정하지 않음
     */
    @Transactional
    public AdCampaignResponseDTO updateCampaign(Long id, AdCampaignUpdateDTO dto) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found. ID: " + id));

        // 부분 업데이트 (null이 아닌 필드만 수정)
        if (dto.getCpcBid() != null) {
            entity.setCpcBid(dto.getCpcBid());
        }
        if (dto.getDailyBudget() != null) {
            entity.setDailyBudget(dto.getDailyBudget());
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

    @Transactional
    public AdCampaignResponseDTO updateStatus(Long id, String status) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found. ID: " + id));

        entity.setStatus(status);
        return AdCampaignResponseDTO.fromEntity(entity);
    }

    /**
     * [광고 노출용] 현재 활성 상태인 광고 목록 조회
     * - ACTIVE 상태이고, 현재 시간이 start_at ~ end_at 사이인 광고
     * - CPC 입찰가 높은 순으로 정렬 (경매 방식)
     */
    public Page<AdCampaignEntity> getActiveAdsForServing(Pageable pageable) {
        return adCampaignRepository.findActiveAdsOrderByCpcDesc(pageable);
    }

    /**
     * [유사도 기반 광고 매칭]
     * - 사용자의 대표 이력서 embedding을 가져와서
     * - 활성 광고의 채용공고와 유사도 계산
     * - 하이브리드 스코어 (similarity * 0.7 + bid * 0.3) 순으로 정렬
     * - 유사도 0.5 미만인 광고는 제외
     * 
     * @param resumeEmbedding 사용자 이력서의 embedding 벡터
     * @param limit           가져올 광고 개수
     * @return 유사도 기반 정렬된 광고 매칭 결과
     */
    private static final double MIN_SIMILARITY_THRESHOLD = 0.5;

    public List<Object[]> getAdsWithSimilarity(List<Double> resumeEmbedding, int limit) {
        // embedding을 PostgreSQL vector 형식 문자열로 변환: [0.1, 0.2, ...] 형태
        String embeddingStr = resumeEmbedding.toString();

        return adCampaignRepository.findActiveAdsWithSimilarity(embeddingStr, MIN_SIMILARITY_THRESHOLD, limit);
    }

    /**
     * [로그인 사용자용 광고 조회]
     * - 사용자의 대표 이력서를 조회하고, embedding이 있으면 유사도 기반 매칭
     * - embedding이 없으면 입찰가 순으로 fallback
     * 
     * @param memberId 사용자 ID
     * @param limit    가져올 광고 개수
     * @return 광고 목록 (DTO)
     */
    public List<AdServeResponseDTO> getAdsForMember(Long memberId, int limit) {
        // 1. 사용자의 대표 이력서 조회
        Optional<Resume> primaryResume = resumeRepository.findByUserIdAndPrimaryTrue(memberId);

        if (primaryResume.isEmpty() || primaryResume.get().getEmbedding() == null) {
            // 대표 이력서나 embedding이 없으면 입찰가 순으로 fallback
            log.info("No primary resume or embedding for memberId: {}. Falling back to bid-based.", memberId);

            Page<AdCampaignEntity> activeAds = getActiveAdsForServing(PageRequest.of(0, limit));

            return activeAds.getContent().stream()
                    .map(AdServeResponseDTO::fromEntity)
                    .toList();
        }

        // 2. 유사도 기반 광고 매칭
        List<Double> userEmbedding = primaryResume.get().getEmbedding();
        List<Object[]> matchResults = getAdsWithSimilarity(userEmbedding, limit);

        // 3. DTO로 변환
        return matchResults.stream()
                .map(AdServeResponseDTO::fromQueryResult)
                .toList();
    }

    // =======================================================
    // 날짜 변환 헬퍼 메서드
    // =======================================================

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
}
