package com.example.pproject.payment.service;

import com.example.pproject.common.vo.Money;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentValidationService {

    private final PaymentRepository paymentRepository;

    public void validatePayment(Long paymentId, Long userId, Money amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다."));

        // 소유권 검증
        if (payment.getOrder() != null) {
            payment.getOrder().validateOwner(userId);
        }

        // 금액 검증
        payment.validateAmount(amount);
    }
}
