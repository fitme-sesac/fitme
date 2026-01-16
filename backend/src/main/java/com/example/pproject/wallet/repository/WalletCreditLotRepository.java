package com.example.pproject.wallet.repository;

import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.entity.WalletCreditLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletCreditLotRepository extends JpaRepository<WalletCreditLot, Long> {

    /**
     * 특정 지갑의 '사용 가능한(잔여량이 0보다 큰)' 크레딧 묶음을 조회합니다.
     * 생성일(createdAt) 오름차순으로 정렬하여 FIFO(선입선출) 차감 로직에 사용합니다.
     */
    List<WalletCreditLot> findByWalletAndRemainingCreditGreaterThanOrderByCreatedAtAsc(Wallet wallet, Long remainingCredit);
}
