package com.example.pproject.subscription.service;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.CreditStatus;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.Constant.PaymentStatus;
import com.example.pproject.payment.dto.request.PaymentCreateRequest;
import com.example.pproject.payment.dto.response.PaymentResponse;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.payment.repository.PaymentRepository;
import com.example.pproject.payment.service.PaymentService;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionBillingCycleService {

    private final SubscriptionBillingCycleRepository billingCycleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    // =============================================================================================
    // [일반 유저 기능]
    // =============================================================================================

    /**
     * [일반 유저] 내 구독의 결제/청구 이력 조회
     */
    public List<SubscriptionBillingCycleResponse> getBillingCycles(Long subscriptionId) {
        return billingCycleRepository.findAllBySubscription_SubscriptionId(subscriptionId).stream()
                .map(SubscriptionBillingCycleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * [일반 유저] 특정 결제 주기 상세 조회
     */
    public SubscriptionBillingCycleResponse getBillingCycle(Long cycleId) {
        SubscriptionBillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 빌링 사이클입니다."));
        return SubscriptionBillingCycleResponse.from(cycle);
    }

    // =============================================================================================
    // [관리자/시스템 기능]
    // =============================================================================================

    /**
     * [관리자] 모든 결제 주기 목록 조회
     */
    public List<SubscriptionBillingCycleResponse> getAllBillingCycles() {
        return billingCycleRepository.findAll().stream()
                .map(SubscriptionBillingCycleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * [관리자] 결제 상태 강제 변경
     */
    @Transactional
    public void updatePaymentStatus(Long cycleId, PaymentStatus newStatus) {
        SubscriptionBillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 빌링 사이클입니다."));
        cycle.updatePaymentStatus(newStatus);
    }

    /**
     * [관리자] 크레딧 상태 강제 변경
     */
    @Transactional
    public void updateCreditStatus(Long cycleId, CreditStatus newStatus) {
        SubscriptionBillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 빌링 사이클입니다."));
        cycle.updateCreditStatus(newStatus);
    }

    /**
     * [시스템/배치] 월별 정기 결제 처리 (결제 + 사이클 생성 + 크레딧 지급)
     */
    @Transactional
    public void processMonthlyPayment(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보가 없습니다."));

        // 1. 결제 요청 DTO 생성
        PaymentCreateRequest request = new PaymentCreateRequest(
                subscription.getProduct().getPrice().getAmount(), // amount (BigDecimal)
                "구독 정기 결제", // orderName
                PaymentMethod.CARD, // method
                BuyerType.EMPLOYER, // buyerType
                subscription.getProduct().getProductCode(), // productCode
                UUID.randomUUID().toString() // idempotencyKey
        );

        // 2. 빌링키 결제 시도 (PaymentService)
        // payWithBillingKey 내부에서 PG사 결제 및 Wallet 크레딧 지급까지 완료됨
        PaymentResponse paymentResponse = paymentService.payWithBillingKey(
                subscription.getEmployer().getId(),
                subscription.getBillingKey(),
                request
        );

        // 3. 빌링 사이클 생성
        SubscriptionBillingCycle cycle = SubscriptionBillingCycle.create(subscription, LocalDate.now());

        // 4. Payment 엔티티 조회 및 연결
        Payment payment = paymentRepository.findById(paymentResponse.paymentId())
                .orElseThrow(() -> new IllegalStateException("결제 정보가 생성되지 않았습니다."));
        
        cycle.markPaymentSuccess(payment);

        // 5. 크레딧 상태 업데이트 (이미 지급되었으므로 상태만 변경)
        cycle.updateCreditStatus(CreditStatus.GRANTED);

        billingCycleRepository.save(cycle);
    }

    /**
     * [시스템/배치] 월별 빌링 사이클 생성 (청구서 생성) - 수동/테스트용
     */
    @Transactional
    public SubscriptionBillingCycleResponse createBillingCycle(Long subscriptionId, LocalDate billingMonth) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독입니다."));

        SubscriptionBillingCycle cycle = SubscriptionBillingCycle.create(subscription, billingMonth);
        
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

        cycle.validateStatusConsistency();
        
        cycle.grantCredit(creditAmount, walletLedger);
        
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
