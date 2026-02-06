package com.example.pproject.subscription.controller;

import com.example.pproject.subscription.dto.SubscriptionBillingCycleResponse;
import com.example.pproject.subscription.service.SubscriptionBillingCycleService;
import lombok.RequiredArgsConstructor;
import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.subscription.dto.SubscriptionBillingCycleResponse;
import com.example.pproject.subscription.service.SubscriptionBillingCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-billing-cycles")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SubscriptionBillingCycleController {

    private final SubscriptionBillingCycleService billingCycleService;

    // =============================================================================================
    // [일반 유저 기능]
    // =============================================================================================

    /**
     * 내 구독의 결제/청구 이력 조회
     */
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<SubscriptionBillingCycleResponse>> getBillingCycles(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long subscriptionId) {
        List<SubscriptionBillingCycleResponse> responses = billingCycleService.getBillingCycles(principal.getUserid(),
                subscriptionId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 결제 주기 상세 조회
     */
    @GetMapping("/{cycleId}")
    public ResponseEntity<SubscriptionBillingCycleResponse> getBillingCycle(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long cycleId) {
        SubscriptionBillingCycleResponse response = billingCycleService.getBillingCycle(principal.getUserid(), cycleId);
        return ResponseEntity.ok(response);
    }
}
