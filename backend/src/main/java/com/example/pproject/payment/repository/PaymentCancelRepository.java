package com.example.pproject.payment.repository;

import com.example.pproject.payment.entity.Payment;
import com.example.pproject.payment.entity.PaymentCancel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {

    // 멱등키 존재 여부 확인 (중복 취소 방지)
    boolean existsByIdempotencyKey(String idempotencyKey);

    // 특정 결제의 취소 이력 조회
    List<PaymentCancel> findByPayment(Payment payment);
}
