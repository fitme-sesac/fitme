package com.example.pproject.payment.entity;

import com.example.pproject.Constant.PaymentAppStatus;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    // Order와 연관관계 (N:1)
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id", nullable = false)
//    private Order order;

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
    // Spring Boot 3 + Hibernate 6에서는 별도 라이브러리 없이 아래 어노테이션 사용 가능
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pg_payload", columnDefinition = "jsonb")
    private Map<String, Object> pgPayload;

    // === 시간 정보 ===

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;




}
