package com.example.pproject.product.service;

import com.example.pproject.product.dto.CreateOneTimeRequest;
import com.example.pproject.product.dto.CreateSubscriptionRequest;
import com.example.pproject.product.dto.ProductResponse;
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

    /**
     * Create and persist a one-time (credit) product from the given request.
     *
     * @param request the request containing product data for a one-time product
     * @return the generated productId of the persisted product
     * @throws IllegalArgumentException if a product with the same product code already exists, including logically deleted records
     */
    @Transactional
    public Long createOneTime(CreateOneTimeRequest request) {
        // 중복 체크 (삭제된 데이터 포함)
        validateDuplicateCode(request.productCode());

        // Entity 생성 (Assert 검증 수행됨)
        Product product = request.toEntity();

        return productRepository.save(product).getProductId();
    }

    /**
     * Creates and persists a subscription product and returns its generated id.
     *
     * Performs a uniqueness check for the product code (including logically deleted records)
     * and requires request fields necessary for a subscription product (for example, a valid plan tier).
     *
     * @param request the subscription product creation request containing product data (must include a valid plan tier and a unique product code)
     * @return the generated productId of the persisted Product
     */
    @Transactional
    public Long createSubscription(CreateSubscriptionRequest request) {
        // 중복 체크 (삭제된 데이터 포함)
        validateDuplicateCode(request.productCode());

        // Entity 생성 (Assert 검증 수행됨 - planTier 없으면 에러)
        Product product = request.toEntity();

        return productRepository.save(product).getProductId();
    }

    /**
     * Retrieve the product identified by the given id and convert it to a ProductResponse.
     *
     * @param productId the identifier of the product to retrieve
     * @return the ProductResponse representing the requested product
     * @throws IllegalArgumentException if no product exists with the given id
     */
    public ProductResponse getProduct(Long productId) {
        Product product = getProductById(productId);
        return ProductResponse.from(product);
    }

    /**
     * Retrieve all products using the supplied pagination and sorting information.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of ProductResponse objects representing stored products
     */
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::from);
    }

    /**
     * Retrieve the Product entity identified by the given id.
     *
     * @param productId the id of the product to retrieve
     * @return the Product with the specified id
     * @throws IllegalArgumentException if no product exists with the given id
     */
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
    }

    /**
     * Marks the product with the given id as deleted (soft delete) by invoking the entity's delete operation.
     *
     * @param productId the id of the product to soft-delete
     */
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = getProductById(productId);
        // Repository.delete() 대신 엔티티의 비즈니스 메서드 호출
        // Dirty Checking에 의해 트랜잭션 커밋 시 update 쿼리 발생
        product.delete(); 
    }

    // === 상태 변경 로직 ===

    /**
     * Pause the product identified by the given id, transitioning its state to paused.
     *
     * @param productId the id of the product to pause
     * @throws IllegalArgumentException if no product exists with the given id
     */
    @Transactional
    public void pauseProduct(Long productId) {
        Product product = getProductById(productId);
        product.pause();
    }

    /**
     * Resumes selling for the product with the specified id.
     *
     * @param productId the id of the product to resume selling
     * @throws IllegalArgumentException if no product exists with the given id
     */
    @Transactional
    public void resumeProduct(Long productId) {
        Product product = getProductById(productId);
        product.resume();
    }

    /**
     * Transitions the specified product into the stopped state so it is no longer sellable.
     *
     * @param productId the identifier of the product to stop
     * @throws IllegalArgumentException if no product exists for the given id
     */
    @Transactional
    public void stopProduct(Long productId) {
        Product product = getProductById(productId);
        product.stop();
    }

    /**
     * Validates that no product exists with the given product code, including logically deleted records.
     *
     * @param code the product code to check for duplicates
     * @throws IllegalArgumentException if a product with the same code already exists (including deleted records)
     */
    private void validateDuplicateCode(String code) {
        if (productRepository.existsByProductCodeIncludeDeleted(code)) {
            throw new IllegalArgumentException("이미 존재하는 상품 코드입니다.");
        }
    }
}