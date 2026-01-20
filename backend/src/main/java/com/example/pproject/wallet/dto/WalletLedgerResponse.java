package com.example.pproject.wallet.dto;

import com.example.pproject.Constant.TxType;
import com.example.pproject.wallet.entity.WalletLedger;

import java.time.LocalDateTime;

/**
 * [응답 DTO] 지갑 거래 내역(원장)
 * - 입금/출금 내역을 리스트 형태로 보여줄 때 사용합니다.
 */
public record WalletLedgerResponse(
        Long ledgerId,
        TxType type, // CREDIT(입금) / DEBIT(출금)
        Long amount,          // 변동 금액
        Long balanceAfter,    // 변동 후 잔액
        String memo,
        LocalDateTime occurredAt
) {
    public static WalletLedgerResponse from(WalletLedger ledger) {
        return new WalletLedgerResponse(
                ledger.getLedgerId(),
                ledger.getTxType(),
                ledger.getAmount(),
                ledger.getBalanceAfter(),
                ledger.getMemo(),
                ledger.getOccurredAt()
        );
    }
}
