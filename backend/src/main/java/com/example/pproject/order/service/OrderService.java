package com.example.pproject.order.service;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.common.vo.Money;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.order.entity.Orders;
import com.example.pproject.order.repository.OrderRepository;
import com.example.pproject.product.entity.Product;
import com.example.pproject.product.repository.ProductRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final ProductRepository productRepository;

    /**
     * 주문 생성
     *
     * @param buyerId        구매자 ID (PK)
     * @param buyerType      구매자 타입 (CANDIDATE / EMPLOYER)
     * @param productCode    상품 코드
     * @param idempotencyKey 멱등성 키 (중복 주문 방지)
     * @return 생성된(또는 기존) 주문 엔티티
     */
    @Transactional
    public Orders createOrder(Long buyerId, BuyerType buyerType, String productCode, String idempotencyKey) {
        // 1. 멱등성 검사: 이미 처리된 요청이면 기존 주문 반환
        if (idempotencyKey != null) {
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseGet(() -> createNewOrder(buyerId, buyerType, productCode, idempotencyKey));
        }
        return createNewOrder(buyerId, buyerType, productCode, null);
    }

    private Orders createNewOrder(Long buyerId, BuyerType buyerType, String productCode, String idempotencyKey) {
        // 2. 상품 조회 (삭제된 상품 제외)
        Product product = productRepository.findByProductCodeAndDeletedAtIsNull(productCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 3. 구매자 조회 및 주문 생성
        UserEntity buyer = null;
        EmployerEntity employer = null;

        if (buyerType == BuyerType.MEMBER) {
            buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        } else if (buyerType == BuyerType.EMPLOYER) {
            EmployerMemberEntity em = employerMemberRepository.findFirstByMemberIdAndActiveTrue(buyerId)
                    .orElseThrow(() -> new IllegalArgumentException("소속된 기업이 없습니다."));
            employer = employerRepository.findByIdAndDeletedAtIsNull(em.getEmployerId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 회원입니다."));
        } else {
            throw new IllegalArgumentException("지원하지 않는 구매자 타입입니다.");
        }

        // 4. 주문 엔티티 생성 (팩토리 메서드 활용)
        Orders order = Orders.createOrder(
                UUID.randomUUID(),
                buyerType,
                buyer,
                employer,
                product,
                product.getPrice(), // 상품 가격을 주문 금액으로 설정
                idempotencyKey);

        return orderRepository.save(order);
    }

    /**
     * 주문 조회
     *
     * @param orderUid 주문 고유 번호
     * @param userId   요청자 ID (권한 검증용)
     * @return 주문 엔티티
     */
    public Orders getOrder(UUID orderUid, Long userId) {
        Orders order = orderRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 권한 검증
        validateOrderOwner(order, userId);

        return order;
    }

    /**
     * 주문 완료 처리 (결제 성공 시 호출)
     *
     * @param orderUid      주문 고유 번호
     * @param paymentAmount 결제된 금액
     */
    @Transactional
    public void completeOrder(UUID orderUid, Money paymentAmount) {
        Orders order = orderRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 금액 검증
        order.validatePaymentAmount(paymentAmount);

        // 상태 변경 (Dirty Checking)
        order.complete();
    }

    /**
     * 주문 취소
     *
     * @param orderUid 주문 고유 번호
     * @param userId   요청자 ID (권한 검증용)
     */
    @Transactional
    public void cancelOrder(UUID orderUid, Long userId) {
        Orders order = orderRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 권한 검증
        validateOrderOwner(order, userId);

        // 취소 처리
        order.cancel();
    }

    /**
     * 결제 실패 처리
     *
     * @param orderUid 주문 고유 번호
     */
    @Transactional
    public void failOrder(UUID orderUid) {
        Orders order = orderRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        order.fail();
    }
    private void validateOrderOwner(Orders order, Long userId) {
        if (order.getBuyerType() == BuyerType.EMPLOYER) {
            boolean isMember = employerMemberRepository.existsByEmployerIdAndMemberIdAndActiveTrue(
                    order.getBuyerEmployer().getId(), userId);
            if (!isMember) {
                throw new IllegalStateException("본인의 기업 주문 내역만 접근할 수 있습니다.");
            }
        } else {
            order.validateOwner(userId);
        }
    }
}
