package com.example.pproject.payment.entity;

import com.example.pproject.Constant.PaymentAppStatus;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import com.example.pproject.order.entity.Orders;
import com.example.pproject.payment.dto.toss.TossPaymentResponse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 결제 정보 엔티티.
 * <p>
 * 결제 요청부터 승인, 취소까지의 전체 생명주기를 관리합니다.
 * 토스 페이먼츠 연동을 위한 핵심 데이터(paymentKey, status 등)를 포함합니다.
 * </p>
 */
@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_payment_uid", columnNames = "payment_uid"),
                @UniqueConstraint(name = "uq_payment_pg_payment_key", columnNames = "pg_payment_key")
        },
        indexes = {
                @Index(name = "idx_payment_order_id", columnList = "order_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_uid", nullable = false, updatable = false)
    private UUID paymentUid;

    // Orders와 연관관계 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method; // CARD, VIRTUAL_ACCOUNT, TRANSFER 등

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 3))
    })
    private Money paidAmount;

    // === 상태 관리 ===

    // 내부(App) 관리 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "app_status", nullable = false, length = 30)
    private PaymentAppStatus appStatus;

    // PG(토스) 원문 상태 (READY, IN_PROGRESS, DONE, CANCELED 등)
    @Column(name = "pg_status", length = 40)
    private String pgStatus;

    // === 토스 페이먼츠 핵심 데이터 ===

    // 토스 결제 고유 키 (취소/조회시 필수)
    @Column(name = "pg_payment_key", length = 100)
    private String pgPaymentKey;

    @Column(name = "pg_approval_key", length = 80)
    private String pgApprovalKey; // 승인 요청용 키 (필요 시)

    @Column(name = "pg_transaction_id", length = 80)
    private String pgTransactionId;

    // 토스 응답 전체 JSON 저장 (디버깅 및 이력용)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pg_payload", columnDefinition = "jsonb")
    private Map<String, Object> pgPayload;

    // === 시간 정보 ===

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Builder
    public Payment(Orders order, PaymentMethod method, Money paidAmount) {
        this.paymentUid = UUID.randomUUID();
        this.order = order;
        this.method = method;
        this.paidAmount = paidAmount;
        this.appStatus = PaymentAppStatus.REQUESTED; // 초기 상태: REQUESTED
    }

    // === 비즈니스 로직 ===

    /**
     * 1. 인증 완료 (Confirm 단계 진입)
     * <p>
     * 클라이언트가 토스 인증을 마치고 돌아왔을 때 호출합니다.
     * paymentKey를 저장하지만, 아직 최종 승인 전이므로 상태는 REQUESTED를 유지합니다.
     * </p>
     * @param pgPaymentKey 토스에서 발급받은 결제 키
     * @throws IllegalStateException 결제 요청(REQUESTED) 상태가 아닐 경우
     */
    public void confirm(String pgPaymentKey) {
        verifyConfirmableState();
        this.pgPaymentKey = pgPaymentKey;
    }

    /**
     * 2. 승인 완료 (Approve)
     * <p>
     * 토스 승인 API 호출 성공 후 호출합니다.
     * 상태를 APPROVED로 변경하고, 토스 응답 데이터를 저장합니다.
     * 요청 금액과 승인 금액이 일치하는지 검증합니다.
     * </p>
     * @param response 토스 승인 API 응답 DTO
     * @param rawPayload 토스 응답 원본 Map (저장용)
     * @throws IllegalStateException 요청 금액과 승인 금액이 일치하지 않을 경우
     */
    public void approve(TossPaymentResponse response, Map<String, Object> rawPayload) {
        verifyApprovableState();
        validateAmount(Money.wons(response.totalAmount()));

        this.appStatus = PaymentAppStatus.APPROVED;
        this.pgStatus = response.status();
        this.pgPaymentKey = response.paymentKey();
        this.pgTransactionId = response.transactionKey();
        this.pgPayload = rawPayload;
        this.approvedAt = parseDateTime(response.approvedAt());
    }

    /**
     * 3. 결제 취소 (Cancel)
     * <p>
     * 토스 취소 API 호출 성공 후 호출합니다.
     * 상태를 CANCELED로 변경하고, 취소 일시를 기록합니다.
     * </p>
     * @param response 토스 취소 API 응답 DTO
     * @param rawPayload 토스 응답 원본 Map (저장용)
     */
    public void cancel(TossPaymentResponse response, Map<String, Object> rawPayload) {
        verifyCancelableState();

        this.appStatus = PaymentAppStatus.CANCELED; // 부분 취소 고려 시 로직 추가 필요
        this.pgStatus = response.status();
        this.pgPayload = rawPayload;
        this.canceledAt = Instant.now();
    }

    /**
     * 결제 취소 이력 생성
     */
    public PaymentCancel createCancel(TossPaymentResponse tossResponse, String cancelReason, Money cancelAmount, String idempotencyKey, Map<String, Object> rawPayload) {
        return PaymentCancel.builder()
                .payment(this)
                .tossTransactionKey(tossResponse.transactionKey())
                .cancelStatus("DONE")
                .cancelAmount(cancelAmount)
                .cancelReason(cancelReason)
                .idempotencyKey(idempotencyKey)
                .rawCancel(rawPayload)
                .build();
    }

    /**
     * 결제 금액 검증
     * @param amount 검증할 금액
     * @throws IllegalStateException 금액이 일치하지 않을 경우
     */
    public void validateAmount(Money amount) {
        if (amount != null && !this.paidAmount.equals(amount)) {
            throw new IllegalStateException("결제 금액 불일치: 요청된 금액(" + amount + ")과 실제 결제 금액(" + this.paidAmount + ")이 다릅니다.");
        }
    }
    
    public String getOrderName() {
        if (this.order != null && this.order.getProduct() != null) {
            return this.order.getProduct().getName();
        }
        return "상품 정보 없음";
    }

    // === 내부 검증 로직 ===

    private void verifyConfirmableState() {
        if (this.appStatus != PaymentAppStatus.REQUESTED) {
            throw new IllegalStateException("결제 요청(REQUESTED) 상태에서만 승인 요청을 진행할 수 있습니다.");
        }
    }

    private void verifyApprovableState() {
        if (!this.appStatus.canTransitionTo(PaymentAppStatus.APPROVED)) {
            throw new IllegalStateException("현재 상태에서는 승인 완료(APPROVED)로 변경할 수 없습니다.");
        }
    }

    private void verifyCancelableState() {
        if (!this.appStatus.canTransitionTo(PaymentAppStatus.CANCELED)) {
            throw new IllegalStateException("현재 상태에서는 취소(CANCELED)로 변경할 수 없습니다.");
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
