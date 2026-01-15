package com.example.pproject.product.repository;

import com.example.pproject.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    /**
 * Finds a product with the given product code.
 *
 * @param productCode the exact product code to search for
 * @return an Optional containing the matching Product if found, or an empty Optional if none exists
 */
    Optional<Product> findByProductCode(String productCode);

    /**
 * Determines whether an active (non–soft-deleted) product exists with the given product code.
 *
 * @param productCode the product's unique code to check for existence
 * @return true if at least one non–soft-deleted product has the specified product code, false otherwise
 */
    boolean existsByProductCode(String productCode);

    /**
     * Check whether any product exists with the given product code, including records marked as soft-deleted.
     *
     * This query is executed as a native SQL statement and therefore bypasses entity-level soft-delete filters.
     *
     * @param productCode the product code to check for
     * @return `true` if at least one product with the given code exists (including soft-deleted rows), `false` otherwise
     */
    @Query(value = "SELECT count(*) > 0 FROM product WHERE product_code = :productCode", nativeQuery = true)
    boolean existsByProductCodeIncludeDeleted(@Param("productCode") String productCode);
}