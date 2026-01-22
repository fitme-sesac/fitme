package com.example.pproject.wallet.dto;

import com.example.pproject.wallet.entity.WalletCreditLot;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * [응답 DTO] 크레딧 묶음(Lot) 정보
 * - 사용자가 보유한 개별 크레딧 충전 건들의 잔여량과 정보를 보여줄 때 사용합니다.
 */
public record WalletCreditLotResponse(
        Long lotId,
        Long grantedCredit,
        Long remainingCredit,
        BigDecimal unitPriceAmount,
        String currency,
        Instant createdAt,
        Instant expiredAt // 만료일 (현재는 미사용, 확장성 고려)
) {
    public static WalletCreditLotResponse from(WalletCreditLot lot) {
        return new WalletCreditLotResponse(
                lot.getLotId(),
                lot.getGrantedCredit(),
                lot.getRemainingCredit(),
                lot.getPrice().getAmount(),
                lot.getPrice().getCurrency(),
                lot.getCreatedAt(),
                null
        );
    }
}
