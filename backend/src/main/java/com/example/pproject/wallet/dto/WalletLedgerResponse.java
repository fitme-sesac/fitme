package com.example.pproject.wallet.dto;

import com.example.pproject.Constant.TxType;
import com.example.pproject.wallet.entity.WalletLedger;

import java.time.LocalDateTime;

public record WalletLedgerResponse(
        Long ledgerId,
        TxType type, // CREDIT(입금) / DEBIT(출금)
        int amount,          // 변동 금액
        int balanceAfter,    // 변동 후 잔액
        String memo,
        LocalDateTime occurredAt
) {
    /**
     * Create a WalletLedgerResponse DTO from a WalletLedger entity.
     *
     * @param ledger the source WalletLedger entity whose fields will be copied into the response
     * @return a WalletLedgerResponse populated with the ledger's id, transaction type, amount, balance after, memo, and occurrence timestamp
     */
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