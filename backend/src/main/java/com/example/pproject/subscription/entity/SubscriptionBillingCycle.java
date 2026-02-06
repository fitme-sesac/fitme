package com.example.pproject.subscription.entity;

import com.example.pproject.Constant.CreditStatus;
import com.example.pproject.Constant.PaymentStatus;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.wallet.entity.WalletLedger;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "subscription_billing_cycle")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SubscriptionBillingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cycle_id")
    private Long subscriptionBillingCycleId;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "billing_month")
    private LocalDate billingMonth;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "credit_grant")
    private Integer creditGrant;

    @ManyToOne
    @JoinColumn(name = "ledger_id")
    private WalletLedger walletLedger;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_status")
    private CreditStatus creditStatus;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    // ===========================================
    // 정적 팩토리 메서드 (Static Factory Method)
    // ===========================================

    /**
     * 빌링 사이클 생성
     *
     * @param subscription 소속 구독 (필수)
     * @param billingMonth 결제 월 (필수)
     * @return 새로운 빌링 사이클
     */
    public static SubscriptionBillingCycle create(Subscription subscription, LocalDate billingMonth) {
        if (subscription == null) {
            throw new IllegalArgumentException("구독 정보는 필수입니다.");
        }
        if (billingMonth == null) {
            throw new IllegalArgumentException("결제 월은 필수입니다.");
        }
        SubscriptionBillingCycle cycle = new SubscriptionBillingCycle();
        cycle.subscription = subscription;
        cycle.billingMonth = billingMonth;
        cycle.paymentStatus = PaymentStatus.SCHEDULED;
        cycle.creditStatus = CreditStatus.PENDING;
        cycle.creditGrant = 0; // Not Null Constraint 준수
        cycle.createdAt = Instant.now();
        return cycle;
    }

    // ===========================================
    // 헬퍼 메서드 (Private Helpers)
    // ===========================================

    /**
     * 크레딧 금액 유효성 검증
     */
    private void validateCreditAmount(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("크레딧은 0보다 커야 합니다.");
        }
    }

    // ===========================================
    // 자기검증 로직 (Self-Validation)
    // ===========================================

    /**
     * 크레딧 지급량 검증
     * creditGrant가 양수인지 확인
     */
    public void validateCreditGrant() {
        validateCreditAmount(this.creditGrant);
    }

    /**
     * 상태 정합성 검증
     * paymentStatus가 PAID일 때만 creditStatus가 GRANTED 가능
     */
    public void validateStatusConsistency() {
        if (this.creditStatus == CreditStatus.GRANTED) {
            if (this.paymentStatus != PaymentStatus.PAID) {
                throw new IllegalStateException("결제가 성공해야만 크레딧을 지급할 수 있습니다.");
            }
        }
    }

    // ===========================================
    // 상태 전이 가능 여부 확인 (State Transition Guards)
    // ===========================================

    /**
     * 결제 성공 처리 가능 여부 확인
     */
    public boolean canMarkPaymentSuccess() {
        return this.paymentStatus == PaymentStatus.SCHEDULED;
    }

    /**
     * 결제 실패 처리 가능 여부 확인
     */
    public boolean canMarkPaymentFailed() {
        return this.paymentStatus == PaymentStatus.SCHEDULED;
    }

    /**
     * 크레딧 지급 가능 여부 확인
     */
    public boolean canGrantCredit() {
        return this.paymentStatus == PaymentStatus.PAID
                && this.creditStatus == CreditStatus.PENDING;
    }

    /**
     * 크레딧 회수 가능 여부 확인
     */
    public boolean canRevokeCredit() {
        return this.creditStatus == CreditStatus.GRANTED;
    }

    // ===========================================
    // 비즈니스 로직 (Business Logic)
    // ===========================================

    /**
     * 결제 성공 처리
     *
     * @param payment 결제 정보
     */
    public void markPaymentSuccess(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("결제 정보가 없습니다.");
        }
        // 멱등성: 이미 성공 상태면 무시
        if (this.paymentStatus == PaymentStatus.PAID) {
            return;
        }
        if (!canMarkPaymentSuccess()) {
            throw new IllegalStateException("결제 성공 처리를 할 수 없는 상태입니다.");
        }
        this.payment = payment;
        this.paymentStatus = PaymentStatus.PAID;
    }

    /**
     * 결제 실패 처리
     */
    public void markPaymentFailed() {
        // 멱등성: 이미 실패 상태면 무시
        if (this.paymentStatus == PaymentStatus.FAILED) {
            return;
        }
        if (!canMarkPaymentFailed()) {
            throw new IllegalStateException("결제 실패 처리를 할 수 없는 상태입니다.");
        }
        this.paymentStatus = PaymentStatus.FAILED;
        this.creditStatus = CreditStatus.FAILED;
    }

    /**
     * 크레딧 지급
     *
     * @param creditAmount 지급할 크레딧 양
     * @param walletLedger 지갑 원장
     */
    public void grantCredit(Integer creditAmount, WalletLedger walletLedger) {
        // 멱등성: 이미 지급 완료면 무시
        if (this.creditStatus == CreditStatus.GRANTED) {
            return;
        }
        if (!canGrantCredit()) {
            throw new IllegalStateException("크레딧을 지급할 수 없는 상태입니다.");
        }
        validateCreditAmount(creditAmount);
        this.creditGrant = creditAmount;
        this.walletLedger = walletLedger;
        this.creditStatus = CreditStatus.GRANTED;
    }

    /**
     * 이미 지급된 크레딧 정보 기록 (시스템 동기화용)
     */
    public void recordGrantedCredit(Integer creditAmount) {
        if (creditAmount != null) {
            this.creditGrant = creditAmount;
        }
    }

    /**
     * 크레딧 회수 (환불/취소 시)
     */
    public void revokeCredit() {
        // 멱등성: 이미 회수된 상태면 무시
        if (this.creditStatus == CreditStatus.REVOKED) {
            return;
        }
        if (!canRevokeCredit()) {
            throw new IllegalStateException("지급된 크레딧만 회수할 수 있습니다.");
        }
        this.creditStatus = CreditStatus.REVOKED;
    }

    /**
     * 결제 상태 변경 (관리자용)
     */
    public void updatePaymentStatus(PaymentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("변경할 결제 상태는 필수입니다.");
        }
        this.paymentStatus = newStatus;
    }

    /**
     * 크레딧 상태 변경 (관리자용)
     */
    public void updateCreditStatus(CreditStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("변경할 크레딧 상태는 필수입니다.");
        }
        this.creditStatus = newStatus;
    }

    // ===========================================
    // 상태 확인 메서드 (Query Methods)
    // ===========================================

    /**
     * 결제 완료 여부 확인
     */
    public boolean isPaymentCompleted() {
        return this.paymentStatus == PaymentStatus.PAID;
    }

    /**
     * 크레딧 지급 여부 확인
     */
    public boolean isCreditGranted() {
        return this.creditStatus == CreditStatus.GRANTED;
    }

    /**
     * 전체 처리 완료 여부 확인 (결제 + 크레딧 지급)
     */
    public boolean isProcessComplete() {
        return isPaymentCompleted() && isCreditGranted();
    }
}
