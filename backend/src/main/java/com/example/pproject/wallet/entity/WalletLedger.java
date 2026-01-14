package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.SourceType;
import com.example.pproject.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletLedger extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ledgerId;

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet walletId;

    private String txType;  // 거래 유형

    private SourceType sourceType;  // 원인 유형

    private Long sourceRefId;   // 유형 선택 다시

    private int amount;

    private int balanceBefore;

    private int balanceAfter;

    private String idempotencyKey;  // 멱등키

    private String memo;    // 운영 메모
}
