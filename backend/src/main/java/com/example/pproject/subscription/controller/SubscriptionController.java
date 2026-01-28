package com.example.pproject.subscription.controller;

import com.example.pproject.subscription.dto.SubscriptionBillingInfoUpdateRequest;
import com.example.pproject.subscription.dto.SubscriptionCreateRequest;
import com.example.pproject.subscription.dto.SubscriptionProductUpdateRequest;
import com.example.pproject.subscription.dto.SubscriptionResponse;
import com.example.pproject.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // =============================================================================================
    // [일반 유저 기능]
    // =============================================================================================

    /**
     * 구독 생성 (신청)
     */
    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(@RequestBody SubscriptionCreateRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 구독 상세 조회
     */
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable Long subscriptionId) {
        SubscriptionResponse response = subscriptionService.getSubscription(subscriptionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 기업의 구독 목록 조회
     */
    @GetMapping("/employer/{employerId}")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByEmployer(@PathVariable Long employerId) {
        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsByEmployer(employerId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 구독 취소 (해지 예약)
     */
    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 구독 재개 (결제 실패 등으로 중단된 경우)
     */
    @PostMapping("/{subscriptionId}/resume")
    public ResponseEntity<Void> resumeSubscription(@PathVariable Long subscriptionId) {
        subscriptionService.resumeSubscription(subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 결제 수단 변경/등록
     */
    @PutMapping("/{subscriptionId}/billing-info")
    public ResponseEntity<Void> updateBillingInfo(
            @PathVariable Long subscriptionId,
            @RequestBody SubscriptionBillingInfoUpdateRequest request) {
        subscriptionService.updateBillingInfo(subscriptionId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 구독 상품 변경 예약 (다음 결제일부터 적용)
     */
    @PostMapping("/{subscriptionId}/change-product")
    public ResponseEntity<Void> scheduleProductChange(
            @PathVariable Long subscriptionId,
            @RequestBody SubscriptionProductUpdateRequest request) {
        subscriptionService.scheduleProductChange(subscriptionId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 예약된 상품 변경 정보 조회
     */
    @GetMapping("/{subscriptionId}/scheduled-product-change")
    public ResponseEntity<SubscriptionResponse> getScheduledProductChange(@PathVariable Long subscriptionId) {
        SubscriptionResponse response = subscriptionService.getScheduledProductChange(subscriptionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 예약된 상품 변경 취소
     */
    @PostMapping("/{subscriptionId}/cancel-scheduled-product-change")
    public ResponseEntity<Void> cancelScheduledProductChange(@PathVariable Long subscriptionId) {
        subscriptionService.cancelScheduledProductChange(subscriptionId);
        return ResponseEntity.ok().build();
    }
}
