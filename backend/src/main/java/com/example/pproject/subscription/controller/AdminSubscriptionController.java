package com.example.pproject.subscription.controller;

import com.example.pproject.Constant.CreditStatus;
import com.example.pproject.Constant.PaymentStatus;
import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.subscription.dto.SubscriptionBillingCycleResponse;
import com.example.pproject.subscription.dto.SubscriptionResponse;
import com.example.pproject.subscription.service.SubscriptionBillingCycleService;
import com.example.pproject.subscription.service.SubscriptionService;
import com.example.pproject.wallet.entity.WalletLedger;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/api/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionBillingCycleService billingCycleService;

    // =============================================================================================
    // [구독 관리] - SubscriptionService
    // =============================================================================================

    /**
     * 모든 구독 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        List<SubscriptionResponse> responses = subscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(responses);
    }

    /**
     * 활성 구독(기업) 수 조회
     */
    @GetMapping("/count/active")
    public ResponseEntity<Long> countActiveSubscriptions() {
        return ResponseEntity.ok(subscriptionService.countActiveSubscriptions());
    }

    /**
     * 구독 상태 강제 변경
     */
    @PutMapping("/{subscriptionId}/status")
    public ResponseEntity<Void> updateSubscriptionStatus(
            @PathVariable Long subscriptionId,
            @RequestParam SubscriptionStatus newStatus) {
        subscriptionService.updateSubscriptionStatus(subscriptionId, newStatus);
        return ResponseEntity.ok().build();
    }

    /**
     * 구독 강제 활성화
     */
    @PostMapping("/{subscriptionId}/activate")
    public ResponseEntity<Void> activateSubscription(@PathVariable Long subscriptionId) {
        subscriptionService.activateSubscription(subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 예약된 상품 변경 강제 적용 (수동 트리거)
     */
    @PostMapping("/{subscriptionId}/apply-product-change")
    public ResponseEntity<Void> applyScheduledProductChange(@PathVariable Long subscriptionId) {
        subscriptionService.applyScheduledProductChange(subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 결제 실패 처리 (수동 트리거)
     */
    @PostMapping("/{subscriptionId}/mark-payment-failed")
    public ResponseEntity<Void> markPaymentFailed(@PathVariable Long subscriptionId) {
        subscriptionService.markPaymentFailed(subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 다음 결제일 스케줄링 (수동 트리거)
     */
    @PostMapping("/{subscriptionId}/schedule-next-billing")
    public ResponseEntity<Void> scheduleNextBilling(@PathVariable Long subscriptionId) {
        subscriptionService.scheduleNextBilling(subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 결제 가능 상태 확인 (디버깅용)
     */
    @GetMapping("/{subscriptionId}/validate-billable")
    public ResponseEntity<Void> validateBillableState(@PathVariable Long subscriptionId) {
        subscriptionService.validateBillableState(subscriptionId);
        return ResponseEntity.ok().build();
    }

    // =============================================================================================
    // [결제/정산 관리] - SubscriptionBillingCycleService
    // =============================================================================================

    /**
     * 모든 결제 주기 목록 조회
     */
    @GetMapping("/billing-cycles")
    public ResponseEntity<List<SubscriptionBillingCycleResponse>> getAllBillingCycles() {
        List<SubscriptionBillingCycleResponse> responses = billingCycleService.getAllBillingCycles();
        return ResponseEntity.ok(responses);
    }

    /**
     * 월별 빌링 사이클 생성 (청구서 생성)
     */
    @PostMapping("/billing-cycles/create")
    public ResponseEntity<SubscriptionBillingCycleResponse> createBillingCycle(
            @RequestParam Long subscriptionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingMonth) {
        SubscriptionBillingCycleResponse response = billingCycleService.createBillingCycle(subscriptionId, billingMonth);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 성공 처리
     */
    @PostMapping("/billing-cycles/{cycleId}/mark-success")
    public ResponseEntity<Void> markPaymentSuccess(
            @PathVariable Long cycleId,
            @RequestBody Payment payment) {
        billingCycleService.markPaymentSuccess(cycleId, payment);
        return ResponseEntity.ok().build();
    }

    /**
     * 결제 실패 처리
     */
    @PostMapping("/billing-cycles/{cycleId}/mark-failed")
    public ResponseEntity<Void> markPaymentFailedCycle(@PathVariable Long cycleId) {
        billingCycleService.markPaymentFailed(cycleId);
        return ResponseEntity.ok().build();
    }

    /**
     * 결제 상태 강제 변경
     */
    @PutMapping("/billing-cycles/{cycleId}/payment-status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable Long cycleId,
            @RequestParam PaymentStatus newStatus) {
        billingCycleService.updatePaymentStatus(cycleId, newStatus);
        return ResponseEntity.ok().build();
    }

    /**
     * 크레딧 지급
     */
    @PostMapping("/billing-cycles/{cycleId}/grant-credit")
    public ResponseEntity<Void> grantCredit(
            @PathVariable Long cycleId,
            @RequestParam Integer creditAmount,
            @RequestBody WalletLedger walletLedger) {
        billingCycleService.grantCredit(cycleId, creditAmount, walletLedger);
        return ResponseEntity.ok().build();
    }

    /**
     * 크레딧 회수
     */
    @PostMapping("/billing-cycles/{cycleId}/revoke-credit")
    public ResponseEntity<Void> revokeCredit(@PathVariable Long cycleId) {
        billingCycleService.revokeCredit(cycleId);
        return ResponseEntity.ok().build();
    }

    /**
     * 크레딧 상태 강제 변경
     */
    @PutMapping("/billing-cycles/{cycleId}/credit-status")
    public ResponseEntity<Void> updateCreditStatus(
            @PathVariable Long cycleId,
            @RequestParam CreditStatus newStatus) {
        billingCycleService.updateCreditStatus(cycleId, newStatus);
        return ResponseEntity.ok().build();
    }
}
