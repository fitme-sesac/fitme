package com.example.pproject.ad.service;

import com.example.pproject.ad.dto.AdCampaignCreateDTO;
import com.example.pproject.ad.dto.AdCampaignResponseDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdCampaignServiceTest {

    /* 가짜 객체 생성*/
    @Mock
    private AdCampaignRepository adCampaignRepository;

    /* 가짜 객체 주입 */
    @InjectMocks
    private AdCampaignService adCampaignService;

    // =======================================================
    // 1. 캠페인 생성 테스트
    // =======================================================

    @Test
    @DisplayName("캠페인 생성이 정상 작동한다")
    void createCampaign_Success() {
        // 1. [Given] 생성 요청 DTO
        AdCampaignCreateDTO request = AdCampaignCreateDTO.builder()
                .employerId(1L)
                .jobId(100L)
                .cpcBid(50)
                .dailyBudget(10000)
                .startAt(Instant.now())
                .endAt(Instant.now().plusSeconds(86400 * 7)) // 7일 후
                .build();

        // 저장되면 반환될 엔티티
        AdCampaignEntity savedEntity = AdCampaignEntity.builder()
                .id(1L)
                .employerId(1L)
                .jobId(100L)
                .cpcBid(50)
                .dailyBudget(10000)
                .status("ACTIVE")
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();

        /* 리포지토리에 ID 조회 요청이 오면, 이 가짜 Entity(미리 만들어둔 savedEntity)를 리턴해라 */
        given(adCampaignRepository.save(any(AdCampaignEntity.class))).willReturn(savedEntity);

        // 2. [When] 캠페인 생성 -> 실제 테스트할 서비스 메서드를 호출
        AdCampaignResponseDTO response = adCampaignService.createCampaign(request);

        // 3. [Then] 결과 검증 -> 결과값이 예상과 일치하는지(assertThat), 리포지토리가 호출되었는지(verify) 확인
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmployerId()).isEqualTo(1L);
        assertThat(response.getCpcBid()).isEqualTo(50);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        verify(adCampaignRepository).save(any(AdCampaignEntity.class));
    }

    // =======================================================
    // 2. 캠페인 조회 테스트
    // =======================================================

    @Test
    @DisplayName("존재하는 캠페인 ID로 조회하면 성공한다")
    void getCampaign_Success() {
        // 1. [Given]
        Long campaignId = 1L;
        AdCampaignEntity entity = AdCampaignEntity.builder()
                .id(campaignId)
                .employerId(1L)
                .jobId(100L)
                .cpcBid(50)
                .dailyBudget(10000)
                .status("ACTIVE")
                .build();

        given(adCampaignRepository.findByIdAndNotDeleted(campaignId)).willReturn(Optional.of(entity));

        // 2. [When]
        AdCampaignResponseDTO response = adCampaignService.getCampaign(campaignId);

        // 3. [Then]
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(campaignId);
        assertThat(response.getCpcBid()).isEqualTo(50);
    }

    @Test
    @DisplayName("존재하지 않는 캠페인 ID로 조회하면 예외가 발생한다")
    void getCampaign_NotFound() {
        // 1. [Given]
        Long campaignId = 999L;
        given(adCampaignRepository.findByIdAndNotDeleted(campaignId)).willReturn(Optional.empty());

        // 2. [When & Then]
        assertThatThrownBy(() -> adCampaignService.getCampaign(campaignId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("기업별 캠페인 목록 조회가 정상 작동한다")
    void getCampaignsByEmployer_Success() {
        // 1. [Given]
        Long employerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        AdCampaignEntity entity1 = AdCampaignEntity.builder().id(1L).employerId(employerId).cpcBid(50).status("ACTIVE")
                .build();
        AdCampaignEntity entity2 = AdCampaignEntity.builder().id(2L).employerId(employerId).cpcBid(100).status("PAUSED")
                .build();
        Page<AdCampaignEntity> page = new PageImpl<>(List.of(entity1, entity2));

        given(adCampaignRepository.findByEmployerIdAndNotDeleted(employerId, pageable)).willReturn(page);

        // 2. [When]
        Page<AdCampaignResponseDTO> response = adCampaignService.getCampaignsByEmployer(employerId, pageable);

        // 3. [Then]
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent().get(0).getCpcBid()).isEqualTo(50);
        assertThat(response.getContent().get(1).getStatus()).isEqualTo("PAUSED");
    }

    // =======================================================
    // 3. 상태 변경 테스트
    // =======================================================

    @Test
    @DisplayName("캠페인 상태 변경이 정상 작동한다")
    void updateStatus_Success() {
        // 1. [Given]
        Long campaignId = 1L;
        AdCampaignEntity entity = AdCampaignEntity.builder()
                .id(campaignId)
                .employerId(1L)
                .jobId(100L)
                .cpcBid(50)
                .status("ACTIVE")
                .build();

        given(adCampaignRepository.findByIdAndNotDeleted(campaignId)).willReturn(Optional.of(entity));

        // 2. [When] PAUSED로 상태 변경
        AdCampaignResponseDTO response = adCampaignService.updateStatus(campaignId, "PAUSED");

        // 3. [Then]
        assertThat(response.getStatus()).isEqualTo("PAUSED");
    }

    @Test
    @DisplayName("존재하지 않는 캠페인 상태 변경 시 예외가 발생한다")
    void updateStatus_NotFound() {
        // 1. [Given]
        Long campaignId = 999L;
        given(adCampaignRepository.findByIdAndNotDeleted(campaignId)).willReturn(Optional.empty());

        // 2. [When & Then]
        assertThatThrownBy(() -> adCampaignService.updateStatus(campaignId, "PAUSED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
