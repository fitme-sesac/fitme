package com.example.pproject.ad.service;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.SourceType;
import com.example.pproject.ad.dto.AdClickEventCreateDTO;
import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.entity.AdClickEventEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import com.example.pproject.ad.repository.AdClickEventRepository;
import com.example.pproject.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdClickServiceTest {

        @Mock
        private AdClickEventRepository adClickEventRepository;

        @Mock
        private AdCampaignRepository adCampaignRepository;

        @Mock
        private WalletService walletService;

        @Mock
        private AdCampaignService adCampaignService;

        @InjectMocks
        private AdClickService adClickService;

        // =======================================================
        // 1. 정상 클릭 처리 테스트
        // =======================================================

        @Test
        @DisplayName("정상 클릭 - 과금 및 이벤트 저장")
        void trackClick_Success() {
                // 1. [Given]
                AdClickEventCreateDTO dto = AdClickEventCreateDTO.builder()
                                .campaignId(1L)
                                .memberId(123L)
                                .clickKey("c1_abc12345")
                                .build();

                AdCampaignEntity campaign = AdCampaignEntity.builder()
                                .id(1L)
                                .employerId(10L)
                                .cpcBid(50)
                                .status("ACTIVE")
                                .build();

                // 중복 클릭 아님
                given(adClickEventRepository.existsByRecentClick(eq(1L), eq(123L), any(Instant.class)))
                                .willReturn(false);
                given(adCampaignRepository.findByIdAndNotDeleted(1L))
                                .willReturn(Optional.of(campaign));

                // 2. [When]
                adClickService.trackClick(dto);

                // 3. [Then]
                verify(walletService).useCredit(
                                eq(10L), eq(BuyerType.EMPLOYER), eq(50L), anyString(), eq(SourceType.AD_CLICK));
                verify(adClickEventRepository).save(any(AdClickEventEntity.class));
        }

        // =======================================================
        // 2. 10분 내 중복 클릭 무시 테스트
        // =======================================================

        @Test
        @DisplayName("10분 내 중복 클릭 - 과금 안 함")
        void trackClick_DuplicateWithin10Minutes_Ignored() {
                // 1. [Given]
                AdClickEventCreateDTO dto = AdClickEventCreateDTO.builder()
                                .campaignId(1L)
                                .memberId(123L)
                                .clickKey("c1_abc12345")
                                .build();

                // 10분 내 이미 클릭 있음
                given(adClickEventRepository.existsByRecentClick(eq(1L), eq(123L), any(Instant.class)))
                                .willReturn(true);

                // 2. [When]
                adClickService.trackClick(dto);

                // 3. [Then] - 과금 안 함, 저장 안 함
                verify(walletService, never()).useCredit(anyLong(), any(), anyLong(), anyString(), any());
                verify(adClickEventRepository, never()).save(any());
        }

        // =======================================================
        // 3. 비로그인 사용자 테스트
        // =======================================================

        @Test
        @DisplayName("비로그인 사용자 - clickKey로 중복 체크")
        void trackClick_Anonymous_CheckByClickKey() {
                // 1. [Given]
                AdClickEventCreateDTO dto = AdClickEventCreateDTO.builder()
                                .campaignId(1L)
                                .memberId(null) // 비로그인
                                .clickKey("c1_anonymous123")
                                .build();

                AdCampaignEntity campaign = AdCampaignEntity.builder()
                                .id(1L)
                                .employerId(10L)
                                .cpcBid(50)
                                .status("ACTIVE")
                                .build();

                // clickKey 없음 (새 클릭)
                given(adClickEventRepository.findByClickKey("c1_anonymous123"))
                                .willReturn(Optional.empty());
                given(adCampaignRepository.findByIdAndNotDeleted(1L))
                                .willReturn(Optional.of(campaign));

                // 2. [When]
                adClickService.trackClick(dto);

                // 3. [Then]
                verify(walletService).useCredit(
                                eq(10L), eq(BuyerType.EMPLOYER), eq(50L), anyString(), eq(SourceType.AD_CLICK));
                verify(adClickEventRepository).save(any(AdClickEventEntity.class));
        }

        @Test
        @DisplayName("비로그인 사용자 - clickKey 중복이면 무시")
        void trackClick_Anonymous_DuplicateClickKey_Ignored() {
                // 1. [Given]
                AdClickEventCreateDTO dto = AdClickEventCreateDTO.builder()
                                .campaignId(1L)
                                .memberId(null)
                                .clickKey("c1_duplicate123")
                                .build();

                // clickKey 이미 존재
                given(adClickEventRepository.findByClickKey("c1_duplicate123"))
                                .willReturn(Optional.of(new AdClickEventEntity()));

                // 2. [When]
                adClickService.trackClick(dto);

                // 3. [Then] - 과금 안 함, 저장 안 함
                verify(walletService, never()).useCredit(anyLong(), any(), anyLong(), anyString(), any());
                verify(adClickEventRepository, never()).save(any());
        }

        // =======================================================
        // 4. 비활성 캠페인 테스트
        // =======================================================

        @Test
        @DisplayName("비활성 캠페인 클릭 - 무시")
        void trackClick_InactiveCampaign_Ignored() {
                // 1. [Given]
                AdClickEventCreateDTO dto = AdClickEventCreateDTO.builder()
                                .campaignId(1L)
                                .memberId(123L)
                                .clickKey("c1_inactive")
                                .build();

                AdCampaignEntity campaign = AdCampaignEntity.builder()
                                .id(1L)
                                .employerId(10L)
                                .cpcBid(50)
                                .status("PAUSED") // 비활성
                                .build();

                given(adClickEventRepository.existsByRecentClick(eq(1L), eq(123L), any(Instant.class)))
                                .willReturn(false);
                given(adCampaignRepository.findByIdAndNotDeleted(1L))
                                .willReturn(Optional.of(campaign));

                // 2. [When]
                adClickService.trackClick(dto);

                // 3. [Then] - 과금 안 함, 저장 안 함
                verify(walletService, never()).useCredit(anyLong(), any(), anyLong(), anyString(), any());
                verify(adClickEventRepository, never()).save(any());
        }

        // =======================================================
        // 5. 잔액 부족 시 캠페인 일시정지 테스트
        // =======================================================

        @Test
        @DisplayName("잔액 부족 시 캠페인 PAUSED로 변경")
        void trackClick_InsufficientBalance_CampaignPaused() {
                // 1. [Given]
                AdClickEventCreateDTO dto = AdClickEventCreateDTO.builder()
                                .campaignId(1L)
                                .memberId(123L)
                                .clickKey("c1_nobalance")
                                .build();

                AdCampaignEntity campaign = AdCampaignEntity.builder()
                                .id(1L)
                                .employerId(10L)
                                .cpcBid(50)
                                .status("ACTIVE")
                                .build();

                given(adClickEventRepository.existsByRecentClick(eq(1L), eq(123L), any(Instant.class)))
                                .willReturn(false);
                given(adCampaignRepository.findByIdAndNotDeleted(1L))
                                .willReturn(Optional.of(campaign));

                // 잔액 부족 예외 - void 메서드는 doThrow 사용
                doThrow(new IllegalStateException("잔액 부족"))
                                .when(walletService).useCredit(anyLong(), any(), anyLong(), anyString(), any());

                // 2. [When]
                org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
                        adClickService.trackClick(dto);
                });

                // 3. [Then] - 캠페인 상태가 PAUSED로 변경되었는지 확인
                // 별도 트랜잭션 메서드가 호출되었는지 확인
                verify(adCampaignService).pauseCampaignInNewTx(eq(1L));

                // 이벤트 저장은 안 되어야 함
                verify(adClickEventRepository, never()).save(any());
        }
}
