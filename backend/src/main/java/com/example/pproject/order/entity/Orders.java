package com.example.pproject.order.entity;

import com.example.pproject.Constant.OrderStatus;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.common.vo.Money;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.product.entity.Product;
import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_orders_uid", columnNames = "order_uid"),
        @UniqueConstraint(name = "uq_orders_idempotency_key", columnNames = "idempotency_key")
    }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orders extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId; // 주문 ID

    @Column(name = "order_uid", nullable = false, updatable = false)
    private UUID orderUid; // 주문 고유 번호

    @Column(name = "buyer_type", nullable = false, length = 20)
    private RoleType buyerType; // 구매자 타입

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_member_id")
    private UserEntity buyerMember; // 구매자(개인)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_employer_id")
    private EmployerEntity buyerEmployer; // 구매자(기업)
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product; // 상품 정보
    
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "order_amount", nullable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money orderAmount; // 주문 금액
    
    @Column(name = "status")
    private OrderStatus status; // 주문 상태
    
    @Column(name = "idempotency_key")
    private String idempotencyKey; // 멱등성 키

    // === 팩토리 메서드 ===

    /**
     * 주문 생성 (개인/기업 분기 처리 포함)
     */
    public static Orders createOrder(UUID orderUid, RoleType buyerType, UserEntity buyer, EmployerEntity employer, Money amount) {
        Assert.notNull(orderUid, "주문 UID는 필수입니다.");
        Assert.notNull(buyerType, "구매자 타입은 필수입니다.");
        Assert.notNull(amount, "주문 금액은 필수입니다.");

        OrdersBuilder builder = Orders.builder()
                .orderUid(orderUid)
                .buyerType(buyerType)
                .orderAmount(amount)
                .status(OrderStatus.CREATED);

        if (buyerType == RoleType.CANDIDATE) {
            Assert.notNull(buyer, "개인 회원은 필수입니다.");
            builder.buyerMember(buyer);
        } else if (buyerType == RoleType.EMPLOYER) {
            Assert.notNull(employer, "기업 회원은 필수입니다.");
            builder.buyerEmployer(employer);
        } else {
            throw new IllegalArgumentException("지원하지 않는 구매자 타입입니다.");
        }

        return builder.build();
    }

    // === 비즈니스 로직 ===

    /**
     * 주문 소유자 검증
     * @param userId 요청한 사용자의 ID (PK)
     * @throws IllegalStateException 본인의 주문이 아닐 경우 예외 발생
     */
    public void validateOwner(Long userId) {
        if (this.buyerType == RoleType.CANDIDATE) {
            if (this.buyerMember == null || !this.buyerMember.getId().equals(userId)) {
                throw new IllegalStateException("본인의 주문 내역만 접근할 수 있습니다.");
            }
        } else if (this.buyerType == RoleType.EMPLOYER) {
            if (this.buyerEmployer == null || !this.buyerEmployer.getId().equals(userId)) {
                throw new IllegalStateException("본인의 기업 주문 내역만 접근할 수 있습니다.");
            }
        }
    }

    /**
     * 결제 금액 검증
     * 결제 요청 금액이 주문 금액과 일치하는지 확인합니다.
     */
    public void validatePaymentAmount(Money paymentAmount) {
        if (paymentAmount == null || !this.orderAmount.equals(paymentAmount)) {
            throw new IllegalStateException(
                    String.format("주문 금액(%s)과 결제 금액(%s)이 일치하지 않습니다.",
                            this.orderAmount, paymentAmount)
            );
        }
    }

    /**
     * 주문 완료 처리 (결제 성공 시)
     */
    public void complete() {
        if (this.status == OrderStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 주문은 완료 처리할 수 없습니다.");
        }
        // 이미 완료된 경우 멱등성 보장을 위해 무시하거나 예외 처리
        if (this.status == OrderStatus.PAID) {
            return;
        }
        this.status = OrderStatus.PAID;
    }

    /**
     * 주문 취소 처리
     */
    public void cancel() {
        if (this.status == OrderStatus.PAID) {
            throw new IllegalStateException("이미 완료된 주문은 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCELED;
    }
}
