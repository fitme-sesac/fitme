package com.example.pproject.payment.service;

import com.example.pproject.Constant.OrderStatus;
import com.example.pproject.Constant.PaymentAppStatus;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
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
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
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

        Orders.OrdersBuilder orderBuilder = Orders.builder()
                .orderUid(orderUid)
                .buyerType(request.buyerType())
                .orderAmount(Money.wons(request.amount()))
                .status(OrderStatus.CREATED);

        if (request.buyerType() == RoleType.CANDIDATE) {
            UserEntity buyer = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            orderBuilder.buyerMemberId(buyer);
        } else if (request.buyerType() == RoleType.EMPLOYER) {
            EmployerEntity employer = employerRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));
            orderBuilder.buyerEmployerId(employer);
        } else {
            throw new IllegalArgumentException("지원하지 않는 구매자 타입입니다.");
        }

        Orders order = orderBuilder.build();
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

        // 1. 결제 요청 상태 검증 및 paymentKey 저장 (트랜잭션 분리)
        transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
            
            // 본인 확인 (엔티티 로직 위임)
            payment.getOrder().validateOwner(userId);
            
            payment.confirm(request.paymentKey());
            return null;
        });

        // 2. 외부 PG사 승인 요청 (서킷 브레이커 적용됨)
        TossPaymentResponse tossResponse = paymentPort.confirm(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );

        // 3. 결과 처리 (트랜잭션 분리)
        return transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

            // 서킷 브레이커 Fallback 등으로 인해 상태를 알 수 없는 경우
            if ("UNKNOWN".equals(tossResponse.status())) {
                log.warn("결제 승인 결과 불명 (Circuit Open or Timeout). orderId={}", request.orderId());
                // 상태를 변경하지 않고(REQUESTED 유지), pgStatus만 업데이트하거나 별도 처리가 필요함.
                // 여기서는 예외를 던져서 클라이언트가 재시도하거나 조회를 유도하도록 함.
                throw new IllegalStateException("결제 시스템 응답이 지연되고 있습니다. 잠시 후 결제 내역을 확인해주세요.");
            }

            Map<String, Object> rawPayload = objectMapper.convertValue(tossResponse, new TypeReference<>() {});
            
            // 승인 성공 처리
            payment.approve(tossResponse, rawPayload);

            // 지갑 충전 로직
            walletService.chargeCredit(
                    userId,
                    payment.getOrder().getBuyerType(), // 주문 당시의 구매자 타입 사용
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
        
        // 본인 확인 (엔티티 로직 위임)
        if (paymentInfo.getOrder() != null) {
            paymentInfo.getOrder().validateOwner(userId);
        }
        
        String paymentKey = paymentInfo.getPgPaymentKey();

        // 외부 PG사 취소 요청
        TossPaymentResponse tossResponse = paymentPort.cancel(
                paymentKey,
                request.cancelReason()
        );

        transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

            if ("UNKNOWN".equals(tossResponse.status())) {
                log.warn("결제 취소 결과 불명. orderId={}", orderId);
                throw new IllegalStateException("결제 취소 요청이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
            }

            Map<String, Object> rawPayload = objectMapper.convertValue(tossResponse, new TypeReference<>() {});
            payment.cancel(tossResponse, rawPayload);

            // PaymentCancel 생성 로직을 Payment 엔티티로 위임
            PaymentCancel cancel = payment.createCancel(
                    tossResponse,
                    request.cancelReason(),
                    Money.wons(request.cancelAmount() != null ? request.cancelAmount() : payment.getPaidAmount().getAmount()),
                    UUID.randomUUID().toString(),
                    rawPayload
            );

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
            Map<String, Object> payload = objectMapper.convertValue(request, new TypeReference<>() {});
            
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
                // 웹훅으로 결제 완료 처리 (승인 API 응답을 못 받았을 경우 대비)
                if (inbox.getPayment() != null && inbox.getPayment().getAppStatus() == PaymentAppStatus.REQUESTED) {
                    log.info("웹훅을 통한 결제 승인 처리: {}", orderIdStr);
                    // 주의: 웹훅 데이터로 approve 호출 시 검증 로직 필요 (금액 등)
                    // 여기서는 단순 로깅만 하고, 실제 상태 변경은 신중해야 함.
                }
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
    public PaymentResponse getPayment(Long userId, String orderId) {
        UUID orderUid = UUID.fromString(orderId);
        Payment payment = paymentRepository.findByOrder_OrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        
        // 본인 확인 (엔티티 로직 위임)
        if (payment.getOrder() != null) {
            payment.getOrder().validateOwner(userId);
        }
        
        return PaymentResponse.from(payment);
    }

    /**
     * 6. 내 결제 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Long userId, RoleType roleType, Pageable pageable) {
        if (roleType == RoleType.CANDIDATE) {
            return paymentRepository.findByOrder_BuyerMemberId_Id(userId, pageable)
                    .map(PaymentResponse::from);
        } else if (roleType == RoleType.EMPLOYER) {
            return paymentRepository.findByOrder_BuyerEmployerId_Id(userId, pageable)
                    .map(PaymentResponse::from);
        } else {
            throw new IllegalArgumentException("지원하지 않는 사용자 타입입니다.");
        }
    }

    /**
     * 7. 결제 취소 이력 조회
     */
    @Transactional(readOnly = true)
    public List<PaymentCancelResponse> getPaymentCancels(Long userId, String orderId) {
        UUID orderUid = UUID.fromString(orderId);
        Payment payment = paymentRepository.findByOrder_OrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        
        // 본인 확인 (엔티티 로직 위임)
        if (payment.getOrder() != null) {
            payment.getOrder().validateOwner(userId);
        }
        
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
