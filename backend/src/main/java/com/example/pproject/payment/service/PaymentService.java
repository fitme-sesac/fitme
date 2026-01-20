package com.example.pproject.payment.service;

import com.example.pproject.Constant.OrderStatus;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.order.entity.Orders;
import com.example.pproject.order.repository.OrderRepository;
import com.example.pproject.payment.dto.request.PaymentCancelRequest;
import com.example.pproject.payment.dto.request.PaymentConfirmRequest;
import com.example.pproject.payment.dto.request.PaymentCreateRequest;
import com.example.pproject.payment.dto.response.PaymentCancelResponse;
import com.example.pproject.payment.dto.response.PaymentResponse;
import com.example.pproject.payment.dto.response.PgWebhookInboxResponse;
import com.example.pproject.payment.dto.toss.TossPaymentResponse;
import com.example.pproject.payment.dto.webhook.TossWebhookRequest;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.payment.entity.PaymentCancel;
import com.example.pproject.payment.entity.PgWebhookInbox;
import com.example.pproject.payment.port.PaymentPort;
import com.example.pproject.payment.repository.PaymentCancelRepository;
import com.example.pproject.payment.repository.PaymentRepository;
import com.example.pproject.payment.repository.PgWebhookInboxRepository;
import com.example.pproject.wallet.service.WalletService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;
    private final PgWebhookInboxRepository pgWebhookInboxRepository;
    private final OrderRepository orderRepository;
    private final PaymentPort paymentPort;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 1. 결제 생성 (요청)
     */
    @Transactional
    public PaymentResponse createPayment(Long userId, PaymentCreateRequest request) {
        UUID orderUid = UUID.randomUUID();

        if (paymentRepository.existsByOrder_OrderUid(orderUid)) {
            throw new IllegalStateException("이미 존재하는 주문 ID입니다. 다시 시도해주세요.");
        }

        Orders order = Orders.builder()
                .orderUid(orderUid)
                .buyerType(RoleType.CANDIDATE)
                .buyerMemberId(userId)
                .orderAmount(Money.wons(request.amount()))
                .status(OrderStatus.CREATED)
                .build();
        orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(order)
                .method(request.method() != null ? request.method() : PaymentMethod.CARD)
                .paidAmount(Money.wons(request.amount()))
                .build();

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    /**
     * 2. 결제 승인 (최종 완료)
     */
    public PaymentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        UUID orderUid = UUID.fromString(request.orderId());

        transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
            
            // 본인 확인 (선택 사항: 승인 단계에서도 체크 가능)
            if (!payment.getOrder().getBuyerMemberId().equals(userId)) {
                throw new IllegalStateException("본인의 결제만 승인할 수 있습니다.");
            }
            
            payment.confirm(request.paymentKey());
            return null;
        });

        TossPaymentResponse tossResponse = paymentPort.confirm(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );

        return transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

            Map<String, Object> rawPayload = objectMapper.convertValue(tossResponse, new TypeReference<Map<String, Object>>() {});
            payment.approve(tossResponse, rawPayload);

            walletService.chargeCredit(
                    userId,
                    RoleType.CANDIDATE,
                    payment.getPaidAmount().getAmount().longValue(),
                    payment.getPaidAmount(),
                    payment.getPaymentId()
            );
            return PaymentResponse.from(payment);
        });
    }

    /**
     * 3. 결제 취소
     */
    public void cancelPayment(Long userId, String orderId, PaymentCancelRequest request) {
        UUID orderUid = UUID.fromString(orderId);

        Payment paymentInfo = paymentRepository.findByOrder_OrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        
        // [추가] 본인 확인 로직
        if (!paymentInfo.getOrder().getBuyerMemberId().equals(userId)) {
            throw new IllegalStateException("본인의 결제만 취소할 수 있습니다.");
        }
        
        String paymentKey = paymentInfo.getPgPaymentKey();

        TossPaymentResponse tossResponse = paymentPort.cancel(
                paymentKey,
                request.cancelReason()
        );

        transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

            Map<String, Object> rawPayload = objectMapper.convertValue(tossResponse, new TypeReference<Map<String, Object>>() {});
            payment.cancel(tossResponse, rawPayload);

            PaymentCancel cancel = PaymentCancel.builder()
                    .payment(payment)
                    .tossTransactionKey(tossResponse.transactionKey())
                    .cancelStatus("DONE")
                    .cancelAmount(Money.wons(request.cancelAmount() != null ? request.cancelAmount() : payment.getPaidAmount().getAmount()))
                    .cancelReason(request.cancelReason())
                    .idempotencyKey(UUID.randomUUID().toString())
                    .rawCancel(rawPayload)
                    .build();
            paymentCancelRepository.save(cancel);
            return null;
        });
    }

    /**
     * 4. 웹훅 처리 (비동기)
     */
    @Async
    @Transactional
    public void handleWebhook(TossWebhookRequest request) {
        processWebhookLogic(request, null);
    }

    /**
     * 4-1. 웹훅 처리 로직 (공통)
     */
    private void processWebhookLogic(TossWebhookRequest request, PgWebhookInbox existingInbox) {
        String paymentKey = request.data().paymentKey();
        String status = request.data().status();
        String orderIdStr = request.data().orderId();

        log.info("웹훅 처리 시작: paymentKey={}, status={}, orderId={}", paymentKey, status, orderIdStr);

        PgWebhookInbox inbox = existingInbox;
        if (inbox == null) {
            Map<String, Object> payload = objectMapper.convertValue(request, new TypeReference<Map<String, Object>>() {});
            
            Payment payment = null;
            try {
                UUID orderUid = UUID.fromString(orderIdStr);
                payment = paymentRepository.findByOrder_OrderUid(orderUid)
                        .orElseGet(() -> paymentRepository.findByPgPaymentKey(paymentKey).orElse(null));
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 orderId 형식: {}. paymentKey로 조회를 시도합니다.", orderIdStr);
                payment = paymentRepository.findByPgPaymentKey(paymentKey).orElse(null);
            }

            inbox = PgWebhookInbox.builder()
                    .pgEventId(UUID.randomUUID().toString())
                    .eventType(request.eventType())
                    .payment(payment)
                    .payload(payload)
                    .build();
            pgWebhookInboxRepository.save(inbox);
        }

        try {
            if ("DONE".equals(status)) {
                log.info("결제 완료 웹훅 처리: {}", orderIdStr);
            }
            inbox.markAsProcessed();
        } catch (Exception e) {
            log.error("웹훅 처리 실패", e);
            inbox.markAsFailed(e.getMessage());
        }
    }

    /**
     * 5. 결제 단건 조회
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String orderId) {
        UUID orderUid = UUID.fromString(orderId);
        Payment payment = paymentRepository.findByOrder_OrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        return PaymentResponse.from(payment);
    }

    /**
     * 6. 내 결제 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByOrder_BuyerMemberId(userId, pageable)
                .map(PaymentResponse::from);
    }

    /**
     * 7. 결제 취소 이력 조회
     */
    @Transactional(readOnly = true)
    public List<PaymentCancelResponse> getPaymentCancels(String orderId) {
        UUID orderUid = UUID.fromString(orderId);
        Payment payment = paymentRepository.findByOrder_OrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        
        return paymentCancelRepository.findByPayment(payment).stream()
                .map(PaymentCancelResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 8. [관리자] 웹훅 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<PgWebhookInboxResponse> getWebhooks(Pageable pageable) {
        return pgWebhookInboxRepository.findAll(pageable)
                .map(PgWebhookInboxResponse::from);
    }

    /**
     * 9. [관리자] 웹훅 재처리
     */
    @Transactional
    public void retryWebhook(Long inboxId) {
        PgWebhookInbox inbox = pgWebhookInboxRepository.findById(inboxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 웹훅입니다."));
        
        TossWebhookRequest request = objectMapper.convertValue(inbox.getPayload(), TossWebhookRequest.class);
        processWebhookLogic(request, inbox);
    }
}
