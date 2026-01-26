package com.example.pproject.subscription.service;

import com.example.pproject.payment.entity.Payment;
import com.example.pproject.subscription.dto.SubscriptionBillingCycleResponse;
import com.example.pproject.subscription.entity.Subscription;
import com.example.pproject.subscription.entity.SubscriptionBillingCycle;
import com.example.pproject.subscription.repository.SubscriptionBillingCycleRepository;
import com.example.pproject.subscription.repository.SubscriptionRepository;
import com.example.pproject.wallet.entity.WalletLedger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionBillingCycleService {

    private final SubscriptionBillingCycleRepository billingCycleRepository;
    private final SubscriptionRepository subscriptionRepository;

    // =============================================================================================
    // [일반 유저 기능] - 결제 내역 조회 등
    // =============================================================================================

    /**
     * [일반 유저] 내 구독의 결제/청구 이력 조회
     */
    public List<SubscriptionBillingCycleResponse> getBillingCycles(Long subscriptionId) {
        return billingCycleRepository.findAllBySubscription_SubscriptionId(subscriptionId).stream()
                .map(SubscriptionBillingCycleResponse::from)
                .collect(Collectors.toList());
    }

    // =============================================================================================
    // [관리자/시스템 기능] - 결제 생성, 성공/실패 처리, 크레딧 지급/회수 (주로 배치 작업)
    // =============================================================================================

    /**
     * [시스템/배치] 월별 빌링 사이클 생성 (청구서 생성)
     */
    @Transactional
    public SubscriptionBillingCycleResponse createBillingCycle(Long subscriptionId, LocalDate billingMonth) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독입니다."));

        SubscriptionBillingCycle cycle = SubscriptionBillingCycle.create(subscription, billingMonth);
        
        // 생성 시점 검증
        cycle.validateStatusConsistency();

        SubscriptionBillingCycle saved = billingCycleRepository.save(cycle);
        return SubscriptionBillingCycleResponse.from(saved);
    }

    /**
     * [시스템/배치] 결제 성공 처리 (PG사 콜백 또는 배치 결과)
     */
    @Transactional
    public void markPaymentSuccess(Long cycleId, Payment payment) {
        SubscriptionBillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 빌링 사이클입니다."));

        cycle.markPaymentSuccess(payment);
    }

    /**
     * [시스템/배치] 결제 실패 처리
     */
    @Transactional
    public void markPaymentFailed(Long cycleId) {
        SubscriptionBillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 빌링 사이클입니다."));

        cycle.markPaymentFailed();
    }

    /**
     * [시스템/배치] 크레딧 지급 (결제 성공 후)
     */
    @Transactional
    public void grantCredit(Long cycleId, Integer creditAmount, WalletLedger walletLedger) {
        SubscriptionBillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 빌링 사이클입니다."));

        // 상태 정합성 검증 (결제 성공 상태여야 함)
        cycle.validateStatusConsistency();
        
        // 크레딧 지급량 검증 (엔티티 내부 로직 활용)
        cycle.grantCredit(creditAmount, walletLedger);
        
        // 지급 후 상태 검증 (선택적)
        cycle.validateCreditGrant();
    }

    /**
     * [관리자/시스템] 크레딧 회수 (환불 처리 시)
     */
    @Transactional
    public void revokeCredit(Long cycleId) {
        SubscriptionBillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 빌링 사이클입니다."));

        cycle.revokeCredit();
    }
}
