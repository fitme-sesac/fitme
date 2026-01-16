package com.example.pproject.wallet.service;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.SourceType;
import com.example.pproject.Constant.TxType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.wallet.dto.WalletLedgerResponse;
import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.entity.WalletCreditLot;
import com.example.pproject.wallet.entity.WalletLedger;
import com.example.pproject.wallet.repository.WalletCreditLotRepository;
import com.example.pproject.wallet.repository.WalletLedgerRepository;
import com.example.pproject.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private static final int LOT_FETCH_CHUNK_SIZE = 50;

    private final WalletRepository walletRepository;
    private final WalletCreditLotRepository creditLotRepository;
    private final WalletLedgerRepository ledgerRepository;

    // =================================================================================
    // 1. 조회 로직 (Read)
    // =================================================================================

    public Wallet getMyWallet(Long userId, RoleType roleType) {
        return findWalletByOwner(userId, roleType);
    }

    public Page<WalletLedgerResponse> getMyLedgers(Long userId, RoleType roleType, Pageable pageable) {
        Wallet wallet = findWalletByOwner(userId, roleType);
        return ledgerRepository.findByWalletOrderByOccurredAtDesc(wallet, pageable)
                .map(WalletLedgerResponse::from);
    }

    /**
     * 월별 조회: [startAt, endExclusive) 권장 (경계 안전)
     */
    public Page<WalletLedgerResponse> getMyLedgersByMonth(Long userId, RoleType roleType, int year, int month, Pageable pageable) {
        Wallet wallet = findWalletByOwner(userId, roleType);

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startAt = ym.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = ym.plusMonths(1).atDay(1).atStartOfDay();

        return ledgerRepository.findByWalletAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                        wallet, startAt, endExclusive, pageable
                )
                .map(WalletLedgerResponse::from);
    }

    public List<WalletCreditLot> getMyCreditLots(Long userId, RoleType roleType) {
        Wallet wallet = findWalletByOwner(userId, roleType);
        return creditLotRepository.findByWalletAndRemainingCreditGreaterThanOrderByCreatedAtAsc(wallet, 0L);
    }

    // =================================================================================
    // 2. 비즈니스 로직 (Write - Transactional)
    // =================================================================================

    /**
     * [시스템/결제] 크레딧 충전
     * - paymentId 멱등성 권장 (중복 결제 콜백/재시도 대응)
     */
    @Transactional
    public void chargeCredit(Long userId, RoleType roleType, long amount, Money price, Long paymentId) {

        // (권장) paymentId로 이미 처리된 충전인지 체크
        // - DB 유니크 제약 + exists 체크 둘 다 있으면 더 안전
        if (creditLotRepository.existsByPaymentId(paymentId)) {
            return; // 이미 충전 처리됨(멱등)
        }

        Wallet wallet = findWalletByOwnerWithLock(userId, roleType);
        long balanceBefore = wallet.getBalance();

        wallet.charge(amount);
        long balanceAfter = wallet.getBalance();

        WalletCreditLot creditLot = WalletCreditLot.builder()
                .wallet(wallet)
                .grantedCredit(amount)
                .price(price)
                .paymentId(paymentId)
                .build();
        creditLotRepository.save(creditLot);

        WalletLedger ledger = WalletLedger.builder()
                .wallet(wallet)
                .txType(TxType.CREDIT)
                .sourceType(SourceType.PAYMENT)
                .sourceRefId(paymentId)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .idempotencyKey("PAYMENT:" + paymentId) // (권장) 멱등키 기록
                .memo("크레딧 충전 (결제)")
                .build();
        ledgerRepository.save(ledger);
    }

    /**
     * [시스템/사용] 크레딧 사용 - FIFO 차감
     * - orderId 멱등성 (중복 차감 방지)
     * - LOT 전체 로딩 제거 (Chunk 방식)
     */
    @Transactional
    public void useCredit(Long userId, RoleType roleType, long amount, String orderId, SourceType sourceType) {

        // 0) 멱등성: 이미 처리된 주문이면 중복 차감 방지
        if (ledgerRepository.existsByIdempotencyKey(orderId)) {
            return;
        }

        // 1) 지갑 조회 + Lock
        Wallet wallet = findWalletByOwnerWithLock(userId, roleType);
        long balanceBefore = wallet.getBalance();

        // 2) 지갑 잔액 차감 (부족하면 예외)
        wallet.use(amount);
        long balanceAfter = wallet.getBalance();

        // 3) LOT FIFO 차감 (Chunk 조회)
        long remaining = amount;

        while (remaining > 0) {
            Pageable firstChunk = PageRequest.of(0, LOT_FETCH_CHUNK_SIZE);

            // (중요) 항상 page=0만 재조회해서 skip 문제 방지
            List<WalletCreditLot> lots = creditLotRepository
                    .findByWalletAndRemainingCreditGreaterThanOrderByCreatedAtAsc(wallet, 0L, firstChunk);

            if (lots.isEmpty()) {
                throw new IllegalStateException("데이터 정합성 오류: 지갑 잔액은 충분하나 LOT 합계가 부족합니다.");
            }

            long deductedThisRound = 0;

            for (WalletCreditLot lot : lots) {
                if (remaining <= 0) break;

                long canUse = lot.getRemainingCredit();
                long deduct = Math.min(canUse, remaining);

                lot.consume(deduct);
                remaining -= deduct;
                deductedThisRound += deduct;
            }

            // 안전장치: 무한 루프 방지
            if (deductedThisRound == 0) {
                throw new IllegalStateException("데이터 정합성 오류: LOT 차감이 진행되지 않습니다.");
            }
        }

        // 4) Ledger 기록
        WalletLedger ledger = WalletLedger.builder()
                .wallet(wallet)
                .txType(TxType.DEBIT)
                .sourceType(sourceType)
                .sourceRefId(null)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .idempotencyKey(orderId)
                .memo("크레딧 사용 (주문: " + orderId + ")")
                .build();
        ledgerRepository.save(ledger);
    }

    // =================================================================================
    // 4. 내부 헬퍼
    // =================================================================================

    private Wallet findWalletByOwner(Long userId, RoleType roleType) {
        return switch (roleType) {
            case CANDIDATE -> walletRepository.findByMember(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
            case EMPLOYER -> walletRepository.findByEmployer(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
            default -> throw new IllegalArgumentException("잘못된 사용자 타입입니다.");
        };
    }

    private Wallet findWalletByOwnerWithLock(Long userId, RoleType roleType) {
        return switch (roleType) {
            case CANDIDATE -> walletRepository.findByMemberWithLock(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
            case EMPLOYER -> walletRepository.findByEmployerWithLock(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
            default -> throw new IllegalArgumentException("잘못된 사용자 타입입니다.");
        };
    }
}
