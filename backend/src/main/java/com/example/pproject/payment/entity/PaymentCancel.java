package com.example.pproject.payment.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
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

}
