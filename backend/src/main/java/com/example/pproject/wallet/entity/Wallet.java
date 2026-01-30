package com.example.pproject.wallet.entity;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.SourceType;
import com.example.pproject.Constant.TxType;
import com.example.pproject.Constant.WalletStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Check;
import org.springframework.util.Assert;

@Entity
@DynamicUpdate
@Table(name = "wallet", indexes = {
        @Index(name = "uq_wallet_member_one", columnList = "member_id", unique = true),
        @Index(name = "uq_wallet_employer_one", columnList = "employer_id", unique = true)
})
// DB가 CHECK 제약 지원 시 강력 추천
@Check(constraints = """
        (owner_type = 'MEMBER' AND member_id IS NOT NULL AND employer_id IS NULL)
        OR
        (owner_type = 'EMPLOYER' AND employer_id IS NOT NULL AND member_id IS NULL)
        """)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private BuyerType ownerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private UserEntity member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id")
    private EmployerEntity employer;

    @Column(name = "balance", nullable = false)
    private long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status;

    @Column(name = "reserved_balance", nullable = false)
    private long reservedBalance = 0L;

    @Builder
    public Wallet(BuyerType ownerType, UserEntity member, EmployerEntity employer) {
        validateOwner(ownerType, member, employer);
        this.ownerType = ownerType;
        this.member = member;
        this.employer = employer;
        this.balance = 0L;
        this.reservedBalance = 0L;
        this.status = WalletStatus.ACTIVE;
    }

    public long getAvailableBalance() {
        // [Phase 2 Refactor] 선차감 모델 도입으로 인해 reservedBalance는 가용 잔액 계산에서 제외
        // 00시에 이미 balance에서 차감되어 reserved로 이동했으므로, balance 자체가 가용 잔액임.
        return this.balance;
    }

    public void charge(long amount) {
        verifyActive();
        Assert.isTrue(amount > 0, "충전액은 0보다 커야 합니다.");

        if (Long.MAX_VALUE - this.balance < amount) {
            throw new IllegalStateException("지갑 보유 한도를 초과했습니다.");
        }
        this.balance += amount;
    }

    // 즉시 사용 (가용 잔액 체크 -> balance 체크로 단순화)
    public void use(long amount) {
        verifyActive();
        Assert.isTrue(amount > 0, "사용액은 0보다 커야 합니다.");

        if (this.balance < amount) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    // 예산 예약 (Hold) - [Phase 2: Pre-deduction]
    // 가용 잔액에서 즉시 차감하여 예약금으로 이동
    public void hold(long amount) {
        verifyActive();
        Assert.isTrue(amount > 0, "예약액은 0보다 커야 합니다.");

        if (this.balance < amount) {
            throw new IllegalStateException("잔액이 부족하여 예약할 수 없습니다.");
        }
        this.balance -= amount; // 가용 잔액 차감
        this.reservedBalance += amount; // 예약금 증가
    }

    // 예약금 정산 (사용 확정 + 예약 해제)
    public void settle(long usedAmount, long releasedReservation) {
        verifyActive();
        Assert.isTrue(usedAmount >= 0, "사용액은 0 이상이어야 합니다.");
        Assert.isTrue(releasedReservation > 0, "해제할 예약금은 0보다 커야 합니다.");

        if (this.reservedBalance < releasedReservation) {
            throw new IllegalStateException("해제할 예약금이 현재 예약금보다 큽니다.");
        }
        this.reservedBalance -= releasedReservation;

        // settle은 이제 사용하지 않을 예정이나 호환성을 위해 유지
        // (Pre-deduction에서는 이미 balance가 차감되었으므로 추가 차감 불필요)
        // 하지만 기존 settle 로직은 "예약 건 거에서 사용" 이므로...
        // 아, 기존 로직은 "balance -= used" 였음.
        // Pre-deduction에서는 "balance"는 이미 깎였으니 "reserved"만 깎으면 됨 (used만큼 소멸, 남은건 환불?)
        // -> settle은 "실시간 정산" 용도이므로 배치 정산(deductReserved) 사용 시 호출되지 않음.
    }

    // [Phase 2] 배치 정산용: 실제 사용액만큼 예약금 소멸 (매출 확정)
    public void deductReserved(long amount) {
        if (this.reservedBalance < amount) {
            throw new IllegalStateException("차감할 예약금이 부족합니다.");
        }
        this.reservedBalance -= amount;
    }

    // [Phase 2] 잔여 예약금 환불 (일일 정산 리셋용)
    // 안 쓰고 남은 예약금을 다시 balance로 복구
    public void releaseAllReservation() {
        this.balance += this.reservedBalance;
        this.reservedBalance = 0L;
    }

    public void resume() {
        if (this.status != WalletStatus.ACTIVE) {
            this.status = WalletStatus.ACTIVE;
        }
    }

    public void suspend() {
        if (this.status != WalletStatus.INACTIVE) {
            this.status = WalletStatus.INACTIVE;
        }
    }

    // === 팩토리 메서드 (연관 엔티티 생성 위임) ===

    /**
     * 크레딧 충전용 Lot 생성
     */
    public WalletCreditLot createCreditLot(long amount, Money price, Payment paymentId) {
        return WalletCreditLot.builder()
                .wallet(this)
                .grantedCredit(amount)
                .price(price)
                .payment(paymentId)
                .build();
    }

    /**
     * 거래 원장(Ledger) 생성
     */
    public WalletLedger createLedger(TxType txType, SourceType sourceType, Long sourceRefId, long amount,
            long balanceBefore, String idempotencyKey, String memo) {
        return WalletLedger.builder()
                .wallet(this)
                .txType(txType)
                .sourceType(sourceType)
                .sourceRefId(sourceRefId)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(this.balance) // 현재 잔액 (변경 후)
                .idempotencyKey(idempotencyKey)
                .memo(memo)
                .build();
    }

    private void verifyActive() {
        if (this.status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("정지된 지갑은 사용할 수 없습니다.");
        }
    }

    private void validateOwner(BuyerType ownerType, UserEntity member, EmployerEntity employer) {
        Assert.notNull(ownerType, "지갑 소유자 타입은 필수입니다.");

        if (ownerType == BuyerType.MEMBER) {
            Assert.notNull(member, "일반 회원 지갑은 memberId가 필수입니다.");
            Assert.isNull(employer, "일반 회원 지갑에는 employerId가 없어야 합니다.");
        } else if (ownerType == BuyerType.EMPLOYER) {
            Assert.notNull(employer, "기업 지갑은 employerId가 필수입니다.");
            Assert.isNull(member, "기업 지갑에는 memberId가 없어야 합니다.");
        } else {
            throw new IllegalArgumentException("지갑은 MEMBER 또는 EMPLOYER만 소유할 수 있습니다.");
        }
    }
}
