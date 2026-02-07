package com.example.pproject.wallet.service;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.SourceType;
import com.example.pproject.Constant.TxType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import com.example.pproject.wallet.dto.WalletLedgerResponse;
import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.entity.WalletCreditLot;
import com.example.pproject.wallet.entity.WalletLedger;
import com.example.pproject.wallet.repository.WalletCreditLotRepository;
import com.example.pproject.wallet.repository.WalletLedgerRepository;
import com.example.pproject.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 지갑(Wallet) 도메인의 핵심 비즈니스 로직을 담당하는 서비스.
 * <p>
 * - 지갑 조회, 충전, 사용, 관리자 기능 등을 제공합니다.
 * - 동시성 제어를 위해 비관적 락(Pessimistic Lock)을 사용합니다.
 * - 모든 잔액 변경은 원장(Ledger)에 기록됩니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private static final int LOT_FETCH_CHUNK_SIZE = 50;

    private final WalletRepository walletRepository;
    private final WalletCreditLotRepository creditLotRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;

    // =================================================================================
    // 1. 조회 로직 (Read)
    // =================================================================================

    /**
     * 내 지갑 정보를 조회합니다.
     * 지갑이 없으면 자동으로 생성합니다.
     *
     * @param userId   사용자 ID
     * @param roleType 사용자 역할 (CANDIDATE / EMPLOYER)
     * @return 조회된 지갑 엔티티
     */
    @Transactional
    public Wallet getMyWallet(Long userId, RoleType roleType) {
        BuyerType buyerType = roleType == RoleType.CANDIDATE ? BuyerType.MEMBER : BuyerType.EMPLOYER;
        return findWalletByOwnerWithLock(userId, buyerType);
    }

    /**
     * 기업 ID로 지갑을 조회합니다. (AdCampaignService 등에서 사용)
     *
     * @param employerId 기업 ID
     * @return 조회된 지갑 엔티티
     */
    public Wallet getEmployerWallet(Long employerId) {
        EmployerEntity employer = employerRepository.findById(employerId)
                .orElseThrow(() -> new IllegalArgumentException("기업을 찾을 수 없습니다. ID: " + employerId));
        return walletRepository.findByEmployer(employer)
                .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
    }

    /**
     * 내 지갑의 거래 내역(원장)을 전체 조회합니다. (페이징)
     * 지갑이 없으면 자동으로 생성합니다.
     *
     * @param userId   사용자 ID
     * @param roleType 사용자 역할
     * @param pageable 페이징 정보
     * @return 거래 내역 리스트 (DTO)
     */
    @Transactional
    public Page<WalletLedgerResponse> getMyLedgers(Long userId, RoleType roleType, Pageable pageable) {
        BuyerType buyerType = roleType == RoleType.CANDIDATE ? BuyerType.MEMBER : BuyerType.EMPLOYER;
        Wallet wallet = findWalletByOwnerWithLock(userId, buyerType);
        return ledgerRepository.findByWalletOrderByOccurredAtDesc(wallet, pageable)
                .map(WalletLedgerResponse::from);
    }

    /**
     * 내 지갑의 거래 내역을 특정 월별로 조회합니다.
     * 지갑이 없으면 자동으로 생성합니다.
     *
     * @param userId   사용자 ID
     * @param roleType 사용자 역할
     * @param year     조회할 연도
     * @param month    조회할 월
     * @param pageable 페이징 정보
     * @return 해당 월의 거래 내역 리스트 (DTO)
     */
    @Transactional
    public Page<WalletLedgerResponse> getMyLedgersByMonth(Long userId, RoleType roleType, int year, int month,
            Pageable pageable) {
        BuyerType buyerType = roleType == RoleType.CANDIDATE ? BuyerType.MEMBER : BuyerType.EMPLOYER;
        Wallet wallet = findWalletByOwnerWithLock(userId, buyerType);

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startAt = ym.atDay(1).atStartOfDay();
        // 해당 월의 마지막 순간까지 (Inclusive)
        LocalDateTime endInclusive = ym.atEndOfMonth().atTime(LocalTime.MAX);

        return ledgerRepository.findByWalletAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                wallet, startAt.toInstant(ZoneOffset.UTC), endInclusive.toInstant(ZoneOffset.UTC), pageable)
                .map(WalletLedgerResponse::from);
    }

    /**
     * 내 지갑의 유효한(잔여량이 있는) 크레딧 묶음(Lot) 목록을 조회합니다.
     * 지갑이 없으면 자동으로 생성합니다.
     *
     * @param userId   사용자 ID
     * @param roleType 사용자 역할
     * @return 잔여 크레딧 Lot 리스트 (오래된 순)
     */
    @Transactional
    public List<WalletCreditLot> getMyCreditLots(Long userId, RoleType roleType) {
        BuyerType buyerType = roleType == RoleType.CANDIDATE ? BuyerType.MEMBER : BuyerType.EMPLOYER;
        Wallet wallet = findWalletByOwnerWithLock(userId, buyerType);
        return creditLotRepository.findByWalletAndRemainingCreditGreaterThanOrderByCreatedAtAsc(wallet, 0L);
    }

    // =================================================================================
    // 2. 비즈니스 로직 (Write - Transactional)
    // =================================================================================

    /**
     * [시스템/결제] 크레딧을 충전합니다. (결제 완료 시 호출)
     * <p>
     * 1. 지갑 조회 (비관적 락)
     * 2. 잔액 증가
     * 3. CreditLot 생성
     * 4. Ledger 기록
     * </p>
     * <p>
     * <b>[주의] 결제 소유권 검증 필요:</b>
     * 현재 로직은 paymentId의 유효성이나 소유권을 검증하지 않습니다.
     * 실제 운영 시에는 결제 시스템(PG사) 또는 내부 PaymentService를 통해
     * 해당 paymentId가 유효하고, 현재 요청한 userId의 결제인지 반드시 검증해야 합니다.
     * </p>
     *
     * @param userId    사용자 ID
     * @param buyerType 사용자 역할 (BuyerType)
     * @param amount    충전할 크레딧 양
     * @param price     결제 금액 정보
     * @param payment   결제 엔티티 (멱등성 키로 사용)
     */
    @Transactional
    public void chargeCredit(Long userId, BuyerType buyerType, long amount, Money price, Payment payment) {

        // 1. 락 획득
        Wallet wallet = findWalletByOwnerWithLock(userId, buyerType);

        // 2. 락 획득 후 멱등성 체크
        if (creditLotRepository.existsByPayment(payment)) {
            return; // 이미 처리됨
        }

        // 결제 검증은 호출자(PaymentService.confirmPayment)에서 이미 완료됨
        // - 소유권 검증: prepareConfirm()에서 validateOwner() 호출
        // - 금액 검증: payment.approve()에서 토스 응답과 비교

        String productName = payment.getOrder().getProduct().getName();
        String memo;
        SourceType sourceType;

        if (payment.getOrder().getProduct().isSubscription()) {
            memo = "구독 크레딧 지급 (" + productName + ")";
            sourceType = SourceType.SUBSCRIPTION; // 구독은 SUBSCRIPTION 타입으로 구분
        } else if (payment.getOrder().getProduct().isOneTime()) {
            memo = "크레딧 충전"; // 단건결제는 간단하게 표시
            sourceType = SourceType.PAYMENT;
        } else {
            memo = "크레딧 충전 (" + productName + ")";
            sourceType = SourceType.PAYMENT;
        }

        executeCharge(wallet, amount, price, sourceType, payment, "PAYMENT:" + payment.getPaymentId(), memo);
    }

    /**
     * [시스템/사용] 크레딧을 사용(차감)합니다. (상품 구매/서비스 이용 시 호출)
     * <p>
     * 1. 지갑 조회 (비관적 락)
     * 2. 전체 잔액 차감
     * 3. CreditLot FIFO 차감 (Chunk 조회 최적화 적용)
     * 4. Ledger 기록
     * </p>
     *
     * @param userId     사용자 ID
     * @param buyerType  사용자 역할
     * @param amount     사용할 크레딧 양
     * @param orderId    주문 ID (멱등성 키로 사용)
     * @param sourceType 사용처 (AI, AD_CLICK 등)
     */
    @Transactional
    public void useCredit(Long userId, BuyerType buyerType, long amount, String orderId, SourceType sourceType) {

        // 1. 락 획득
        Wallet wallet = findWalletByOwnerWithLock(userId, buyerType);

        // 2. 락 획득 후 멱등성 체크
        if (ledgerRepository.existsByIdempotencyKey(orderId)) {
            return;
        }

        executeUse(wallet, amount, sourceType, null, orderId, "크레딧 사용 (주문: " + orderId + ")");
    }

    /**
     * [시스템/환불] 결제 취소 시 크레딧을 회수합니다.
     * <p>
     * 1. 해당 결제로 생성된 CreditLot 조회
     * 2. 사용 여부 검증 (이미 사용했으면 예외 발생)
     * 3. 지갑 잔액 차감 및 Lot 삭제
     * 4. Ledger 기록
     * </p>
     *
     * @param userId    사용자 ID
     * @param buyerType 사용자 역할
     * @param payment   취소할 결제 정보
     */
    @Transactional
    public void revokeCredit(Long userId, BuyerType buyerType, Payment payment) {
        // 1. 락 획득
        Wallet wallet = findWalletByOwnerWithLock(userId, buyerType);

        // 2. 해당 결제로 생성된 Lot 조회
        WalletCreditLot lot = creditLotRepository.findByPayment(payment)
                .orElseThrow(() -> new IllegalStateException("해당 결제로 충전된 크레딧 정보를 찾을 수 없습니다."));

        // 3. 사용 여부 검증 (부분 환불 미지원 시, 전액 남아있어야 함)
        if (!lot.isRefundable()) {
            throw new IllegalStateException("이미 사용된 크레딧이 포함되어 있어 환불할 수 없습니다.");
        }

        long revokeAmount = lot.getRemainingCredit();
        long balanceBefore = wallet.getBalance();

        // 4. 지갑 잔액 차감 (회수)
        wallet.revoke(revokeAmount);

        // 5. Lot 삭제 (또는 만료 처리) - 여기서는 삭제하여 재사용 방지
        creditLotRepository.delete(lot);

        // 6. Ledger 기록 (DEBIT)
        String idempotencyKey = "REVOKE:" + payment.getPaymentId();
        if (!ledgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            WalletLedger ledger = wallet.createLedger(TxType.DEBIT, SourceType.PAYMENT, payment.getPaymentId(),
                    revokeAmount, balanceBefore, idempotencyKey, "결제 취소로 인한 크레딧 회수");
            ledgerRepository.save(ledger);
        }
    }

    /**
     * [시스템/환불실패] 결제 취소 실패 시 회수했던 크레딧을 복구합니다. (보상 트랜잭션)
     */
    @Transactional
    public void recoverCredit(Long userId, BuyerType buyerType, Payment payment) {
        // 1. 락 획득
        Wallet wallet = findWalletByOwnerWithLock(userId, buyerType);

        // 2. 이미 복구되었거나 Lot이 존재하는지 확인
        if (creditLotRepository.existsByPayment(payment)) {
            return;
        }

        // 3. 원래 충전했던 금액만큼 다시 충전 (기존 로직 재사용)
        // 주의: Payment 엔티티의 paidAmount를 참조하여 원래 금액 복구
        long amountToRecover = payment.getOrder().getProduct().getCreditAmount().longValue();
        Money price = payment.getPaidAmount();

        executeCharge(wallet, amountToRecover, price, SourceType.PAYMENT, payment,
                "RECOVER:" + payment.getPaymentId(), "결제 취소 실패로 인한 크레딧 복구");
    }

    // =================================================================================
    // 3. 관리자 기능 (Admin)
    // =================================================================================

    /**
     * [관리자] 특정 지갑에 크레딧을 수동으로 지급합니다.
     *
     * @param walletId 지갑 ID
     * @param amount   지급할 양
     * @param memo     관리자 메모
     */
    @Transactional
    public void manualCharge(Long walletId, long amount, String memo) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));

        // 수동 지급은 가격 0원, SourceType.MANUAL
        executeCharge(wallet, amount, Money.ZERO, SourceType.MANUAL, null, null, memo);
    }

    /**
     * [관리자] 특정 지갑에서 크레딧을 수동으로 차감(회수)합니다.
     *
     * @param walletId 지갑 ID
     * @param amount   차감할 양
     * @param memo     관리자 메모
     */
    @Transactional
    public void manualDeduct(Long walletId, long amount, String memo) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));

        executeUse(wallet, amount, SourceType.MANUAL, null, null, memo);
    }

    /**
     * [관리자] 지갑을 수동으로 생성합니다.
     *
     * @param userId    사용자 ID
     * @param buyerType 사용자 역할
     * @return 생성된 지갑 ID
     * @throws IllegalStateException 이미 지갑이 존재하는 경우
     */
    @Transactional
    public Long createWallet(Long userId, BuyerType buyerType) {
        UserEntity user = null;
        EmployerEntity employer = null;

        if (buyerType == BuyerType.MEMBER) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            if (walletRepository.findByMember(user).isPresent()) {
                throw new IllegalStateException("이미 지갑이 존재합니다.");
            }
        } else if (buyerType == BuyerType.EMPLOYER) {
            employer = employerRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("기업을 찾을 수 없습니다."));
            if (walletRepository.findByEmployer(employer).isPresent()) {
                throw new IllegalStateException("이미 지갑이 존재합니다.");
            }
        }

        Wallet wallet = Wallet.builder()
                .ownerType(buyerType)
                .member(user)
                .employer(employer)
                .build();

        try {
            // 2. DB 저장 시도 (Unique 제약조건에 의한 2차 방어)
            return walletRepository.save(wallet).getWalletId();
        } catch (DataIntegrityViolationException e) {
            // TOCTOU(Time-Of-Check to Time-Of-Use) 경쟁 조건 발생 시 예외 변환
            throw new IllegalStateException("이미 지갑이 존재합니다.");
        }
    }

    /**
     * [관리자] 지갑 상태를 변경합니다. (정지/재개)
     *
     * @param walletId 지갑 ID
     * @param suspend  true면 정지(Suspend), false면 재개(Resume)
     */
    @Transactional
    public void changeWalletStatus(Long walletId, boolean suspend) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));

        if (suspend) {
            wallet.suspend();
        } else {
            wallet.resume();
        }
    }

    // =================================================================================
    // 4. 내부 헬퍼 (공통 로직)
    // =================================================================================

    /**
     * 공통 충전 로직 (결제 충전 & 관리자 지급)
     */
    private void executeCharge(Wallet wallet, long amount, Money price, SourceType sourceType, Payment payment,
            String idempotencyKey, String memo) {
        long balanceBefore = wallet.getBalance();

        // 1. 지갑 잔액 증가
        wallet.charge(amount);

        // 2. CreditLot 생성 및 저장 (엔티티 팩토리 메서드 사용)
        WalletCreditLot creditLot = wallet.createCreditLot(amount, price, payment);
        creditLotRepository.save(creditLot);

        // 3. Ledger 기록 (엔티티 팩토리 메서드 사용)
        Long sourceRefId = (payment != null) ? payment.getPaymentId() : null;
        WalletLedger ledger = wallet.createLedger(TxType.CREDIT, sourceType, sourceRefId, amount, balanceBefore,
                idempotencyKey, memo);
        ledgerRepository.save(ledger);
    }

    /**
     * 공통 사용 로직 (서비스 이용 & 관리자 차감)
     * - FIFO 방식으로 CreditLot을 순회하며 차감합니다.
     */
    private void executeUse(Wallet wallet, long amount, SourceType sourceType, Long sourceRefId, String idempotencyKey,
            String memo) {
        long balanceBefore = wallet.getBalance();

        // 1. 지갑 잔액 차감 (부족하면 예외)
        wallet.use(amount);

        // 2. LOT FIFO 차감
        consumeCreditLots(wallet, amount);

        // 3. Ledger 기록 (엔티티 팩토리 메서드 사용)
        WalletLedger ledger = wallet.createLedger(TxType.DEBIT, sourceType, sourceRefId, amount, balanceBefore,
                idempotencyKey, memo);
        ledgerRepository.save(ledger);
    }

    /**
     * [Phase 2] 광고 예산 예약 (하루치)
     */
    @Transactional
    public void holdBudget(Long employerId, long amount, String idempotencyKey) {
        // [Fix] 시스템 호출이므로 EmployerID를 직접 사용 (MemberID 변환 불필요)
        Wallet wallet = findWalletByEmployerWithLock(employerId);

        if (ledgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        long balanceBefore = wallet.getBalance();

        // 1. 예약 (가용잔액 체크 포함)
        wallet.hold(amount);

        // 2. Ledger 기록 (TxType: DEBIT, 금액은 찍히지만 잔액 변동은 없음 - 예약 로그)
        WalletLedger ledger = wallet.createLedger(TxType.DEBIT, SourceType.AD_CLICK, null, amount, balanceBefore,
                idempotencyKey, "광고 예산 예약 (HOLD: " + amount + ")");
        ledgerRepository.save(ledger);
    }

    /**
     * [Phase 2] 광고 클릭 이벤트 배치 정산 및 환불 (Batch Settlement)
     * - 어제 사용한 금액(usedAmount)만큼 차감하고, 남은 예약금을 환불합니다.
     */
    @Transactional
    public void settleDailyUsage(Long employerId, long usedAmount, String idempotencyKey) {
        // 이미 처리된 정산인지 확인
        if (ledgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        // [Fix] 시스템 호출이므로 EmployerID를 직접 사용
        Wallet wallet = findWalletByEmployerWithLock(employerId);
        long balanceBefore = wallet.getBalance(); // 사실상 balance 변화는 없음 (reserved 내부 처리) -> 아님, 환불 시 증가함.

        // 1. 실제 사용분 처리 (예약금 소멸)
        if (usedAmount > 0) {
            wallet.deductReserved(usedAmount);
            consumeCreditLots(wallet, usedAmount); // 사용된 크레딧 소멸
        }

        // 2. 남은 예약금 환불 (Balance 복구)
        long refunded = wallet.getReservedBalance(); // 남은 거 다 환불
        wallet.releaseAllReservation();

        // 3. Ledger (정산 및 환불 로그)
        String memo = String.format("일일 정산 (사용: %d, 환불: %d)", usedAmount, refunded);

        // 사용 내역 기록 (DEBIT)
        if (usedAmount > 0) {
            WalletLedger usageLedger = wallet.createLedger(TxType.DEBIT, SourceType.AD_CLICK, null, usedAmount,
                    balanceBefore,
                    idempotencyKey + "_USAGE", memo);
            ledgerRepository.save(usageLedger);
        }

        // 환불 내역은 별도 기록 필요 없음? -> Balance가 늘어나니까 CREDIT인지?
        // 아님. 원래 내 돈이었으니 그냥 내부 이동임.
        // 하지만 Balance가 늘어나는 것 처럼 보이니 헷갈릴 수 있음.
        // 여기선 "사용 내역"만 명확히 남기면 됨. (환불은 내부 처리)
    }

    /**
     * CreditLot FIFO 차감 로직 (공통로직으로 분리했음)
     */
    private void consumeCreditLots(Wallet wallet, long amount) {
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
                if (remaining <= 0)
                    break;

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
    }

    /**
     * 기업 ID로 지갑을 조회하며 비관적 락을 겁니다. (시스템/스케줄러용)
     * 지갑이 없으면 자동으로 생성합니다.
     */
    private Wallet findWalletByEmployerWithLock(Long employerId) {
        return walletRepository.findByEmployerWithLock(employerId)
                .orElseGet(() -> createWalletForEmployer(employerId));
    }

    /**
     * 사용자 ID와 역할로 지갑을 조회합니다. (락 없음)
     */
    private Wallet findWalletByOwner(Long userId, RoleType roleType) {
        if (roleType == RoleType.CANDIDATE) {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            return walletRepository.findByMember(user)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
        } else if (roleType == RoleType.EMPLOYER) {
            // userId(MemberId) -> employerId 변환
            EmployerMemberEntity em = employerMemberRepository.findFirstByMemberIdAndActiveTrue(userId)
                    .orElseThrow(() -> new IllegalArgumentException("소속된 기업이 없습니다."));
            Long employerId = em.getEmployerId();

            EmployerEntity employer = employerRepository.findById(employerId)
                    .orElseThrow(() -> new IllegalArgumentException("기업을 찾을 수 없습니다."));
            return walletRepository.findByEmployer(employer)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
        } else {
            throw new IllegalArgumentException("잘못된 사용자 타입입니다.");
        }
    }

    /**
     * 사용자 ID와 역할로 지갑을 조회하며 비관적 락을 겁니다. (수정용)
     * 지갑이 없으면 자동으로 생성합니다.
     */
    private Wallet findWalletByOwnerWithLock(Long userId, BuyerType buyerType) {
        if (buyerType == BuyerType.MEMBER) {
            return walletRepository.findByMemberWithLock(userId)
                    .orElseGet(() -> createWalletForMember(userId));
        } else if (buyerType == BuyerType.EMPLOYER) {
            // userId(MemberId) -> employerId 변환
            EmployerMemberEntity em = employerMemberRepository.findFirstByMemberIdAndActiveTrue(userId)
                    .orElseThrow(() -> new IllegalArgumentException("소속된 기업이 없습니다. memberId: " + userId));
            Long employerId = em.getEmployerId();

            return walletRepository.findByEmployerWithLock(employerId)
                    .orElseGet(() -> createWalletForEmployer(employerId));
        } else {
            throw new IllegalArgumentException("잘못된 사용자 타입입니다.");
        }
    }

    /**
     * 개인 회원용 지갑 자동 생성
     */
    private Wallet createWalletForMember(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Wallet wallet = Wallet.builder()
                .ownerType(BuyerType.MEMBER)
                .member(user)
                .build();

        return walletRepository.save(wallet);
    }

    /**
     * 기업 회원용 지갑 자동 생성
     */
    private Wallet createWalletForEmployer(Long userId) {
        EmployerEntity employer = employerRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("기업을 찾을 수 없습니다."));

        Wallet wallet = Wallet.builder()
                .ownerType(BuyerType.EMPLOYER)
                .employer(employer)
                .build();

        return walletRepository.save(wallet);
    }
}
