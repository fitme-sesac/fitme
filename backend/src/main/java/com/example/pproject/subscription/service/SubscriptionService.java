package com.example.pproject.subscription.service;

import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.product.entity.Product;
import com.example.pproject.product.repository.ProductRepository;
import com.example.pproject.subscription.dto.SubscriptionBillingInfoUpdateRequest;
import com.example.pproject.subscription.dto.SubscriptionCreateRequest;
import com.example.pproject.subscription.dto.SubscriptionProductUpdateRequest;
import com.example.pproject.subscription.dto.SubscriptionResponse;
import com.example.pproject.subscription.entity.Subscription;
import com.example.pproject.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final EmployerRepository employerRepository;
    private final ProductRepository productRepository;

    // =============================================================================================
    // [일반 유저 기능]
    // =============================================================================================

    /**
     * [일반 유저] 구독 생성 (신청)
     */
    @Transactional
    public SubscriptionResponse createSubscription(SubscriptionCreateRequest request) {
        EmployerEntity employer = employerRepository.findById(request.employerId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        Subscription subscription = Subscription.create(
                employer,
                product,
                request.customerKey(),
                request.billingKey()
        );

        subscription.validateBillingInfo();
        subscription.validateDateConsistency();

        Subscription saved = subscriptionRepository.save(subscription);
        return SubscriptionResponse.from(saved);
    }

    /**
     * [일반 유저] 내 구독 상세 조회
     */
    public SubscriptionResponse getSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));
        return SubscriptionResponse.from(subscription);
    }

    /**
     * [일반 유저] 내 구독 목록 조회
     */
    public List<SubscriptionResponse> getSubscriptionsByEmployer(Long employerId) {
        return subscriptionRepository.findAllByEmployer_Id(employerId).stream()
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * [일반 유저] 구독 취소 (해지 예약)
     */
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

        subscription.cancel();
    }

    /**
     * [일반 유저] 구독 재개 (결제 실패 등으로 중단된 경우)
     */
    @Transactional
    public void resumeSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

        subscription.resume();
    }

    /**
     * [일반 유저] 결제 수단 변경/등록
     */
    @Transactional
    public void updateBillingInfo(Long subscriptionId, SubscriptionBillingInfoUpdateRequest request) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

        subscription.updateBillingInfo(
                request.billingKey(),
                request.customerKey(),
                request.cardCompany(),
                request.cardNumber()
        );
        
        subscription.validateBillingInfo();
        subscription.validateCardInfo();
    }

    /**
     * [일반 유저] 구독 상품 변경 예약 (다음 결제일부터 적용)
     */
    @Transactional
    public void scheduleProductChange(Long subscriptionId, SubscriptionProductUpdateRequest request) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));

        Product newProduct = productRepository.findById(request.newProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        subscription.scheduleProductChange(newProduct);
    }

    /**
     * [일반 유저] 예약된 상품 변경 정보 조회
     */
    public SubscriptionResponse getScheduledProductChange(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));
        return SubscriptionResponse.from(subscription);
    }

    /**
     * [일반 유저] 예약된 상품 변경 취소
     */
    @Transactional
    public void cancelScheduledProductChange(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독상품입니다."));
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
}
