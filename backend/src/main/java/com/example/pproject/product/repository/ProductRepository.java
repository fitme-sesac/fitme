package com.example.pproject.product.repository;

import com.example.pproject.Constant.ProductType;
import com.example.pproject.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 활성 상품 코드로 조회 (삭제된 상품 제외)
    Optional<Product> findByProductCodeAndDeletedAtIsNull(String productCode);

    // 활성 상품 ID로 조회 (필드명 productId에 맞춤)
    Optional<Product> findByProductIdAndDeletedAtIsNull(Long productId);

    // 활성 상품 목록 조회
    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);

    // 상품 타입별 조회 (삭제된 상품 제외)
    Page<Product> findAllByProductTypeAndDeletedAtIsNull(ProductType productType, Pageable pageable);

    // 상품 코드 존재 여부 (삭제된 상품 포함)
    boolean existsByProductCode(String productCode);
}
