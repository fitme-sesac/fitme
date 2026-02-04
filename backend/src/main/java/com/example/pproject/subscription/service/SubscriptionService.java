package com.example.pproject.subscription.service;

import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.product.entity.Product;
import com.example.pproject.product.repository.ProductRepository;
import com.example.pproject.subscription.dto.SubscriptionBillingInfoUpdateRequest;
import com.example.pproject.subscription.dto.SubscriptionCreateRequest;
import com.example.pproject.subscription.dto.SubscriptionProductUpdateRequest;
import com.example.pproject.subscription.dto.SubscriptionResponse;
import com.example.pproject.subscription.entity.Subscription;
import com.example.pproject.subscription.repository.SubscriptionRepository;
import com.example.pproject.payment.service.PaymentService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionService {

        private final SubscriptionRepository subscriptionRepository;
        private final EmployerRepository employerRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;
        private final EmployerMemberRepository employerMemberRepository;
        private final SubscriptionBillingCycleService subscriptionBillingCycleService;
        private final PaymentService paymentService;

        // =============================================================================================
        // [일반 유저 기능]
        // =============================================================================================

        /**
         * [일반 유저] 구독 생성 (신청)
         */
        @Transactional
        public SubscriptionResponse createSubscription(String userid, SubscriptionCreateRequest request) {
                Long userId = getUserIdByUserid(userid);
                validateEmployerMembership(userId, request.employerId());

                EmployerEntity employer = employerRepository.findById(request.employerId())
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));

                Product product = productRepository.findById(request.productId())
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

                String finalBillingKey = request.billingKey();
                String cardCompany = null;
                String cardNumber = null;

                log.info("Creating subscription: employerId={}, productId={}, authKey={}, customerKey={}",
                                request.employerId(), request.productId(), request.authKey(), request.customerKey());

                if (finalBillingKey == null || finalBillingKey.isBlank()) {
                        if (request.authKey() != null && !request.authKey().isBlank()) {
                                var billingResponse = paymentService
                                                .issueBillingKey(request.authKey(), request.customerKey());
                                finalBillingKey = billingResponse.billingKey();
                                if (billingResponse.card() != null) {
                                        cardCompany = billingResponse.card().issuerCode();
                                        cardNumber = billingResponse.card().number();
                                }
                        } else {
                                throw new IllegalArgumentException("결제 수단 등록 정보(billingKey or authKey)가 필요합니다.");
                        }
                }

                Subscription subscription = Subscription.create(
                                employer,
                                product,
                                request.customerKey(),
                                finalBillingKey);

                // 카드 정보가 있으면 업데이트
                if (cardCompany != null && cardNumber != null) {
                        subscription.updateBillingInfo(finalBillingKey, request.customerKey(), cardCompany, cardNumber);
                }

                subscription.validateBillingInfo();
                subscription.validateDateConsistency();

                Subscription saved = subscriptionRepository.save(subscription);

                // 가입 즉시 첫 달 결제 시도
                subscriptionBillingCycleService.processMonthlyPayment(saved.getSubscriptionId());

                return SubscriptionResponse.from(saved);
        }

        /**
         * [일반 유저] 내 구독 상세 조회
         */
        public SubscriptionResponse getSubscription(String userid, Long subscriptionId) {
                Long userId = getUserIdByUserid(userid);
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                validateSubscriptionOwnership(userId, subscription);
                return SubscriptionResponse.from(subscription);
        }

        /**
         * [일반 유저] 내 구독 목록 조회
         */
        public List<SubscriptionResponse> getSubscriptionsByEmployer(String userid, Long employerId) {
                Long userId = getUserIdByUserid(userid);
                validateEmployerMembership(userId, employerId);

                return subscriptionRepository.findAllByEmployer_Id(employerId).stream()
                                .map(SubscriptionResponse::from)
                                .collect(Collectors.toList());
        }

        /**
         * [일반 유저] 구독 취소 (해지 예약)
         */
        @Transactional
        public void cancelSubscription(String userid, Long subscriptionId) {
                Long userId = getUserIdByUserid(userid);
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                validateSubscriptionOwnership(userId, subscription);

                // 해지 예약 (다음 결제일에 종료)
                if (subscription.getNextBillingAt() != null) {
                        subscription.scheduleCancellation(subscription.getNextBillingAt());
                } else {
                        // 다음 결제일이 없으면 즉시 종료 (예외적 상황)
                        subscription.cancel();
                }
        }

        /**
         * [일반 유저] 구독 재개 (결제 실패 등으로 중단된 경우)
         */
        @Transactional
        public void resumeSubscription(String userid, Long subscriptionId) {
                Long userId = getUserIdByUserid(userid);
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                validateSubscriptionOwnership(userId, subscription);

                // 해지 예약 취소 (구독 유지)
                if (subscription.getStatus() == SubscriptionStatus.ACTIVE && subscription.getEndedAt() != null) {
                        subscription.revokeCancellation();
                } else {
                        // 결제 실패 상태에서 복구
                        subscription.resume();
                }
        }

        /**
         * [일반 유저] 결제 수단 변경/등록
         */
        @Transactional
        public void updateBillingInfo(String userid, Long subscriptionId,
                        SubscriptionBillingInfoUpdateRequest request) {
                Long userId = getUserIdByUserid(userid);
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                validateSubscriptionOwnership(userId, subscription);

                String newBillingKey = request.billingKey();
                String newCardCompany = request.cardCompany();
                String newCardNumber = request.cardNumber();

                if (newBillingKey == null || newBillingKey.isBlank()) {
                        if (request.authKey() != null && !request.authKey().isBlank()) {
                                var billingResponse = paymentService.issueBillingKey(request.authKey(),
                                                request.customerKey());
                                newBillingKey = billingResponse.billingKey();
                                if (billingResponse.card() != null) {
                                        newCardCompany = billingResponse.card().issuerCode(); // 카드사 코드 (예: 61)
                                        newCardNumber = billingResponse.card().number();
                                }
                        } else {
                                throw new IllegalArgumentException("결제 수단 변경을 위해 billingKey 또는 authKey가 필요합니다.");
                        }
                }

                subscription.updateBillingInfo(
                                newBillingKey,
                                request.customerKey(),
                                newCardCompany,
                                newCardNumber);

                subscription.validateBillingInfo();
                subscription.validateCardInfo();
        }

        /**
         * [일반 유저] 구독 상품 변경 예약 (다음 결제일부터 적용)
         */
        @Transactional
        public void scheduleProductChange(String userid, Long subscriptionId,
                        SubscriptionProductUpdateRequest request) {
                Long userId = getUserIdByUserid(userid);
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                validateSubscriptionOwnership(userId, subscription);
                Product newProduct = productRepository.findById(request.newProductId())
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

                subscription.scheduleProductChange(newProduct);
        }

        /**
         * [일반 유저] 예약된 상품 변경 정보 조회
         */
        public SubscriptionResponse getScheduledProductChange(String userid, Long subscriptionId) {
                Long userId = getUserIdByUserid(userid);
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                validateSubscriptionOwnership(userId, subscription);
                return SubscriptionResponse.from(subscription);
        }

        /**
         * [일반 유저] 예약된 상품 변경 취소
         */
        @Transactional
        public void cancelScheduledProductChange(String userid, Long subscriptionId) {
                Long userId = getUserIdByUserid(userid);
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                validateSubscriptionOwnership(userId, subscription);
                subscription.cancelScheduledProductChange();
        }

        // =============================================================================================
        // [관리자/시스템 기능]
        // =============================================================================================

        /**
         * [관리자] 모든 구독 목록 조회
         */
        public List<SubscriptionResponse> getAllSubscriptions() {
                return subscriptionRepository.findAll().stream()
                                .map(SubscriptionResponse::from)
                                .collect(Collectors.toList());
        }

        /**
         * [관리자] 구독 상태 강제 변경
         */
        @Transactional
        public void updateSubscriptionStatus(Long subscriptionId, SubscriptionStatus newStatus) {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));
                subscription.updateStatus(newStatus);
        }

        /**
         * [관리자] 구독 강제 활성화
         */
        @Transactional
        public void activateSubscription(Long subscriptionId) {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                subscription.activate();
        }

        /**
         * [시스템/배치] 예약된 상품 변경 적용
         */
        @Transactional
        public void applyScheduledProductChange(Long subscriptionId) {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                subscription.applyScheduledProductChange();
        }

        /**
         * [시스템/배치] 결제 실패 처리
         */
        @Transactional
        public void markPaymentFailed(Long subscriptionId) {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                subscription.markPaymentFailed();
        }

        /**
         * [시스템/배치] 다음 결제일 스케줄링
         */
        @Transactional
        public void scheduleNextBilling(Long subscriptionId) {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                subscription.scheduleNextBilling();
        }

        /**
         * [시스템/배치] 결제 가능 상태 확인
         */
        public void validateBillableState(Long subscriptionId) {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

                subscription.validateBillableState();
        }

        // =============================================================================================
        // [Private Helper Methods]
        // =============================================================================================

        private Long getUserIdByUserid(String userid) {
                return userRepository.findByUserid(userid)
                                .map(UserEntity::getId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        }

        private void validateEmployerMembership(Long userId, Long employerId) {
                boolean isMember = employerMemberRepository.existsByEmployerIdAndMemberIdAndActiveTrue(employerId,
                                userId);
                if (!isMember) {
                        throw new IllegalArgumentException("해당 기업의 구독 관리 권한이 없습니다.");
                }
        }

        private void validateSubscriptionOwnership(Long userId, Subscription subscription) {
                validateEmployerMembership(userId, subscription.getEmployer().getId());
        }
}
