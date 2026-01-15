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

/**
 * 지갑 거래 내역(원장) 엔티티.
 * <p>
 * 모든 자금의 흐름(입금/출금)을 불변(Immutable)으로 기록합니다.
 * 시스템 장애 시 이 원장을 재계산(Replay)하여 지갑 잔액을 복구할 수 있어야 합니다.
 * </p>
 */
@Entity
@Table(name = "wallet_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletLedger extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false)
    private TxType txType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "memo")
    private String memo;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder
    public WalletLedger(Wallet wallet, TxType txType, SourceType sourceType, Long sourceRefId, Long amount, Long balanceBefore, Long balanceAfter, String idempotencyKey, String memo) {
        Assert.notNull(wallet, "지갑 정보는 필수입니다.");
        Assert.notNull(txType, "거래 유형은 필수입니다.");
        Assert.isTrue(amount != null && amount >= 0, "거래 금액은 0 이상이어야 합니다.");
        
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
        this.occurredAt = LocalDateTime.now();
    }

    /**
     * 거래 유형에 따른 잔액 변화의 정합성을 검증합니다.
     * <p>
     * - CREDIT(입금): 이전 잔액 + 금액 == 이후 잔액
     * - DEBIT(출금): 이전 잔액 - 금액 == 이후 잔액
     * </p>
     */
    private void validateBalanceConsistency(TxType txType, long amount, long balanceBefore, long balanceAfter) {
        if (txType == TxType.CREDIT) {
            if (balanceAfter != balanceBefore + amount) {
                throw new IllegalArgumentException("입금 시 잔액 증가량이 일치하지 않습니다.");
            }
        } else if (txType == TxType.DEBIT) {
            if (balanceAfter != balanceBefore - amount) {
                throw new IllegalArgumentException("출금 시 잔액 감소량이 일치하지 않습니다.");
            }
        }
    }
}
