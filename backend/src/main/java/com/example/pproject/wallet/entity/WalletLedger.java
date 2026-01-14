package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.SourceType;
import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "tx_type")
    private String txType;  // 거래 유형

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
}
