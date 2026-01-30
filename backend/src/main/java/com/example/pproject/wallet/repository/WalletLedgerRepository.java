package com.example.pproject.wallet.repository;

import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.entity.WalletLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {

    Page<WalletLedger> findByWalletOrderByOccurredAtDesc(Wallet wallet, Pageable pageable);

    // [start, endExclusive)
    Page<WalletLedger> findByWalletAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            Wallet wallet, Instant startAt, Instant endExclusive, Pageable pageable
    );

    boolean existsByIdempotencyKey(String idempotencyKey);
}
