package com.example.pproject.wallet.controller;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.SourceType;
import com.example.pproject.common.service.DistributedLockService;
import com.example.pproject.global.exception.DuplicateRequestException;
import com.example.pproject.payment.repository.PaymentRepository;
import com.example.pproject.wallet.dto.WalletUseRequest;
import com.example.pproject.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * WalletController 단위 테스트
 * - useCredit 분산 락 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DistributedLockService lockService;

    @InjectMocks
    private WalletController walletController;

    // =======================================================
    // 1. 정상 크레딧 사용 테스트
    // =======================================================

    @Test
    @DisplayName("정상 크레딧 사용 - 락 획득 성공")
    void useCredit_Success() {
        // 1. [Given]
        UserDetails userDetails = new User("123", "password", Collections.emptyList());
        WalletUseRequest request = new WalletUseRequest(
                100L,
                "order-123",
                SourceType.AI,
                BuyerType.MEMBER);

        // 락 획득 성공
        given(lockService.tryLock("wallet:use:123:order-123")).willReturn(true);

        // 2. [When]
        var response = walletController.useCredit(userDetails, request);

        // 3. [Then]
        assertEquals(200, response.getStatusCode().value());
        verify(walletService).useCredit(
                eq(123L),
                eq(BuyerType.MEMBER),
                eq(100L),
                eq("order-123"),
                eq(SourceType.AI));
        verify(lockService).unlock("wallet:use:123:order-123");
    }

    @Test
    @DisplayName("EMPLOYER 타입 크레딧 사용 - 정상 처리")
    void useCredit_Employer_Success() {
        // 1. [Given]
        UserDetails userDetails = new User("456", "password", Collections.emptyList());
        WalletUseRequest request = new WalletUseRequest(
                200L,
                "order-456",
                SourceType.AD_CLICK,
                BuyerType.EMPLOYER);

        given(lockService.tryLock("wallet:use:456:order-456")).willReturn(true);

        // 2. [When]
        var response = walletController.useCredit(userDetails, request);

        // 3. [Then]
        assertEquals(200, response.getStatusCode().value());
        verify(walletService).useCredit(
                eq(456L),
                eq(BuyerType.EMPLOYER),
                eq(200L),
                eq("order-456"),
                eq(SourceType.AD_CLICK));
        verify(lockService).unlock("wallet:use:456:order-456");
    }

    // =======================================================
    // 2. 분산 락 실패 (중복 요청) 테스트
    // =======================================================

    @Test
    @DisplayName("중복 요청 - 락 획득 실패 시 DuplicateRequestException 발생")
    void useCredit_DuplicateRequest_ThrowsException() {
        // 1. [Given]
        UserDetails userDetails = new User("123", "password", Collections.emptyList());
        WalletUseRequest request = new WalletUseRequest(
                100L,
                "order-123",
                SourceType.AI,
                BuyerType.MEMBER);

        // 락 획득 실패 (이미 다른 요청이 처리 중)
        given(lockService.tryLock("wallet:use:123:order-123")).willReturn(false);

        // 2. [When & Then]
        DuplicateRequestException exception = assertThrows(
                DuplicateRequestException.class,
                () -> walletController.useCredit(userDetails, request));

        assertEquals("이미 처리 중인 요청입니다.", exception.getMessage());

        // walletService는 호출되지 않아야 함
        verify(walletService, never()).useCredit(anyLong(), any(), anyLong(), anyString(), any());
        // unlock도 호출되지 않아야 함 (락을 획득하지 못했으므로)
        verify(lockService, never()).unlock(anyString());
    }

    // =======================================================
    // 3. 예외 발생 시 락 해제 보장 테스트
    // =======================================================

    @Test
    @DisplayName("서비스 예외 발생 시에도 락 해제 보장")
    void useCredit_ServiceException_UnlockGuaranteed() {
        // 1. [Given]
        UserDetails userDetails = new User("123", "password", Collections.emptyList());
        WalletUseRequest request = new WalletUseRequest(
                100L,
                "order-123",
                SourceType.AI,
                BuyerType.MEMBER);

        given(lockService.tryLock("wallet:use:123:order-123")).willReturn(true);

        // 서비스에서 예외 발생
        doThrow(new IllegalStateException("잔액 부족"))
                .when(walletService).useCredit(anyLong(), any(), anyLong(), anyString(), any());

        // 2. [When & Then]
        assertThrows(
                IllegalStateException.class,
                () -> walletController.useCredit(userDetails, request));

        // 예외 발생해도 unlock은 반드시 호출되어야 함 (finally 블록)
        verify(lockService).unlock("wallet:use:123:order-123");
    }

    // =======================================================
    // 4. 동일 사용자 다른 주문 테스트
    // =======================================================

    @Test
    @DisplayName("동일 사용자 다른 주문 - 각각 독립적인 락")
    void useCredit_SameUser_DifferentOrders_IndependentLocks() {
        // 1. [Given]
        UserDetails userDetails = new User("123", "password", Collections.emptyList());
        WalletUseRequest request1 = new WalletUseRequest(
                100L,
                "order-aaa",
                SourceType.AI,
                BuyerType.MEMBER);
        WalletUseRequest request2 = new WalletUseRequest(
                200L,
                "order-bbb",
                SourceType.AI,
                BuyerType.MEMBER);

        given(lockService.tryLock("wallet:use:123:order-aaa")).willReturn(true);
        given(lockService.tryLock("wallet:use:123:order-bbb")).willReturn(true);

        // 2. [When]
        var response1 = walletController.useCredit(userDetails, request1);
        var response2 = walletController.useCredit(userDetails, request2);

        // 3. [Then] - 둘 다 성공
        assertEquals(200, response1.getStatusCode().value());
        assertEquals(200, response2.getStatusCode().value());

        verify(walletService, times(2)).useCredit(anyLong(), any(), anyLong(), anyString(), any());
        verify(lockService).unlock("wallet:use:123:order-aaa");
        verify(lockService).unlock("wallet:use:123:order-bbb");
    }
}
