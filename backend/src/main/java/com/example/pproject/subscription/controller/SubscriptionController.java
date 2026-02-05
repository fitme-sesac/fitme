package com.example.pproject.subscription.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.subscription.dto.SubscriptionBillingInfoUpdateRequest;
import com.example.pproject.subscription.dto.SubscriptionCreateRequest;
import com.example.pproject.subscription.dto.SubscriptionProductUpdateRequest;
import com.example.pproject.subscription.dto.SubscriptionResponse;
import com.example.pproject.subscription.repository.SubscriptionRepository;
import com.example.pproject.subscription.service.SubscriptionService;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final EmployerMemberRepository employerMemberRepository;

    // =============================================================================================
    // [구독 상태 확인]
    // =============================================================================================

    /**
     * 내 구독 상태 확인 (현재 로그인한 기업의 활성 구독 여부)
     */
    @GetMapping("/my/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkMySubscriptionStatus(@AuthenticationPrincipal JwtUserPrincipal principal) {
        try {
            Long employerId = getEmployerIdFromPrincipal(principal);

            if (employerId == null) {
                return ResponseEntity.ok(Map.of(
                        "hasActiveSubscription", false,
                        "message", "기업 정보를 찾을 수 없습니다."
                ));
            }

            Instant now = Instant.now();
            boolean hasActive = subscriptionRepository.hasActiveSubscription(employerId, SubscriptionStatus.ACTIVE, now);

            if (hasActive) {
                var subscription = subscriptionRepository.findActiveByEmployerId(employerId, SubscriptionStatus.ACTIVE, now);
                return ResponseEntity.ok(Map.of(
                        "hasActiveSubscription", true,
                        "subscription", subscription.map(s -> Map.of(
                                "id", s.getSubscriptionId(),
                                "status", s.getStatus().name(),
                                "nextBillingAt", s.getNextBillingAt() != null ? s.getNextBillingAt().toString() : null
                        )).orElse(null)
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "hasActiveSubscription", false,
                        "message", "활성 구독이 없습니다."
                ));
            }
        } catch (Exception e) {
            log.error("구독 상태 확인 중 오류", e);
            return ResponseEntity.ok(Map.of(
                    "hasActiveSubscription", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Principal에서 기업 ID 추출
     */
    private Long getEmployerIdFromPrincipal(JwtUserPrincipal principal) {
        if (principal == null) return null;

        try {
            String userid = principal.getUserid();
            String email = principal.getEmail();

            var userOpt = userid != null ? userRepository.findByUserid(userid) :
                         email != null ? userRepository.findByEmail(email) : null;

            if (userOpt == null || userOpt.isEmpty()) return null;

            Long memberId = userOpt.get().getId();
            var membership = employerMemberRepository.findFirstByMemberIdAndActiveTrue(memberId);

            return membership.map(EmployerMemberEntity::getEmployerId).orElse(null);
        } catch (Exception e) {
            log.warn("기업 ID 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    // =============================================================================================
    // [일반 유저 기능]
    // =============================================================================================

    /**
     * 구독 생성 (신청)
     */
    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody SubscriptionCreateRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(principal.getUserid(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 구독 상세 조회
     */
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long subscriptionId) {
        SubscriptionResponse response = subscriptionService.getSubscription(principal.getUserid(), subscriptionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 기업의 구독 목록 조회
     */
    @GetMapping("/employer/{employerId}")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByEmployer(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long employerId) {
        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsByEmployer(principal.getUserid(),
                employerId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 구독 취소 (해지 예약)
     */
    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancelSubscription(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long subscriptionId) {
        subscriptionService.cancelSubscription(principal.getUserid(), subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 구독 재개 (결제 실패 등으로 중단된 경우)
     */
    @PostMapping("/{subscriptionId}/resume")
    public ResponseEntity<Void> resumeSubscription(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long subscriptionId) {
        subscriptionService.resumeSubscription(principal.getUserid(), subscriptionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 결제 수단 변경/등록
     */
    @PutMapping("/{subscriptionId}/billing-info")
    public ResponseEntity<Void> updateBillingInfo(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long subscriptionId,
            @RequestBody SubscriptionBillingInfoUpdateRequest request) {
        subscriptionService.updateBillingInfo(principal.getUserid(), subscriptionId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 구독 상품 변경 예약 (다음 결제일부터 적용)
     */
    @PostMapping("/{subscriptionId}/change-product")
    public ResponseEntity<Void> scheduleProductChange(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long subscriptionId,
            @RequestBody SubscriptionProductUpdateRequest request) {
        subscriptionService.scheduleProductChange(principal.getUserid(), subscriptionId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 예약된 상품 변경 정보 조회
     */
    @GetMapping("/{subscriptionId}/scheduled-product-change")
    public ResponseEntity<SubscriptionResponse> getScheduledProductChange(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long subscriptionId) {
        SubscriptionResponse response = subscriptionService.getScheduledProductChange(principal.getUserid(),
                subscriptionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 예약된 상품 변경 취소
     */
    @PostMapping("/{subscriptionId}/cancel-scheduled-product-change")
    public ResponseEntity<Void> cancelScheduledProductChange(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long subscriptionId) {
        subscriptionService.cancelScheduledProductChange(principal.getUserid(), subscriptionId);
        return ResponseEntity.ok().build();
    }
}
