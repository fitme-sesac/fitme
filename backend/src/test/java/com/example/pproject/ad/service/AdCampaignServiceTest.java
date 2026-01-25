package com.example.pproject.ad.service;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.ad.dto.AdCampaignCreateDTO;
import com.example.pproject.ad.dto.AdCampaignResponseDTO;
import com.example.pproject.ad.dto.AdServeResponseDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.service.WalletService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdCampaignServiceTest {

        @Mock
        private AdCampaignRepository adCampaignRepository;

        @Mock
        private EmployerRepository employerRepository;

        @Mock
        private ResumeRepository resumeRepository;

        @Mock
        private WalletService walletService;

        @InjectMocks
        private AdCampaignService adCampaignService;

        // =======================================================
        // 1. 캠페인 생성 테스트
        // =======================================================

        @Test
        @DisplayName("캠페인 생성이 정상 작동한다")
        void createCampaign_Success() {
                // 1. [Given]
                AdCampaignCreateDTO request = AdCampaignCreateDTO.builder()
                                .employerId(1L)
                                .jobId(100L)
                                .cpcBid(50)
                                .dailyBudget(10000)
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .build();

                AdCampaignEntity savedEntity = AdCampaignEntity.builder()
                                .id(1L)
                                .employerId(1L)
                                .jobId(100L)
                                .cpcBid(50)
                                .dailyBudget(10000)
                                .status("ACTIVE")
                                .build();

                // Wallet Mock 설정
                Wallet mockWallet = mock(Wallet.class);
                given(mockWallet.getBalance()).willReturn(50000L);

                // Mock 설정
                given(employerRepository.existsById(1L)).willReturn(true);
                given(adCampaignRepository.existsByJobIdAndStatusNot(100L, "ENDED")).willReturn(false);
                given(walletService.getMyWallet(1L, RoleType.EMPLOYER)).willReturn(mockWallet);
                given(adCampaignRepository.save(any(AdCampaignEntity.class))).willReturn(savedEntity);

                // 2. [When]
                AdCampaignResponseDTO response = adCampaignService.createCampaign(request);

                // 3. [Then]
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(1L);
                assertThat(response.getStatus()).isEqualTo("ACTIVE");
                verify(adCampaignRepository).save(any(AdCampaignEntity.class));
        }

        @Test
        @DisplayName("존재하지 않는 기업으로 캠페인 생성 시 예외 발생")
        void createCampaign_EmployerNotFound() {
                // 1. [Given]
                AdCampaignCreateDTO request = AdCampaignCreateDTO.builder()
                                .employerId(999L)
                                .jobId(100L)
                                .build();

                given(employerRepository.existsById(999L)).willReturn(false);

                // 2. [When & Then]
                assertThatThrownBy(() -> adCampaignService.createCampaign(request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("존재하지 않는 기업");
        }

        @Test
        @DisplayName("중복 캠페인 생성 시 예외 발생")
        void createCampaign_DuplicateCampaign() {
                // 1. [Given]
                AdCampaignCreateDTO request = AdCampaignCreateDTO.builder()
                                .employerId(1L)
                                .jobId(100L)
                                .build();

                given(employerRepository.existsById(1L)).willReturn(true);
                given(adCampaignRepository.existsByJobIdAndStatusNot(100L, "ENDED")).willReturn(true);

                // 2. [When & Then]
                assertThatThrownBy(() -> adCampaignService.createCampaign(request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("활성 광고 캠페인이 존재");
        }

        @Test
        @DisplayName("잔액 부족 시 캠페인 생성 예외 발생")
        void createCampaign_InsufficientBalance() {
                // 1. [Given]
                AdCampaignCreateDTO request = AdCampaignCreateDTO.builder()
                                .employerId(1L)
                                .jobId(100L)
                                .dailyBudget(10000)
                                .build();

                // Wallet Mock (잔액 부족)
                Wallet mockWallet = mock(Wallet.class);
                given(mockWallet.getBalance()).willReturn(5000L);

                given(employerRepository.existsById(1L)).willReturn(true);
                given(adCampaignRepository.existsByJobIdAndStatusNot(100L, "ENDED")).willReturn(false);
                given(walletService.getMyWallet(1L, RoleType.EMPLOYER)).willReturn(mockWallet);

                // 2. [When & Then]
                assertThatThrownBy(() -> adCampaignService.createCampaign(request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("잔액이 부족");
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
                                .status("ACTIVE")
                                .build();

                given(adCampaignRepository.findByIdAndNotDeleted(campaignId)).willReturn(Optional.of(entity));

                // 2. [When]
                AdCampaignResponseDTO response = adCampaignService.getCampaign(campaignId);

                // 3. [Then]
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(campaignId);
        }

        @Test
        @DisplayName("존재하지 않는 캠페인 ID로 조회하면 예외 발생")
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

                AdCampaignEntity entity1 = AdCampaignEntity.builder()
                                .id(1L).employerId(employerId).cpcBid(50).status("ACTIVE").build();
                AdCampaignEntity entity2 = AdCampaignEntity.builder()
                                .id(2L).employerId(employerId).cpcBid(100).status("PAUSED").build();
                Page<AdCampaignEntity> page = new PageImpl<>(List.of(entity1, entity2));

                given(adCampaignRepository.findByEmployerIdAndNotDeleted(employerId, pageable)).willReturn(page);

                // 2. [When]
                Page<AdCampaignResponseDTO> response = adCampaignService.getCampaignsByEmployer(employerId, pageable);

                // 3. [Then]
                assertThat(response.getTotalElements()).isEqualTo(2);
                assertThat(response.getContent().get(0).getCpcBid()).isEqualTo(50);
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
                                .status("ACTIVE")
                                .build();

                given(adCampaignRepository.findByIdAndNotDeleted(campaignId)).willReturn(Optional.of(entity));

                // 2. [When]
                AdCampaignResponseDTO response = adCampaignService.updateStatus(campaignId, "PAUSED");

                // 3. [Then]
                assertThat(response.getStatus()).isEqualTo("PAUSED");
        }

        @Test
        @DisplayName("존재하지 않는 캠페인 상태 변경 시 예외 발생")
        void updateStatus_NotFound() {
                // 1. [Given]
                Long campaignId = 999L;
                given(adCampaignRepository.findByIdAndNotDeleted(campaignId)).willReturn(Optional.empty());

                // 2. [When & Then]
                assertThatThrownBy(() -> adCampaignService.updateStatus(campaignId, "PAUSED"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("not found");
        }

        // =======================================================
        // 4. 광고 노출 테스트
        // =======================================================

        @Test
        @DisplayName("비로그인 사용자 광고 노출 - 입찰가 순")
        void getActiveAdsForServing_Success() {
                // 1. [Given]
                Pageable pageable = PageRequest.of(0, 5);
                AdCampaignEntity entity = AdCampaignEntity.builder()
                                .id(1L).employerId(1L).jobId(100L).cpcBid(50).status("ACTIVE").build();
                Page<AdCampaignEntity> page = new PageImpl<>(List.of(entity));

                given(adCampaignRepository.findActiveAdsOrderByCpcDesc(pageable)).willReturn(page);

                // 2. [When]
                Page<AdCampaignEntity> result = adCampaignService.getActiveAdsForServing(pageable);

                // 3. [Then]
                assertThat(result.getTotalElements()).isEqualTo(1);
                assertThat(result.getContent().get(0).getCpcBid()).isEqualTo(50);
        }

        @Test
        @DisplayName("로그인 사용자 - 이력서 없으면 입찰가 순 fallback")
        void getAdsForMember_NoResume_FallbackToBidBased() {
                // 1. [Given]
                Long memberId = 1L;
                given(resumeRepository.findByUserIdAndPrimaryTrue(memberId)).willReturn(Optional.empty());

                AdCampaignEntity entity = AdCampaignEntity.builder()
                                .id(1L).employerId(1L).jobId(100L).cpcBid(50).status("ACTIVE").build();
                Page<AdCampaignEntity> page = new PageImpl<>(List.of(entity));
                given(adCampaignRepository.findActiveAdsOrderByCpcDesc(any(Pageable.class))).willReturn(page);

                // 2. [When]
                List<AdServeResponseDTO> result = adCampaignService.getAdsForMember(memberId, 5);

                // 3. [Then]
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getSimilarity()).isNull(); // fallback이므로 유사도 null
        }

        @Test
        @DisplayName("로그인 사용자 - embedding 없으면 입찰가 순 fallback")
        void getAdsForMember_NoEmbedding_FallbackToBidBased() {
                // 1. [Given]
                Long memberId = 1L;

                // embedding이 null인 Resume Mock
                Resume mockResume = mock(Resume.class);
                given(mockResume.getEmbedding()).willReturn(null);
                given(resumeRepository.findByUserIdAndPrimaryTrue(memberId)).willReturn(Optional.of(mockResume));

                AdCampaignEntity entity = AdCampaignEntity.builder()
                                .id(1L).employerId(1L).jobId(100L).cpcBid(50).status("ACTIVE").build();
                Page<AdCampaignEntity> page = new PageImpl<>(List.of(entity));
                given(adCampaignRepository.findActiveAdsOrderByCpcDesc(any(Pageable.class))).willReturn(page);

                // 2. [When]
                List<AdServeResponseDTO> result = adCampaignService.getAdsForMember(memberId, 5);

                // 3. [Then]
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getSimilarity()).isNull();
        }
}
