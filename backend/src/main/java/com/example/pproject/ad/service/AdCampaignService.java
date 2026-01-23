package com.example.pproject.ad.service;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.ad.dto.AdCampaignCreateDTO;
import com.example.pproject.ad.dto.AdCampaignResponseDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdCampaignService {

    private final AdCampaignRepository adCampaignRepository;
    private final EmployerRepository employerRepository;
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

    @Transactional
    public AdCampaignResponseDTO updateStatus(Long id, String status) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found. ID: " + id));

        entity.setStatus(status);
        return AdCampaignResponseDTO.fromEntity(entity);
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
