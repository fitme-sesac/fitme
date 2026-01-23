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
// 클래스 전체에 기본적으로 '읽기 전용' 트랜잭션을 건다.
@Transactional(readOnly = true)
public class AdCampaignService {

    private final AdCampaignRepository adCampaignRepository;

    /* 광고주(Employer)가 입력한 정보를 바탕으로 실제 광고를 생성 */
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

    /* 캠페인 조회 : 특정 고용주의 광고 목록을 페이징(Page) 처리하여 가져오거나, 특정 광고 1개를 가져온다. */
    public Page<AdCampaignResponseDTO> getCampaignsByEmployer(Long employerId, Pageable pageable) {
        return adCampaignRepository.findByEmployerIdAndNotDeleted(employerId, pageable)
                .map(AdCampaignResponseDTO::fromEntity);
    }

    /* 캠페인 전체 조회 */
    public AdCampaignResponseDTO getCampaign(Long id) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found or deleted. ID: " + id));
        return AdCampaignResponseDTO.fromEntity(entity);
    }

    /* 광고를 일시정지(PAUSED)하거나 다시 활성화(ACTIVE)할 때 사용 */
    @Transactional
    public AdCampaignResponseDTO updateStatus(Long id, String status) {
        AdCampaignEntity entity = adCampaignRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Ad Campaign not found. ID: " + id));

        entity.setStatus(status);
        // JPA Dirty Checking으로 자동 저장
        /* JPA의 변경 감지(Dirty Checking) 기능
        * 트랜잭션 안에서 조회해 온 Entity의 값을 변경(setStatus)하면, 트랜잭션이 끝나는 시점에 JPA가 알아서 변경 사항을 감지하고 DB에 UPDATE 쿼리를 날린다.
        * */
        return AdCampaignResponseDTO.fromEntity(entity);
    }
}
