package com.example.pproject.wallet.dto;

import com.example.pproject.Constant.WalletStatus;
import com.example.pproject.wallet.entity.Wallet;

import java.time.LocalDateTime;

// 내 지갑 정보 응답 ResponseDTO
public record WalletInfoResponse(
        Long walletId,
        Long balance,
        WalletStatus status,
        LocalDateTime lastUpdated
) {
    /**
     * Create a WalletInfoResponse DTO from a Wallet entity.
     *
     * @param wallet the Wallet entity to convert; its id, balance, status, and updated timestamp are copied into the DTO
     * @return a WalletInfoResponse containing the walletId, balance, status, and lastUpdated values from the provided entity
     */
    public static WalletInfoResponse from(Wallet wallet) {
        return new WalletInfoResponse(
                wallet.getWalletId(),
                wallet.getBalance(),
                wallet.getStatus(),
                wallet.getUpdatedAt()
        );
    }
}