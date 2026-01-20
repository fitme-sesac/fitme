package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.SourceType;
import com.example.pproject.Constant.TxType;
import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_ledger",
        indexes = {
                // 지갑별 최신순 조회 최적화
                @Index(name = "idx_ledger_wallet_occurred", columnList = "wallet_id, occurred_at"),
                // 멱등성 (가능하면 unique index 권장)
                @Index(name = "uq_ledger_idempotency_key", columnList = "idempotency_key", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletLedger extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 10)
    private TxType txType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "balance_before", nullable = false)
    private long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Builder
    public WalletLedger(
            Wallet wallet,
            TxType txType,
            SourceType sourceType,
            Long sourceRefId,
            long amount,
            long balanceBefore,
            long balanceAfter,
            String idempotencyKey,
            String memo,
            LocalDateTime occurredAt
    ) {
        Assert.notNull(wallet, "지갑 정보는 필수입니다.");
        Assert.notNull(txType, "거래 유형은 필수입니다.");
        Assert.notNull(sourceType, "거래 출처 유형은 필수입니다.");
        Assert.isTrue(amount > 0, "거래 금액은 0보다 커야 합니다.");
        validateBalanceConsistency(txType, amount, balanceBefore, balanceAfter);

        this.wallet = wallet;
        this.txType = txType;
        this.sourceType = sourceType;
        this.sourceRefId = sourceRefId;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.idempotencyKey = idempotencyKey;
        this.memo = memo;
        this.occurredAt = (occurredAt != null) ? occurredAt : LocalDateTime.now();
    }

    private void validateBalanceConsistency(TxType txType, long amount, long balanceBefore, long balanceAfter) {
        if (txType == TxType.CREDIT) {
            if (balanceAfter - balanceBefore != amount) {
                throw new IllegalArgumentException("입금 시 잔액 증가량이 일치하지 않습니다.");
            }
        } else if (txType == TxType.DEBIT) {
            if (balanceBefore - balanceAfter != amount) {
                throw new IllegalArgumentException("출금 시 잔액 감소량이 일치하지 않습니다.");
            }
        }
    }
}
