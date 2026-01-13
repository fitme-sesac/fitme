package com.example.pproject.product.controller;

import com.example.pproject.product.dto.CreateOneTimeRequest;
import com.example.pproject.product.dto.CreateSubscriptionRequest;
import com.example.pproject.product.dto.ProductResponse;
import com.example.pproject.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 1. 상품 생성 (단건) - 관리자 전용
    @PostMapping("/one-time")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> createOneTime(@RequestBody @Valid CreateOneTimeRequest request) {
        Long productId = productService.createOneTime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    // 2. 상품 생성 (구독) - 관리자 전용
    @PostMapping("/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> createSubscription(@RequestBody @Valid CreateSubscriptionRequest request) {
        Long productId = productService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    // 3. 상품 단건 조회 - 인증된 사용자 가능 (혹은 permitAll)
    @GetMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    // 4. 상품 전체 조회 (페이징) - 인증된 사용자 가능
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    // 5. 상품 삭제 - 관리자 전용
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // 6. 상태 변경 (일시 정지) - 관리자 전용
    @PatchMapping("/{productId}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> pauseProduct(@PathVariable Long productId) {
        productService.pauseProduct(productId);
        return ResponseEntity.ok().build();
    }

    // 7. 상태 변경 (재개) - 관리자 전용
    @PatchMapping("/{productId}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resumeProduct(@PathVariable Long productId) {
        productService.resumeProduct(productId);
        return ResponseEntity.ok().build();
    }

    // 8. 상태 변경 (종료) - 관리자 전용
    @PatchMapping("/{productId}/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> stopProduct(@PathVariable Long productId) {
        productService.stopProduct(productId);
        return ResponseEntity.ok().build();
    }
}
