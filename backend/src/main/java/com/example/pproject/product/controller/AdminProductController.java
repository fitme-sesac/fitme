package com.example.pproject.product.controller;

import com.example.pproject.product.dto.CreateOneTimeRequest;
import com.example.pproject.product.dto.CreateSubscriptionRequest;
import com.example.pproject.product.dto.UpdatePriceRequest;
import com.example.pproject.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // 클래스 레벨에서 관리자 권한 체크
public class AdminProductController {

    private final ProductService productService;

    // 1. 상품 생성 (단건)
    @PostMapping("/one-time")
    public ResponseEntity<Long> createOneTime(@RequestBody @Valid CreateOneTimeRequest request) {
        Long productId = productService.createOneTime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    // 2. 상품 생성 (구독)
    @PostMapping("/subscription")
    public ResponseEntity<Long> createSubscription(@RequestBody @Valid CreateSubscriptionRequest request) {
        Long productId = productService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    // 3. 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // 4. 가격 변경
    @PatchMapping("/{productId}/price")
    public ResponseEntity<Void> updatePrice(@PathVariable Long productId, @RequestBody @Valid UpdatePriceRequest request) {
        productService.updatePrice(productId, request);
        return ResponseEntity.ok().build();
    }

    // 5. 상태 변경 (일시 정지)
    @PatchMapping("/{productId}/pause")
    public ResponseEntity<Void> pauseProduct(@PathVariable Long productId) {
        productService.pauseProduct(productId);
        return ResponseEntity.ok().build();
    }

    // 6. 상태 변경 (재개)
    @PatchMapping("/{productId}/resume")
    public ResponseEntity<Void> resumeProduct(@PathVariable Long productId) {
        productService.resumeProduct(productId);
        return ResponseEntity.ok().build();
    }

    // 7. 상태 변경 (종료)
    @PatchMapping("/{productId}/stop")
    public ResponseEntity<Void> stopProduct(@PathVariable Long productId) {
        productService.stopProduct(productId);
        return ResponseEntity.ok().build();
    }
}
