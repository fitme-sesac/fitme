package com.example.pproject.payment.repository;

import com.example.pproject.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 주문 ID로 조회 (단순 조회용)
    Optional<Payment> findByOrderId(String orderId);

    // 주문 ID로 조회 + 비관적 락 (결제 승인/취소 등 상태 변경 시 사용)
    // 락 획득 대기 시간 3초 설정 (3초 안에 락 못 얻으면 예외 발생)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("select p from Payment p where p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") String orderId);

    // PG 결제 키로 조회 (웹훅 처리 등)
    Optional<Payment> findByPgPaymentKey(String pgPaymentKey);

    // 주문 ID 중복 검사 (결제 생성 시)
    boolean existsByOrderId(String orderId);
}
