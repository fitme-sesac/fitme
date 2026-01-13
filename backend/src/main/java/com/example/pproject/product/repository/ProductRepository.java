package com.example.pproject.product.repository;

import com.example.pproject.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 상품 코드로 조회 (중복 체크 등에 사용)
    Optional<Product> findByProductCode(String productCode);

    // exists 메서드 (Soft Delete된 데이터는 조회 안 됨 - @Where 영향 받음)
    boolean existsByProductCode(String productCode);

    // Soft Delete된 데이터까지 포함하여 중복 체크 (Native Query 사용)
    @Query(value = "SELECT count(*) > 0 FROM product WHERE product_code = :productCode", nativeQuery = true)
    boolean existsByProductCodeIncludeDeleted(@Param("productCode") String productCode);
}
