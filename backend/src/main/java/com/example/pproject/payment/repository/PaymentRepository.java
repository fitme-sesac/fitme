package com.example.pproject.payment.repository;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 주문 UID로 조회 (단순 조회용)
    Optional<Payment> findByOrder_OrderUid(UUID orderUid);

    // 주문 UID로 조회 + 비관적 락 (결제 승인/취소 등 상태 변경 시 사용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "3000") })
    @Query("select p from Payment p where p.order.orderUid = :orderUid")
    Optional<Payment> findByOrder_OrderUidWithLock(@Param("orderUid") UUID orderUid);

    // PG 결제 키로 조회 (웹훅 처리 등)
    Optional<Payment> findByPgPaymentKey(String pgPaymentKey);

    // 주문 UID 중복 검사 (결제 생성 시)
    boolean existsByOrder_OrderUid(UUID orderUid);

    // 내 결제 내역 조회 (페이징) - 개인 회원
    // 필드명 변경 반영: BuyerMemberId -> BuyerMember_Id (ID값으로 조회)
    Page<Payment> findByOrder_BuyerMember_Id(Long memberId, Pageable pageable);

    // 내 결제 내역 조회 (페이징) - 기업 회원
    // 필드명 변경 반영: BuyerEmployerId -> BuyerEmployer_Id (ID값으로 조회)
    Page<Payment> findByOrder_BuyerEmployer_Id(Long employerId, Pageable pageable);

    // 총 수익 계산 (결제 완료된 금액 합계)
    @Query("SELECT COALESCE(SUM(p.paidAmount.amount), 0) FROM Payment p WHERE p.appStatus = com.example.pproject.Constant.PaymentAppStatus.APPROVED")
    Long sumTotalRevenue();

    // 구매자 유형별 총 수익 계산
    @Query("SELECT COALESCE(SUM(p.paidAmount.amount), 0) FROM Payment p WHERE p.appStatus = com.example.pproject.Constant.PaymentAppStatus.APPROVED AND p.order.buyerType = :buyerType")
    Long sumTotalRevenueByBuyerType(@Param("buyerType") BuyerType buyerType);
}
