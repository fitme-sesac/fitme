package com.example.pproject.product.service;

import com.example.pproject.common.vo.Money;
import com.example.pproject.product.dto.CreateOneTimeRequest;
import com.example.pproject.product.dto.CreateSubscriptionRequest;
import com.example.pproject.product.dto.ProductResponse;
import com.example.pproject.product.dto.UpdatePriceRequest;
import com.example.pproject.product.entity.Product;
import com.example.pproject.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 단건(크레딧) 상품 생성 로직
    @Transactional
    public Long createOneTime(CreateOneTimeRequest request) {
        // 중복 체크 (삭제된 데이터 포함)
        validateDuplicateCode(request.productCode());

        // Entity 생성 (Assert 검증 수행됨)
        Product product = request.toEntity();

        return productRepository.save(product).getProductId();
    }

    // 구독 상품 생성 로직
    @Transactional
    public Long createSubscription(CreateSubscriptionRequest request) {
        // 중복 체크 (삭제된 데이터 포함)
        validateDuplicateCode(request.productCode());

        // Entity 생성 (Assert 검증 수행됨 - planTier 없으면 에러)
        Product product = request.toEntity();

        return productRepository.save(product).getProductId();
    }

    // 상품 단건 조회 (삭제된 상품 제외)
    public ProductResponse getProduct(Long productId) {
        Product product = getProductById(productId);
        return ProductResponse.from(product);
    }

    // 모든 상품 조회 (삭제된 상품 제외, 페이징)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAllByDeletedAtIsNull(pageable)
                .map(ProductResponse::from);
    }

    // 내부용 엔티티 조회 메서드 (삭제된 상품 제외)
    public Product getProductById(Long productId) {
        return productRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
    }

    // 상품 삭제 (Soft Delete)
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = getProductById(productId);
        // Repository.delete() 대신 엔티티의 비즈니스 메서드 호출
        // Dirty Checking에 의해 트랜잭션 커밋 시 update 쿼리 발생
        product.delete(); 
    }

    // === 상태 변경 로직 ===

    // 상품 가격 변경
    @Transactional
    public void updatePrice(Long productId, UpdatePriceRequest request) {
        Product product = getProductById(productId);
        Money newPrice = request.toMoney();
        product.changePrice(newPrice);
    }

    // 상품 일시 정지
    @Transactional
    public void pauseProduct(Long productId) {
        Product product = getProductById(productId);
        product.pause();
    }

    // 상품 판매 재개
    @Transactional
    public void resumeProduct(Long productId) {
        Product product = getProductById(productId);
        product.resume();
    }

    // 상품 판매 종료
    @Transactional
    public void stopProduct(Long productId) {
        Product product = getProductById(productId);
        product.stop();
    }

    // 상품 코드 중복 체크 (삭제된 데이터 포함)
    private void validateDuplicateCode(String code) {
        if (productRepository.existsByProductCode(code)) {
            throw new IllegalArgumentException("이미 존재하는 상품 코드입니다.");
        }
    }
}
