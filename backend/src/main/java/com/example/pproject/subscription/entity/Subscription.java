package com.example.pproject.subscription.entity;

import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Subscription extends BaseTimeEntity {

    private static final int BILLING_CYCLE_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @ManyToOne
    @JoinColumn(name = "employer_id")
    private EmployerEntity employer;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "next_billing_at")
    private Instant nextBillingAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "customer_key")
    private String customerKey;

    @Column(name = "billing_key")
    private String billingKey;

    @Column(name = "card_company")
    private String cardCompany;

    @Column(name = "card_number")
    private String cardNumber;

    // ===========================================
    // 헬퍼 메서드 (Private Helpers)
    // ===========================================

    /**
     * 빌링 정보 존재 여부 확인
     */
    private boolean hasBillingInfo() {
        return this.billingKey != null && !this.billingKey.isBlank()
                && this.customerKey != null && !this.customerKey.isBlank();
    }

    /**
     * 카드 정보 존재 여부 확인
     */
    private boolean hasCardInfo() {
        return this.cardCompany != null && !this.cardCompany.isBlank()
                && this.cardNumber != null && !this.cardNumber.isBlank();
    }

    // ===========================================
    // 자기검증 로직 (Self-Validation)
    // ===========================================

    /**
     * 빌링 정보 검증
     * billingKey와 customerKey가 존재하는지 확인
     */
    public void validateBillingInfo() {
        if (!hasBillingInfo()) {
            throw new IllegalStateException("빌링키 또는 고객키가 등록되지 않았습니다.");
        }
    }

    /**
     * 날짜 정합성 검증
     * startedAt < nextBillingAt, endedAt가 있으면 startedAt < endedAt
     */
    public void validateDateConsistency() {
        if (this.startedAt != null && this.nextBillingAt != null) {
            if (this.startedAt.isAfter(this.nextBillingAt)) {
                throw new IllegalStateException("시작일이 다음 결제일보다 늦을 수 없습니다.");
            }
        }
        if (this.startedAt != null && this.endedAt != null) {
            if (this.startedAt.isAfter(this.endedAt)) {
                throw new IllegalStateException("시작일이 종료일보다 늦을 수 없습니다.");
            }
        }
    }

    /**
     * 카드 정보 검증
     * cardCompany와 cardNumber가 존재하는지 확인
     */
    public void validateCardInfo() {
        if (!hasCardInfo()) {
            throw new IllegalStateException("카드사 또는 카드번호 정보가 없습니다.");
        }
    }

    /**
     * 결제 가능 상태 종합 검증
     * 빌링 정보 + 카드 정보 + 활성 상태 확인
     */
    public void validateBillableState() {
        validateBillingInfo();
        validateCardInfo();
        if (!isActive()) {
            throw new IllegalStateException("활성 상태의 구독만 결제할 수 있습니다.");
        }
    }

    // ===========================================
    // 상태 전이 가능 여부 확인 (State Transition Guards)
    // ===========================================

    /**
     * 활성화 가능 여부 확인
     */
    public boolean canActivate() {
        return this.status != SubscriptionStatus.ACTIVE && hasBillingInfo();
    }

    /**
     * 결제 실패 처리 가능 여부 확인
     */
    public boolean canMarkPaymentFailed() {
        return this.status == SubscriptionStatus.ACTIVE;
    }

    /**
     * 취소 가능 여부 확인
     */
    public boolean canCancel() {
        return this.status != SubscriptionStatus.CANCELED;
    }

    /**
     * 재개 가능 여부 확인
     */
    public boolean canResume() {
        return this.status == SubscriptionStatus.PAYMENT_FAILED && hasBillingInfo();
    }

    // ===========================================
    // 비즈니스 로직 (Business Logic)
    // ===========================================

    /**
     * 구독 활성화
     */
    public void activate() {
        if (!canActivate()) {
            throw new IllegalStateException("구독을 활성화할 수 없습니다. 이미 활성 상태이거나 빌링 정보가 없습니다.");
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = Instant.now();
        this.endedAt = null;
    }

    /**
     * 결제 실패로 인한 구독 중단
     */
    public void markPaymentFailed() {
        if (!canMarkPaymentFailed()) {
            throw new IllegalStateException("활성 상태의 구독만 결제 실패 처리할 수 있습니다.");
        }
        this.status = SubscriptionStatus.PAYMENT_FAILED;
    }

    /**
     * 구독 취소
     */
    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("이미 취소된 구독입니다.");
        }
        this.status = SubscriptionStatus.CANCELED;
        this.endedAt = Instant.now();
    }

    /**
     * 구독 재개 (결제 실패 상태에서 복구)
     */
    public void resume() {
        if (!canResume()) {
            throw new IllegalStateException("재개할 수 없습니다. 결제 실패 상태가 아니거나 빌링 정보가 없습니다.");
        }
        this.status = SubscriptionStatus.ACTIVE;
    }

    /**
     * 결제 대상인지 확인
     * 활성 상태이고 빌링 정보가 유효한 경우
     */
    public boolean isBillable() {
        return isActive() && hasBillingInfo();
    }

    /**
     * 결제일 도래 여부 확인
     *
     * @param now 현재 시간
     */
    public boolean isDueToBill(Instant now) {
        if (this.nextBillingAt == null) {
            return false;
        }
        return !now.isBefore(this.nextBillingAt);
    }

    /**
     * 다음 결제일 설정 (빌링 주기 후)
     */
    public void scheduleNextBilling() {
        this.nextBillingAt = Instant.now().plus(BILLING_CYCLE_DAYS, ChronoUnit.DAYS);
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE;
    }

    /**
     * 만료되었는지 확인
     */
    public boolean isExpired() {
        if (this.endedAt == null) {
            return false;
        }
        return Instant.now().isAfter(this.endedAt);
    }

}
