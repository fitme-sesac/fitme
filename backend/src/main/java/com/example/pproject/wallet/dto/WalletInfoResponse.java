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
    // Entity -> DTO 변환 편의 메서드
    public static WalletInfoResponse from(Wallet wallet) {
        return new WalletInfoResponse(
                wallet.getWalletId(),
                wallet.getBalance(),
                wallet.getStatus(),
                wallet.getUpdatedAt()
        );
    }
}
