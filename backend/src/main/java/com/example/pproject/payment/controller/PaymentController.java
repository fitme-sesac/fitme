package com.example.pproject.payment.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.payment.dto.request.PaymentCancelRequest;
import com.example.pproject.payment.dto.request.PaymentConfirmRequest;
import com.example.pproject.payment.dto.request.PaymentCreateRequest;
import com.example.pproject.payment.dto.response.PaymentCancelResponse;
import com.example.pproject.payment.dto.response.PaymentResponse;
import com.example.pproject.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 1. 결제 생성 (요청)
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody @Valid PaymentCreateRequest request) {
        Long userId = principal.getId();
        return ResponseEntity.ok(paymentService.createPayment(userId, request));
    }

    /**
     * 2. 결제 승인 (최종 완료)
     */
    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody @Valid PaymentConfirmRequest request) {
        Long userId = principal.getId();
        return ResponseEntity.ok(paymentService.confirmPayment(userId, request));
    }

    /**
     * 3. 결제 취소
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelPayment(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String orderId,
            @RequestBody @Valid PaymentCancelRequest request) {
        Long userId = principal.getId();
        paymentService.cancelPayment(userId, orderId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 4. 내 결제 목록 조회
     * - roleType 파라미터를 통해 개인/기업 구분 (기본값: CANDIDATE)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "CANDIDATE") RoleType roleType,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = principal.getId();
        return ResponseEntity.ok(paymentService.getMyPayments(userId, roleType, pageable));
    }

    /**
     * 5. 결제 상세 조회
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String orderId) {
        Long userId = principal.getId();
        return ResponseEntity.ok(paymentService.getPayment(userId, orderId));
    }

    /**
     * 6. 결제 취소 이력 조회
     */
    @GetMapping("/{orderId}/cancels")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentCancelResponse>> getPaymentCancels(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String orderId) {
        Long userId = principal.getId();
        return ResponseEntity.ok(paymentService.getPaymentCancels(userId, orderId));
    }
}
