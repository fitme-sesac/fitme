package com.example.pproject.wallet.repository;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * 지갑 ID로 지갑을 조회하며, 비관적 락(PESSIMISTIC_WRITE)을 겁니다.
     * 잔액 변경(충전/사용) 시 동시성 문제를 방지하기 위해 사용합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.walletId = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") Long id);

    // 멤버 ID로 지갑 조회 (단순 조회용)
    Optional<Wallet> findByMember(UserEntity memberId);

    // 고용주 ID로 지갑 조회 (단순 조회용)
    Optional<Wallet> findByEmployer(EmployerEntity employerId);

    // 멤버 ID로 지갑 조회 + 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.member = :memberId")
    Optional<Wallet> findByMemberWithLock(@Param("memberId") Long memberId);

    // 고용주 ID로 지갑 조회 + 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.employer = :employerId")
    Optional<Wallet> findByEmployerWithLock(@Param("employerId") Long employerId);
}
