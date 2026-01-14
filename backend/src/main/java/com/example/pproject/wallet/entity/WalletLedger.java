package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.SourceType;
import com.example.pproject.Constant.TxType;
import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "wallet_id")
    private Wallet wallet; // walletId

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type")
    private TxType txType;  // 거래 유형

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;  // 원인 유형

    @Column(name = "source_ref_id")
    private Long sourceRefId;   // 유형 선택 다시

    @Column(name = "amount")
    private int amount;

    @Column(name = "balance_before")
    private int balanceBefore;

    @Column(name = "balance_after")
    private int balanceAfter;

    @Column(name = "idempotency_key")
    private String idempotencyKey;  // 멱등키

    @Column(name = "memo")
    private String memo;    // 운영 메모

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt; // 거래 발생 시각 추가

    @Builder
    public WalletLedger(Wallet wallet, TxType txType, SourceType sourceType, Long sourceRefId, int amount, int balanceBefore, int balanceAfter, String idempotencyKey, String memo) {
        this.wallet = wallet;
        this.txType = txType;
        this.sourceType = sourceType;
        this.sourceRefId = sourceRefId;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.idempotencyKey = idempotencyKey;
        this.memo = memo;
        this.occurredAt = LocalDateTime.now(); // 생성 시점에 현재 시간으로 초기화
    }
}
