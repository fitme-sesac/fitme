package com.example.pproject.wallet.repository;

import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.entity.WalletLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {

    /**
     * 특정 지갑의 거래 내역을 최신순으로 조회합니다. (페이징 처리)
     */
    Page<WalletLedger> findByWalletOrderByOccurredAtDesc(Wallet wallet, Pageable pageable);

    /**
     * 특정 지갑의 특정 기간 내 거래 내역을 최신순으로 조회합니다. (월별 조회용)
     */
    Page<WalletLedger> findByWalletAndOccurredAtBetweenOrderByOccurredAtDesc(
            Wallet wallet, LocalDateTime startAt, LocalDateTime endAt, Pageable pageable
    );
    
    /**
     * 특정 멱등키(idempotencyKey)를 가진 거래 내역이 있는지 확인합니다.
     * 중복 거래 방지용으로 사용됩니다.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
