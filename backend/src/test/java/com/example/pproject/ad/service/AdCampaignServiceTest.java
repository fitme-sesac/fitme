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

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

        @Mock
        private AdGuardService adGuardService;

        @Mock
        private AdImpressionService adImpressionService;

        @InjectMocks
        private AdCampaignService adCampaignService;

        @Test
        @DisplayName("광고 캠페인을 성공적으로 생성한다")
        void createCampaign_Success() {
                // 1. [Given]
                AdCampaignCreateDTO dto = AdCampaignCreateDTO.builder()
                                .employerId(1L).jobId(100L).cpcBid(50).dailyBudget(1000).build();

                given(employerRepository.existsById(1L)).willReturn(true);
                given(adCampaignRepository.existsByJobIdAndStatusNot(100L, "ENDED")).willReturn(false);

                // Wallet의 balance는 @Builder에 포함되어 있지 않으므로 Mock으로 처리
                Wallet wallet = mock(Wallet.class);
                given(wallet.getBalance()).willReturn(2000L);
                given(walletService.getEmployerWallet(1L)).willReturn(wallet);

                AdCampaignEntity savedEntity = AdCampaignEntity.builder()
                                .id(1L).employerId(1L).jobId(100L).cpcBid(50).dailyBudget(1000).status("ACTIVE")
                                .build();
                given(adCampaignRepository.save(any(AdCampaignEntity.class))).willReturn(savedEntity);

                // 2. [When]
                AdCampaignResponseDTO result = adCampaignService.createCampaign(dto);

                // 3. [Then]
                assertThat(result.getId()).isEqualTo(1L);
                assertThat(result.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("존재하지 않는 기업의 캠페인 생성 시 예외가 발생한다")
        void createCampaign_EmployerNotFound() {
                // 1. [Given]
                AdCampaignCreateDTO dto = AdCampaignCreateDTO.builder().employerId(999L).build();
                given(employerRepository.existsById(999L)).willReturn(false);

                // 2. [When & Then]
                assertThatThrownBy(() -> adCampaignService.createCampaign(dto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("존재하지 않는 기업");
        }

        @Test
        @DisplayName("이미 동일한 채용공고로 활성화된 캠페인이 있으면 예외가 발생한다")
        void createCampaign_DuplicateCampaign() {
                // 1. [Given]
                AdCampaignCreateDTO dto = AdCampaignCreateDTO.builder().employerId(1L).jobId(100L).build();
                given(employerRepository.existsById(1L)).willReturn(true);
                given(adCampaignRepository.existsByJobIdAndStatusNot(100L, "ENDED")).willReturn(true);

                // 2. [When & Then]
                assertThatThrownBy(() -> adCampaignService.createCampaign(dto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("활성 광고 캠페인이 존재");
        }

        @Test
        @DisplayName("잔액이 부족하면 캠페인 생성 시 예외가 발생한다")
        void createCampaign_InsufficientBalance() {
                // 1. [Given]
                AdCampaignCreateDTO dto = AdCampaignCreateDTO.builder()
                                .employerId(1L).jobId(100L).dailyBudget(5000).build();

                given(employerRepository.existsById(1L)).willReturn(true);
                given(adCampaignRepository.existsByJobIdAndStatusNot(100L, "ENDED")).willReturn(false);

                // Wallet의 balance는 @Builder에 포함되어 있지 않으므로 Mock으로 처리
                Wallet wallet = mock(Wallet.class);
                given(wallet.getBalance()).willReturn(1000L); // 5000원 필요한데 1000원뿐
                given(walletService.getEmployerWallet(1L)).willReturn(wallet);

                // 2. [When & Then]
                assertThatThrownBy(() -> adCampaignService.createCampaign(dto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("잔액이 부족");
        }

        @Test
        @DisplayName("광고 캠페인 단건 조회 성공")
        void getCampaign_Success() {
                // 1. [Given]
                Long id = 1L;
                AdCampaignEntity entity = AdCampaignEntity.builder()
                                .id(id).employerId(1L).jobId(100L).cpcBid(50).status("ACTIVE").build();

                given(adCampaignRepository.findByIdAndNotDeleted(id)).willReturn(Optional.of(entity));

                // 2. [When]
                AdCampaignResponseDTO result = adCampaignService.getCampaign(id);

                // 3. [Then]
                assertThat(result.getId()).isEqualTo(id);
                assertThat(result.getCpcBid()).isEqualTo(50);
        }

        @Test
        @DisplayName("존재하지 않는 광고 조회 시 예외가 발생한다")
        void getCampaign_NotFound() {
                // 1. [Given]
                Long id = 999L;
                given(adCampaignRepository.findByIdAndNotDeleted(id)).willReturn(Optional.empty());

                // 2. [When & Then]
                assertThatThrownBy(() -> adCampaignService.getCampaign(id))
                                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("기업별 광고 목록 조회 성공")
        void getCampaignsByEmployer_Success() {
                // 1. [Given]
                Long employerId = 1L;
                Pageable pageable = PageRequest.of(0, 10);
                AdCampaignEntity entity = AdCampaignEntity.builder()
                                .id(1L).employerId(employerId).status("ACTIVE").build();
                Page<AdCampaignEntity> page = new PageImpl<>(List.of(entity));

                given(adCampaignRepository.findByEmployerIdAndNotDeleted(employerId, pageable)).willReturn(page);

                // 2. [When]
                Page<AdCampaignResponseDTO> result = adCampaignService.getCampaignsByEmployer(employerId, pageable);

                // 3. [Then]
                assertThat(result.getTotalElements()).isEqualTo(1);
                assertThat(result.getContent().get(0).getEmployerId()).isEqualTo(employerId);
        }

        @Test
        @DisplayName("캠페인 상태 변경이 정상 작동한다")
        void updateStatus_Success() {
                // 1. [Given]
                Long campaignId = 1L;
                AdCampaignEntity entity = AdCampaignEntity.builder()
                                .id(campaignId).employerId(1L).status("ACTIVE").build();

                given(adCampaignRepository.findByIdAndNotDeleted(campaignId)).willReturn(Optional.of(entity));

                // 2. [When]
                AdCampaignResponseDTO response = adCampaignService.updateStatus(campaignId, "PAUSED");

                // 3. [Then]
                assertThat(response.getStatus()).isEqualTo("PAUSED");
                verify(adGuardService).updateStatus(campaignId, "PAUSED");
        }

        @Test
        @DisplayName("비로그인 사용자 광고 노출 - 입찰가 순")
        void getActiveAdsForServing_Success() {
                // 1. [Given]
                Pageable pageable = PageRequest.of(0, 5);
                AdCampaignEntity entity = AdCampaignEntity.builder()
                                .id(1L).employerId(1L).jobId(100L).cpcBid(50).status("ACTIVE").build();
                Page<AdCampaignEntity> page = new PageImpl<>(List.of(entity));

                given(adGuardService.getActiveCampaignIds()).willReturn(Set.of("1"));
                // 파라미터 타입 Long[]에 맞춰 any(Long[].class) 사용
                given(adCampaignRepository.findActiveAdsByIdsOrderByCpcDesc(any(Long[].class), any(Pageable.class)))
                                .willReturn(page);

                // 2. [When]
                Page<AdCampaignEntity> result = adCampaignService.getActiveAdsForServing(pageable, null);

                // 3. [Then]
                assertThat(result.getTotalElements()).isEqualTo(1);
                assertThat(result.getContent().get(0).getCpcBid()).isEqualTo(50);
        }

        @Test
        @DisplayName("로그인 사용자 - 이력서 없거나 임베딩 없으면 입찰가 순 fallback")
        void getAdsForMember_NoResume_FallbackToBidBased() {
                // 1. [Given]
                Long memberId = 1L;
                // [Modified] findByUserIdAndPrimaryTrue -> findEmbeddingByUserId
                given(resumeRepository.findEmbeddingByUserId(memberId)).willReturn(Optional.empty());

                AdCampaignEntity entity = AdCampaignEntity.builder()
                                .id(1L).employerId(1L).jobId(100L).cpcBid(50).status("ACTIVE").build();
                Page<AdCampaignEntity> page = new PageImpl<>(List.of(entity));

                given(adGuardService.getActiveCampaignIds()).willReturn(Set.of("1"));
                // 파라미터 타입 Long[]에 맞춰 any(Long[].class) 사용
                given(adCampaignRepository.findActiveAdsByIdsOrderByCpcDesc(any(Long[].class), any(Pageable.class)))
                                .willReturn(page);

                // 2. [When]
                List<AdServeResponseDTO> result = adCampaignService.getAdsForMember(memberId, 5);

                // 3. [Then]
                assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("로그인 사용자 - 임베딩 기반 AI 매칭 및 필터링 성공")
        void getAdsForMember_Success() {
                // 1. [Given]
                Long memberId = 1L;
                String mockEmbedding = "[0.1, 0.2, 0.3]";

                // 이력서 임베딩 조회 Mock
                given(resumeRepository.findEmbeddingByUserId(memberId)).willReturn(Optional.of(mockEmbedding));

                // DB 조회 결과 Mock (Object[] {campaignId, jobId, employerId, title, cpcBid,
                // similarity, hybridScore})
                // 광고 A: 유사도 높음(0.9), 입찰가 낮음(100) -> Hybrid Score 낮음 (0.7)
                Object[] row1 = { 1L, 101L, 10L, "Job A", 100, 0.9, 0.7 };
                // 광고 B: 유사도 낮음(0.5), 입찰가 높음(1000) -> Hybrid Score 높음 (0.9)
                Object[] row2 = { 2L, 102L, 20L, "Job B", 1000, 0.5, 0.9 };

                List<Object[]> queryResult = List.of(row1, row2);

                given(adCampaignRepository.findTopAdsBySimilarity(
                                eq(mockEmbedding), anyDouble(), anyInt(), anyDouble()))
                                .willReturn(queryResult);

                // Redis 예산 필터링 Mock (둘 다 돈이 있다고 가정)
                // 주의: checkActiveBatch는 Map<Long, Boolean>을 반환함
                java.util.Map<Long, Boolean> activeMap = java.util.Map.of(1L, true, 2L, true);
                given(adGuardService.checkActiveBatch(anyList())).willReturn(activeMap);

                // 2. [When]
                List<AdServeResponseDTO> result = adCampaignService.getAdsForMember(memberId, 10);

                // 3. [Then]
                assertThat(result).hasSize(2);

                // 정렬 로직 검증: 서비스 코드에서 Hybrid Score 기준으로 재정렬하는지 확인
                // 광고 B(Hybrid 0.9)가 광고 A(Hybrid 0.7)보다 먼저 나와야 함
                assertThat(result.get(0).getCampaignId()).isEqualTo(2L);
                assertThat(result.get(1).getCampaignId()).isEqualTo(1L);

                // 값 검증
                assertThat(result.get(0).getSimilarity()).isEqualTo(0.5);
                assertThat(result.get(0).getHybridScore()).isEqualTo(0.9);
        }
}
