package com.example.pproject.wallet.repository;

import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.entity.WalletCreditLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface WalletCreditLotRepository extends JpaRepository<WalletCreditLot, Long> {

    // FIFO 차감용: chunk 조회 + (권장) LOT row에도 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<WalletCreditLot> findByWalletAndRemainingCreditGreaterThanOrderByCreatedAtAsc(
            Wallet wallet, Long remainingCredit, Pageable pageable
    );

    // 읽기용 전체 조회
    List<WalletCreditLot> findByWalletAndRemainingCreditGreaterThanOrderByCreatedAtAsc(Wallet wallet, Long remainingCredit);

    // (권장) 결제 멱등성 체크
    boolean existsByPaymentId(Long paymentId);
}
