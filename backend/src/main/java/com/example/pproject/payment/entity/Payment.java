package com.example.pproject.payment.entity;

import com.example.pproject.Constant.PaymentAppStatus;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import com.example.pproject.payment.dto.toss.TossPaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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

    // Order와 연관관계 (N:1) - 추후 Order 엔티티 구현 시 주석 해제
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id", nullable = false)
//    private Order order;

    // 임시 필드 (Order 엔티티 없을 때 사용)
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "order_name")
    private String orderName;

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
    private LocalDateTime canceledAt;

    @Builder
    public Payment(String orderId, String orderName, PaymentMethod method, Money paidAmount) {
        this.paymentUid = UUID.randomUUID();
        this.orderId = orderId;
        this.orderName = orderName;
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
        if (this.appStatus != PaymentAppStatus.REQUESTED) {
            throw new IllegalStateException("결제 요청(REQUESTED) 상태에서만 승인 요청을 진행할 수 있습니다.");
        }
        this.pgPaymentKey = pgPaymentKey;
        // 상태 변경 없음 (여전히 요청 중)
    }

    /**
     * 2. 승인 완료 (Approve)
     * <p>
     * 토스 승인 API 호출 성공 후 호출합니다.
     * 상태를 APPROVED로 변경하고, 토스 응답 데이터를 저장합니다.
     * 요청 금액과 승인 금액이 일치하는지 검증합니다.
     * </p>
     * @param response 토스 승인 API 응답 DTO
     * @throws IllegalStateException 요청 금액과 승인 금액이 일치하지 않을 경우
     */
    public void approve(TossPaymentResponse response) {
        // 금액 검증 (요청 금액과 승인 금액 일치 여부)
        if (this.paidAmount.getAmount().compareTo(response.totalAmount()) != 0) {
            throw new IllegalStateException("요청 금액과 승인 금액이 일치하지 않습니다.");
        }

        this.appStatus = PaymentAppStatus.APPROVED; // DONE -> APPROVED
        this.pgStatus = response.status();
        this.pgPaymentKey = response.paymentKey();
        this.pgTransactionId = response.transactionKey(); // 추가됨
        
        if (response.approvedAt() != null) {
            this.approvedAt = OffsetDateTime.parse(response.approvedAt()).toLocalDateTime();
        }
    }
    
    /**
     * 2-1. 승인 완료 (Map Payload 버전)
     * <p>
     * DTO 대신 원본 Map 데이터를 사용하여 승인 처리를 합니다.
     * 전체 응답 JSON(pgPayload)을 저장할 때 유용합니다.
     * </p>
     * @param pgPayload 토스 승인 API 응답 Map
     */
    public void approve(Map<String, Object> pgPayload) {
        this.appStatus = PaymentAppStatus.APPROVED; // DONE -> APPROVED
        this.pgStatus = (String) pgPayload.get("status");
        this.pgTransactionId = (String) pgPayload.get("transactionKey"); // 추가됨
        this.pgPayload = pgPayload;
        
        String approvedAtStr = (String) pgPayload.get("approvedAt");
        if (approvedAtStr != null) {
            this.approvedAt = LocalDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    /**
     * 3. 결제 취소 (Cancel)
     * <p>
     * 토스 취소 API 호출 성공 후 호출합니다.
     * 상태를 CANCELED로 변경하고, 취소 일시를 기록합니다.
     * </p>
     * @param pgPayload 토스 취소 API 응답 Map
     */
    public void cancel(Map<String, Object> pgPayload) {
        this.appStatus = PaymentAppStatus.CANCELED; // CANCELED
        this.pgStatus = (String) pgPayload.get("status");
        this.pgPayload = pgPayload;
        this.canceledAt = LocalDateTime.now();
    }
}
