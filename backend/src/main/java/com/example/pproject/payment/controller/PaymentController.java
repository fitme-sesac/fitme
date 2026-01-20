package com.example.pproject.payment.controller;

import com.example.pproject.payment.dto.request.PaymentCancelRequest;
import com.example.pproject.payment.dto.request.PaymentConfirmRequest;
import com.example.pproject.payment.dto.request.PaymentCreateRequest;
import com.example.pproject.payment.dto.response.PaymentResponse;
import com.example.pproject.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 1. 결제 생성 (요청)
     * - 사용자가 '결제하기' 버튼을 눌렀을 때 호출
     * - 주문 ID 생성 및 결제 정보 저장 (READY 상태)
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid PaymentCreateRequest request
    ) {
        // TODO: CustomUserDetails 구현 후 getId()로 변경 권장
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(paymentService.createPayment(userId, request));
    }

    /**
     * 2. 결제 승인 (최종 완료)
     * - 토스 인증 성공 후 호출 (successUrl 리다이렉트 후 클라이언트가 호출)
     * - 토스 승인 API 호출 -> DB 업데이트 -> 지갑 충전
     * - Resilience4j 적용됨 (TossPaymentAdapter 내부)
     */
    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid PaymentConfirmRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(paymentService.confirmPayment(userId, request));
    }

    /**
     * 3. 결제 취소
     * - 사용자가 결제 취소 요청 시 호출
     * - Resilience4j 적용됨 (TossPaymentAdapter 내부)
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String orderId,
            @RequestBody @Valid PaymentCancelRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        paymentService.cancelPayment(userId, orderId, request);
        return ResponseEntity.ok().build();
    }
}
