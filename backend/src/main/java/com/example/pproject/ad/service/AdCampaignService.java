package com.example.pproject.ad.service;

import com.example.pproject.ad.dto.AdCampaignCreateDTO;
import com.example.pproject.ad.dto.AdCampaignResponseDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdCampaignService {

    private final AdCampaignRepository adCampaignRepository;

    @Transactional
    public AdCampaignResponseDTO createCampaign(AdCampaignCreateDTO dto) {
        AdCampaignEntity entity = AdCampaignEntity.builder()
                .employerId(dto.getEmployerId())
                .jobId(dto.getJobId())
                .cpcBid(dto.getCpcBid())
                .dailyBudget(dto.getDailyBudget())
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .status("ACTIVE") // 기본값
                .build();

        AdCampaignEntity saved = adCampaignRepository.save(entity);
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
        // JPA Dirty Checking으로 자동 저장
        return AdCampaignResponseDTO.fromEntity(entity);
    }
}
