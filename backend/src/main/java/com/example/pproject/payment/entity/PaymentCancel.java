package com.example.pproject.payment.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 결제 취소 이력 엔티티.
 * <p>
 * 하나의 결제(Payment)에 대해 여러 번의 취소(부분 취소 등)가 발생할 수 있으므로 별도 엔티티로 관리합니다.
 * 취소 금액, 사유, 토스 거래 키 등을 불변으로 기록합니다.
 * </p>
 */
@Entity
@Immutable // 한번 생성되면 수정되지 않음 (Update 쿼리 방지)
@Table(
        name = "payment_cancel",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_payment_cancel_tx", columnNames = {"payment_id", "toss_transaction_key"}),
                @UniqueConstraint(name = "uq_payment_cancel_idem", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_payment_cancel_payment_created", columnList = "payment_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PaymentCancel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cancel_id")
    private Long cancelId;

    // Payment와 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // 토스 거래 키 (취소 건마다 부여되는 키)
    @Column(name = "toss_transaction_key", nullable = false, length = 80)
    private String tossTransactionKey;

    @Column(name = "cancel_status", length = 40)
    private String cancelStatus; // DONE 등

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "cancel_amount", nullable = false, precision = 12, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 3))
    })
    private Money cancelAmount;

    @Column(name = "cancel_reason", nullable = false, length = 200)
    private String cancelReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    // 취소 응답 원문 JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_cancel", columnDefinition = "jsonb")
    private Map<String, Object> rawCancel;

    @Builder
    public PaymentCancel(Payment payment, String tossTransactionKey, String cancelStatus, Money cancelAmount, String cancelReason, LocalDateTime canceledAt, String idempotencyKey, Map<String, Object> rawCancel) {
        Assert.notNull(payment, "결제 정보는 필수입니다.");
        Assert.hasText(tossTransactionKey, "토스 거래 키는 필수입니다.");
        Assert.notNull(cancelAmount, "취소 금액은 필수입니다.");
        Assert.hasText(cancelReason, "취소 사유는 필수입니다.");
        Assert.hasText(idempotencyKey, "멱등키는 필수입니다.");

        this.payment = payment;
        this.tossTransactionKey = tossTransactionKey;
        this.cancelStatus = cancelStatus;
        this.cancelAmount = cancelAmount;
        this.cancelReason = cancelReason;
        this.canceledAt = canceledAt != null ? canceledAt : LocalDateTime.now();
        this.idempotencyKey = idempotencyKey;
        this.rawCancel = rawCancel;
    }
}
