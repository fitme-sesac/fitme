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
@Table(name = "orders")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orders extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_uid", nullable = false, updatable = false)
    private UUID orderUid;

    @Column(name = "buyer_type", nullable = false, length = 20)
    private RoleType buyerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_member_id")
    private UserEntity buyerMemberId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_employer_id")
    private EmployerEntity buyerEmployerId;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "order_amount")
    private Money orderAmount;
    
    @Column(name = "status")
    private OrderStatus status;
    
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
            builder.buyerMemberId(buyer);
        } else if (buyerType == RoleType.EMPLOYER) {
            Assert.notNull(employer, "기업 회원은 필수입니다.");
            builder.buyerEmployerId(employer);
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
            if (this.buyerMemberId == null || !this.buyerMemberId.getId().equals(userId)) {
                throw new IllegalStateException("본인의 주문 내역만 접근할 수 있습니다.");
            }
        } else if (this.buyerType == RoleType.EMPLOYER) {
            if (this.buyerEmployerId == null || !this.buyerEmployerId.getId().equals(userId)) {
                throw new IllegalStateException("본인의 기업 주문 내역만 접근할 수 있습니다.");
            }
        }
    }
}
