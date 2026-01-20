package com.example.pproject.wallet.dto;

import com.example.pproject.Constant.WalletStatus;
import com.example.pproject.wallet.entity.Wallet;

import java.time.LocalDateTime;

/**
 * [응답 DTO] 내 지갑 정보
 * - 지갑의 현재 총 잔액과 상태 정보를 조회할 때 사용합니다.
 */
public record WalletInfoResponse(
        Long walletId,
        Long balance,
        WalletStatus status,
        LocalDateTime lastUpdated
) {
    public static WalletInfoResponse from(Wallet wallet) {
        return new WalletInfoResponse(
                wallet.getWalletId(),
                wallet.getBalance(),
                wallet.getStatus(),
                wallet.getUpdatedAt()
        );
    }
}
