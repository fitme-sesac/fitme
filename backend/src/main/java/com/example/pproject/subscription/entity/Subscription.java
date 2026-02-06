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
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Subscription extends BaseTimeEntity {

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

    @ManyToOne
    @JoinColumn(name = "next_product_id")
    private Product nextProduct;

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
    // 정적 팩토리 메서드 (Static Factory Method)
    // ===========================================

    /**
     * 구독 생성
     *
     * @param employer    고용주 (필수)
     * @param product     상품 (필수)
     * @param customerKey 고객 키 (필수)
     * @param billingKey  빌링 키 (필수)
     * @return 새로운 구독 (ACTIVE 상태)
     */
    public static Subscription create(EmployerEntity employer, Product product,
            String customerKey, String billingKey) {
        if (employer == null) {
            throw new IllegalArgumentException("고용주 정보는 필수입니다.");
        }
        if (product == null) {
            throw new IllegalArgumentException("상품 정보는 필수입니다.");
        }
        if (customerKey == null || customerKey.isBlank()) {
            throw new IllegalArgumentException("고객키는 필수입니다.");
        }
        if (billingKey == null || billingKey.isBlank()) {
            throw new IllegalArgumentException("빌링키는 필수입니다.");
        }

        Subscription subscription = new Subscription();
        subscription.employer = employer;
        subscription.product = product;
        subscription.customerKey = customerKey;
        subscription.billingKey = billingKey;
        subscription.status = SubscriptionStatus.ACTIVE;
        subscription.startedAt = Instant.now();
        // 다음 결제일: 현재로부터 1개월 뒤 (UTC 기준)
        subscription.nextBillingAt = ZonedDateTime.now(ZoneId.of("UTC")).plusMonths(1).toInstant();
        return subscription;
    }

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
        // 멱등성: 이미 활성 상태면 무시
        if (this.status == SubscriptionStatus.ACTIVE) {
            return;
        }
        if (!canActivate()) {
            throw new IllegalStateException("구독을 활성화할 수 없습니다. 빌링 정보가 없습니다.");
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = Instant.now();
        this.endedAt = null;
    }

    /**
     * 결제 실패로 인한 구독 중단
     */
    public void markPaymentFailed() {
        // 멱등성: 이미 결제 실패 상태면 무시
        if (this.status == SubscriptionStatus.PAYMENT_FAILED) {
            return;
        }
        if (!canMarkPaymentFailed()) {
            throw new IllegalStateException("활성 상태의 구독만 결제 실패 처리할 수 있습니다.");
        }
        this.status = SubscriptionStatus.PAYMENT_FAILED;
    }

    /**
     * 구독 취소 (즉시 취소) - 관리자용 혹은 특수 상황
     */
    public void cancel() {
        // 멱등성: 이미 취소 상태면 무시
        if (this.status == SubscriptionStatus.CANCELED) {
            return;
        }
        this.status = SubscriptionStatus.CANCELED;
        this.endedAt = Instant.now();
    }

    /**
     * 구독 해지 예약 (다음 결제일에 종료)
     */
    public void scheduleCancellation(Instant endDate) {
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태의 구독만 해지 예약할 수 있습니다.");
        }
        this.endedAt = endDate;
    }

    /**
     * 구독 해지 예약 취소 (구독 유지)
     */
    public void revokeCancellation() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태의 구독만 해지 예약을 취소할 수 있습니다.");
        }
        this.endedAt = null;
    }

    /**
     * 구독 재개 (결제 실패 상태에서 복구)
     * - 재개 시, 다음 결제일은 재개일로부터 1개월 후로 재설정됩니다.
     */
    public void resume() {
        // 멱등성: 이미 활성 상태면 무시
        if (this.status == SubscriptionStatus.ACTIVE) {
            return;
        }
        if (!canResume()) {
            throw new IllegalStateException("재개할 수 없습니다. 결제 실패 상태가 아니거나 빌링 정보가 없습니다.");
        }
        this.status = SubscriptionStatus.ACTIVE;
        // 다음 결제일을 재개한 날짜 기준으로 1개월 후로 재설정 (UTC 기준)
        this.nextBillingAt = ZonedDateTime.now(ZoneId.of("UTC")).plusMonths(1).toInstant();
    }

    /**
     * 빌링 정보 업데이트
     *
     * @param billingKey  빌링 키
     * @param customerKey 고객 키
     * @param cardCompany 카드사
     * @param cardNumber  카드번호
     */
    public void updateBillingInfo(String billingKey, String customerKey,
            String cardCompany, String cardNumber) {
        if (billingKey == null || billingKey.isBlank()) {
            throw new IllegalArgumentException("빌링키는 필수입니다.");
        }
        if (customerKey == null || customerKey.isBlank()) {
            throw new IllegalArgumentException("고객키는 필수입니다.");
        }
        this.billingKey = billingKey;
        this.customerKey = customerKey;
        this.cardCompany = cardCompany;
        this.cardNumber = cardNumber;
    }

    /**
     * 다음 결제일 설정 (1개월 후)
     */
    public void scheduleNextBilling() {
        Instant baseTime = (this.nextBillingAt != null) ? this.nextBillingAt : Instant.now();
        // UTC 기준으로 1개월 더하기
        this.nextBillingAt = baseTime.atZone(ZoneId.of("UTC")).plusMonths(1).toInstant();
    }

    /**
     * 구독 상품 변경 예약
     * (즉시 변경되지 않고, 다음 결제일에 반영됨)
     */
    public void scheduleProductChange(Product nextProduct) {
        if (nextProduct == null) {
            throw new IllegalArgumentException("변경할 상품 정보가 없습니다.");
        }
        // 현재 상품과 동일하면 예약 취소 혹은 무시
        if (this.product.equals(nextProduct)) {
            this.nextProduct = null;
            return;
        }
        this.nextProduct = nextProduct;
    }

    /**
     * 예약된 상품 변경 적용 (배치/스케줄러가 결제 시점에 호출)
     */
    public void applyScheduledProductChange() {
        if (this.nextProduct != null) {
            this.product = this.nextProduct;
            this.nextProduct = null; // 예약 내용 초기화
        }
    }

    /**
     * 예약된 상품 변경 취소
     */
    public void cancelScheduledProductChange() {
        this.nextProduct = null;
    }

    /**
     * 구독 상태 변경 (관리자용)
     */
    public void updateStatus(SubscriptionStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("변경할 상태는 필수입니다.");
        }
        this.status = newStatus;
    }

    // ===========================================
    // 상태 확인 메서드 (Query Methods)
    // ===========================================

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
     * 결제 준비 완료 여부 확인 (Batch Reader용)
     * 결제 가능 + 결제일 도래
     *
     * @param now 현재 시간
     */
    public boolean isReadyForBilling(Instant now) {
        return isBillable() && isDueToBill(now);
    }

    /**
     * 상품 변경 예약 여부 확인
     */
    public boolean isChangeScheduled() {
        return this.nextProduct != null;
    }

}
