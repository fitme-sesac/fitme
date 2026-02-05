package com.example.pproject.payment.repository;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.payment.entity.PaymentCancel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {

    // 특정 결제의 취소 이력 조회
    List<PaymentCancel> findByPayment(Payment payment);

    // 총 환불 금액 합계 (완료된 취소 건만)
    @Query("SELECT COALESCE(SUM(pc.cancelAmount.amount), 0) FROM PaymentCancel pc WHERE pc.cancelStatus = 'DONE'")
    Long sumTotalCancels();

    // 구매자 유형별 환불 금액 합계
    @Query("SELECT COALESCE(SUM(pc.cancelAmount.amount), 0) FROM PaymentCancel pc WHERE pc.cancelStatus = 'DONE' AND pc.payment.order.buyerType = :buyerType")
    Long sumTotalCancelsByBuyerType(@Param("buyerType") BuyerType buyerType);
}
