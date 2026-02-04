package com.example.pproject.payment.service;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.PaymentAppStatus;
import com.example.pproject.Constant.PaymentMethod;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.order.entity.Orders;
import com.example.pproject.order.repository.OrderRepository;
import com.example.pproject.payment.dto.request.PaymentCancelRequest;
import com.example.pproject.payment.dto.request.PaymentConfirmRequest;
import com.example.pproject.payment.dto.request.PaymentCreateRequest;
import com.example.pproject.payment.dto.response.PaymentCancelResponse;
import com.example.pproject.payment.dto.response.PaymentResponse;
import com.example.pproject.payment.dto.response.PgWebhookInboxResponse;
import com.example.pproject.payment.dto.toss.TossBillingResponse;
import com.example.pproject.payment.dto.toss.TossPaymentResponse;
import com.example.pproject.payment.dto.webhook.TossWebhookRequest;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.payment.entity.PaymentCancel;
import com.example.pproject.payment.entity.PgWebhookInbox;
import com.example.pproject.payment.port.PaymentPort;
import com.example.pproject.payment.repository.PaymentCancelRepository;
import com.example.pproject.payment.repository.PaymentRepository;
import com.example.pproject.payment.repository.PgWebhookInboxRepository;
import com.example.pproject.product.entity.Product;
import com.example.pproject.product.repository.ProductRepository;
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
    private final EmployerMemberRepository employerMemberRepository;
    private final ProductRepository productRepository;
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
        checkOrderUidDuplicate(orderUid);

        Orders order = createAndSaveOrder(userId, request, orderUid);

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
            prepareConfirm(orderUid, userId, request.paymentKey());
            return null;
        });

        // 2. 외부 PG사 승인 요청 (서킷 브레이커 적용됨)
        TossPaymentResponse tossResponse = paymentPort.confirm(
                request.paymentKey(),
                request.orderId(),
                request.amount());

        // 3. 결과 처리 (트랜잭션 분리)
        return transactionTemplate.execute(status -> completeConfirm(orderUid, userId, tossResponse));
    }

    /**
     * 3. 결제 취소
     */
    public void cancelPayment(Long userId, String orderId, PaymentCancelRequest request) {
        UUID orderUid = UUID.fromString(orderId);

        // 1. 결제 정보 조회 및 검증 (락 없음)
        Payment paymentInfo = getValidatedPaymentForCancel(orderUid, userId);
        String paymentKey = paymentInfo.getPgPaymentKey();

        // 2. 외부 PG사 취소 요청
        TossPaymentResponse tossResponse = paymentPort.cancel(
                paymentKey,
                request.cancelReason());

        // 3. 결과 처리 (트랜잭션 분리)
        transactionTemplate.execute(status -> {
            completeCancel(orderUid, tossResponse, request);
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

        // 1. Inbox 조회 또는 생성
        PgWebhookInbox inbox = existingInbox;
        if (inbox == null) {
            Payment payment = findPaymentForWebhook(orderIdStr, paymentKey);
            inbox = createWebhookInbox(request, payment);
        }

        // 2. 비즈니스 로직 실행 및 상태 업데이트
        try {
            executeBusinessLogic(request, status, inbox);
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
            return paymentRepository.findByOrder_BuyerMember_Id(userId, pageable)
                    .map(PaymentResponse::from);
        } else if (roleType == RoleType.EMPLOYER) {
            return paymentRepository.findByOrder_BuyerEmployer_Id(userId, pageable)
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

    /**
     * 10. 빌링키 발급
     */
    public TossBillingResponse issueBillingKey(String authKey, String customerKey) {
        return paymentPort.issueBillingKey(authKey, customerKey);
    }

    /**
     * 11. 빌링키 결제 (자동 결제)
     */
    public PaymentResponse payWithBillingKey(Long userId, String billingKey, PaymentCreateRequest request) {
        UUID orderUid = UUID.randomUUID();
        checkOrderUidDuplicate(orderUid);

        // 1. 주문 및 결제 정보 생성 (READY 상태)
        // 트랜잭션 분리: 외부 API 호출 전 DB 커밋을 위해
        Payment payment = transactionTemplate.execute(status -> {
            Orders order = createAndSaveOrder(userId, request, orderUid);
            Payment p = Payment.builder()
                    .order(order)
                    .method(PaymentMethod.CARD) // 빌링키 결제도 카드로 간주
                    .paidAmount(Money.wons(request.amount()))
                    .build();
            return paymentRepository.save(p);
        });

        // 2. 외부 PG사 빌링키 결제 승인 요청
        TossPaymentResponse tossResponse = paymentPort.confirmBilling(
                billingKey,
                orderUid.toString(),
                request.amount());

        // 3. 결과 처리 (트랜잭션 분리)
        return transactionTemplate.execute(status -> completeConfirm(orderUid, userId, tossResponse));
    }

    // =================================================================================
    // Private Helper Methods
    // =================================================================================

    // 주문 UID 중복 체크
    private void checkOrderUidDuplicate(UUID orderUid) {
        if (paymentRepository.existsByOrder_OrderUid(orderUid)) {
            throw new IllegalStateException("이미 존재하는 주문 ID입니다. 다시 시도해주세요.");
        }
    }

    // 주문 엔티티 생성 및 저장 (구매자 타입 분기)
    private Orders createAndSaveOrder(Long userId, PaymentCreateRequest request, UUID orderUid) {
        UserEntity buyer = null;
        EmployerEntity employer = null;

        if (request.buyerType() == BuyerType.MEMBER) {
            buyer = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        } else if (request.buyerType() == BuyerType.EMPLOYER) {
            // userId는 memberId이므로, employer_member를 통해 employer 조회
            EmployerMemberEntity employerMember = employerMemberRepository.findFirstByMemberIdAndActiveTrue(userId)
                    .orElseThrow(() -> new IllegalArgumentException("소속된 기업이 없습니다."));
            employer = employerRepository.findById(employerMember.getEmployerId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));
        }

        // 상품 조회 (삭제된 상품 제외)
        Product product = productRepository.findByProductCodeAndDeletedAtIsNull(request.productCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 정적 팩토리 메서드 사용
        Orders order = Orders.createOrder(
                orderUid,
                request.buyerType(),
                buyer,
                employer,
                product,
                Money.wons(request.amount()),
                request.idempotencyKey());

        return orderRepository.save(order);
    }

    // 결제 승인 전 준비 (조회, 락, 검증, 상태 변경)
    private void prepareConfirm(UUID orderUid, Long userId, String paymentKey) {
        Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 본인 확인 (엔티티 로직 위임 및 기업 멤버십 확인)
        if (payment.getOrder().getBuyerType() == BuyerType.EMPLOYER) {
            boolean isMember = employerMemberRepository.existsByEmployerIdAndMemberIdAndActiveTrue(
                    payment.getOrder().getBuyerEmployer().getId(), userId);
            if (!isMember) {
                throw new IllegalStateException("본인의 기업 주문 내역만 접근할 수 있습니다.");
            }
        } else {
            payment.getOrder().validateOwner(userId);
        }

        payment.confirm(paymentKey);
    }

    // 결제 승인 완료 처리 (상태 변경, 지갑 충전)
    private PaymentResponse completeConfirm(UUID orderUid, Long userId, TossPaymentResponse tossResponse) {
        Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 서킷 브레이커 Fallback 등으로 인해 상태를 알 수 없는 경우
        if ("UNKNOWN".equals(tossResponse.status())) {
            log.warn("결제 승인 결과 불명 (Circuit Open or Timeout). orderId={}", orderUid);
            throw new IllegalStateException("결제 시스템 응답이 지연되고 있습니다. 잠시 후 결제 내역을 확인해주세요.");
        }

        Map<String, Object> rawPayload = objectMapper.convertValue(tossResponse, new TypeReference<>() {
        });

        // 승인 성공 처리
        payment.approve(tossResponse, rawPayload);

        try {
            // 지갑 충전 로직
            walletService.chargeCredit(
                    userId,
                    payment.getOrder().getBuyerType(),
                    payment.getOrder().getProduct().getCreditAmount().longValue(),
                    payment.getPaidAmount(),
                    payment);
        } catch (Exception e) {
            log.error("지갑 충전 실패로 인한 결제 취소 진행. orderId={}, error={}", orderUid, e.getMessage());
            // 보상 트랜잭션: 토스 결제 취소
            try {
                paymentPort.cancel(tossResponse.paymentKey(), "시스템 오류로 인한 자동 취소 (지갑 충전 실패)");
            } catch (Exception cancelEx) {
                log.error("결제 취소 실패 (심각). 수동 확인 필요. paymentKey={}", tossResponse.paymentKey(), cancelEx);
                // TODO: 관리자 알림 발송 (Slack, Email 등)
            }
            throw new IllegalStateException("결제 처리 중 오류가 발생하여 취소되었습니다.");
        }

        return PaymentResponse.from(payment);
    }

    // 결제 취소 전 조회 및 검증
    private Payment getValidatedPaymentForCancel(UUID orderUid, Long userId) {
        Payment paymentInfo = paymentRepository.findByOrder_OrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 본인 확인 (엔티티 로직 위임)
        if (paymentInfo.getOrder() != null) {
            paymentInfo.getOrder().validateOwner(userId);
        }
        return paymentInfo;
    }

    // 결제 취소 완료 처리 (상태 변경, 이력 저장)
    private void completeCancel(UUID orderUid, TossPaymentResponse tossResponse, PaymentCancelRequest request) {
        Payment payment = paymentRepository.findByOrder_OrderUidWithLock(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if ("UNKNOWN".equals(tossResponse.status())) {
            log.warn("결제 취소 결과 불명. orderId={}", orderUid);
            throw new IllegalStateException("결제 취소 요청이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
        }

        Map<String, Object> rawPayload = objectMapper.convertValue(tossResponse, new TypeReference<>() {
        });
        payment.cancel(tossResponse, rawPayload);

        // PaymentCancel 생성 로직을 Payment 엔티티로 위임
        PaymentCancel cancel = payment.createCancel(
                tossResponse,
                request.cancelReason(),
                Money.wons(
                        request.cancelAmount() != null ? request.cancelAmount() : payment.getPaidAmount().getAmount()),
                UUID.randomUUID().toString(),
                rawPayload);

        paymentCancelRepository.save(cancel);
    }

    // 웹훅용 결제 정보 조회 (orderId 우선, 실패 시 paymentKey)
    private Payment findPaymentForWebhook(String orderIdStr, String paymentKey) {
        try {
            UUID orderUid = UUID.fromString(orderIdStr);
            return paymentRepository.findByOrder_OrderUid(orderUid)
                    .orElseGet(() -> paymentRepository.findByPgPaymentKey(paymentKey).orElse(null));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 orderId 형식: {}. paymentKey로 조회를 시도합니다.", orderIdStr);
            return paymentRepository.findByPgPaymentKey(paymentKey).orElse(null);
        }
    }

    // 웹훅 Inbox 엔티티 생성
    private PgWebhookInbox createWebhookInbox(TossWebhookRequest request, Payment payment) {
        Map<String, Object> payload = objectMapper.convertValue(request, new TypeReference<>() {
        });

        // 정적 팩토리 메서드 사용
        return PgWebhookInbox.create(
                UUID.randomUUID().toString(),
                request.eventType(),
                payment,
                payload);
    }

    // 웹훅 비즈니스 로직 실행 (결제 완료 처리 등)
    private void executeBusinessLogic(TossWebhookRequest request, String status, PgWebhookInbox inbox) {
        if ("DONE".equals(status)) {
            Payment payment = inbox.getPayment();
            // 웹훅으로 결제 완료 처리 (승인 API 응답을 못 받았을 경우 대비)
            if (payment != null && payment.getAppStatus() == PaymentAppStatus.REQUESTED) {
                log.info("웹훅을 통한 결제 승인 처리: {}", payment.getPaymentUid());

                // DTO 변환 로직을 DTO 내부 팩토리 메서드로 위임
                TossPaymentResponse tossResponse = TossPaymentResponse.from(request, payment.getOrderName());

                Map<String, Object> rawPayload = objectMapper.convertValue(request, new TypeReference<>() {
                });

                processPaymentApproval(payment, tossResponse, rawPayload);
            }
        }
    }

    // 결제 승인 및 지갑 충전 처리
    private void processPaymentApproval(Payment payment, TossPaymentResponse tossResponse,
            Map<String, Object> rawPayload) {
        // 승인 처리 (엔티티 내부에서 상태 및 금액 검증 수행)
        payment.approve(tossResponse, rawPayload);

        // 지갑 충전 로직
        Long userId = payment.getOrder().getBuyerType() == BuyerType.MEMBER
                ? payment.getOrder().getBuyerMember().getId()
                : payment.getOrder().getBuyerEmployer().getId();

        walletService.chargeCredit(
                userId,
                payment.getOrder().getBuyerType(),
                payment.getOrder().getProduct().getCreditAmount().longValue(),
                payment.getPaidAmount(),
                payment);
    }

    // =====================================================
    // userid(문자열) 기반 래퍼 메서드들 (JWT에 ID가 없을 때 사용)
    // =====================================================

    /**
     * userid로 사용자 ID(Long) 조회
     */
    private Long getUserIdByUserid(String userid) {
        return userRepository.findByUserid(userid)
                .map(UserEntity::getId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userid));
    }

    /**
     * 1. 결제 생성 (userid 기반)
     */
    @Transactional
    public PaymentResponse createPaymentByUserid(String userid, PaymentCreateRequest request) {
        Long userId = getUserIdByUserid(userid);
        return createPayment(userId, request);
    }

    /**
     * 2. 결제 승인 (userid 기반)
     */
    public PaymentResponse confirmPaymentByUserid(String userid, PaymentConfirmRequest request) {
        Long userId = getUserIdByUserid(userid);
        return confirmPayment(userId, request);
    }

    /**
     * 3. 결제 취소 (userid 기반)
     */
    public void cancelPaymentByUserid(String userid, String orderId, PaymentCancelRequest request) {
        Long userId = getUserIdByUserid(userid);
        cancelPayment(userId, orderId, request);
    }

    /**
     * 5. 결제 단건 조회 (userid 기반)
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByUserid(String userid, String orderId) {
        Long userId = getUserIdByUserid(userid);
        return getPayment(userId, orderId);
    }

    /**
     * 6. 내 결제 목록 조회 (userid 기반)
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPaymentsByUserid(String userid, RoleType roleType, Pageable pageable) {
        Long userId = getUserIdByUserid(userid);
        return getMyPayments(userId, roleType, pageable);
    }

    /**
     * 7. 결제 취소 이력 조회 (userid 기반)
     */
    @Transactional(readOnly = true)
    public List<PaymentCancelResponse> getPaymentCancelsByUserid(String userid, String orderId) {
        Long userId = getUserIdByUserid(userid);
        return getPaymentCancels(userId, orderId);
    }
}
