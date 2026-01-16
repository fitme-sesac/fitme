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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletCreditLotRepository creditLotRepository;
    private final WalletLedgerRepository ledgerRepository;

    // =================================================================================
    // 1. 조회 로직 (Read)
    // =================================================================================

    /**
     * 내 지갑 조회 (잔액 확인)
     */
    public Wallet getMyWallet(Long userId, RoleType roleType) {
        return findWalletByOwner(userId, roleType);
    }

    /**
     * 지갑 원장(거래 내역) 전체 조회
     */
    public Page<WalletLedgerResponse> getMyLedgers(Long userId, RoleType roleType, Pageable pageable) {
        Wallet wallet = findWalletByOwner(userId, roleType);
        return ledgerRepository.findByWalletOrderByOccurredAtDesc(wallet, pageable)
                .map(WalletLedgerResponse::from);
    }

    /**
     * 지갑 원장(거래 내역) 월별 조회
     */
    public Page<WalletLedgerResponse> getMyLedgersByMonth(Long userId, RoleType roleType, int year, int month, Pageable pageable) {
        Wallet wallet = findWalletByOwner(userId, roleType);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startAt = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        return ledgerRepository.findByWalletAndOccurredAtBetweenOrderByOccurredAtDesc(wallet, startAt, endAt, pageable)
                .map(WalletLedgerResponse::from);
    }

    /**
     * 유효한 크레딧 LOT 목록 조회
     */
    public List<WalletCreditLot> getMyCreditLots(Long userId, RoleType roleType) {
        // TODO: 지갑 조회 후 creditLotRepository 호출
        return null;
    }

    // =================================================================================
    // 2. 비즈니스 로직 (Write - Transactional)
    // =================================================================================

    /**
     * [시스템/결제] 크레딧 충전 (결제 완료 시 호출)
     */
    @Transactional
    public void chargeCredit(Long userId, RoleType roleType, long amount, Money price, Long paymentId) {
        // TODO: 1. 지갑 조회 (Lock)
        // TODO: 2. 지갑 잔액 증가 (wallet.charge)
        // TODO: 3. CreditLot 생성 및 저장 (paymentId 포함)
        // TODO: 4. Ledger 기록 (CREDIT)
    }

    /**
     * [시스템/사용] 크레딧 사용 (상품 구매 시 호출) - FIFO 차감
     */
    @Transactional
    public void useCredit(Long userId, RoleType roleType, long amount, String orderId) {
        // TODO: 1. 지갑 조회 (Lock)
        // TODO: 2. 지갑 잔액 차감 (wallet.use)
        // TODO: 3. CreditLot 순회하며 차감 (FIFO)
        // TODO: 4. Ledger 기록 (DEBIT)
    }

    // =================================================================================
    // 3. 관리자 기능 (Admin)
    // =================================================================================

    /**
     * [관리자] 수동 크레딧 지급
     */
    @Transactional
    public void manualCharge(Long walletId, long amount, String memo) {
        // TODO: 관리자 권한 체크는 Controller에서 수행
        // TODO: chargeCredit 로직 재사용 또는 별도 구현
    }

    /**
     * [관리자] 수동 크레딧 차감
     */
    @Transactional
    public void manualDeduct(Long walletId, long amount, String memo) {
        // TODO: useCredit 로직 재사용 또는 별도 구현
    }

    /**
     * [관리자] 지갑 생성 (수동)
     */
    @Transactional
    public Long createWallet(Long userId, RoleType roleType) {
        // TODO: 지갑 생성 및 저장
        return null;
    }

    /**
     * [관리자] 지갑 상태 변경 (정지/재개)
     */
    @Transactional
    public void changeWalletStatus(Long walletId, boolean suspend) {
        // TODO: wallet.suspend() or wallet.resume()
    }

    // =================================================================================
    // 4. 내부 헬퍼 메서드
    // =================================================================================

    private Wallet findWalletByOwner(Long userId, RoleType roleType) {
        if (roleType == RoleType.CANDIDATE) {
            return walletRepository.findByMember(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
        } else if (roleType == RoleType.EMPLOYER) {
            return walletRepository.findByEmployer(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
        }
        throw new IllegalArgumentException("잘못된 사용자 타입입니다.");
    }
}
