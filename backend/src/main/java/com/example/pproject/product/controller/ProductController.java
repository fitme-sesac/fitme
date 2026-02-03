package com.example.pproject.product.controller;

import com.example.pproject.Constant.ProductType;
import com.example.pproject.product.dto.ProductResponse;
import com.example.pproject.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 1. 상품 단건 조회 - 인증된 사용자 가능
    @GetMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    // 2. 상품 전체 조회 (페이징) - 인증된 사용자 가능
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(required = false) ProductType type,
            @PageableDefault(size = 10, sort = "price.amount", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(type, pageable));
    }
}
