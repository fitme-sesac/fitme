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

    /**
     * Create a one-time (non-subscription) product.
     *
     * @param request the validated DTO containing details for the one-time product to create
     * @return the ID of the created product
     */
    @PostMapping("/one-time")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> createOneTime(@RequestBody @Valid CreateOneTimeRequest request) {
        Long productId = productService.createOneTime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    /**
     * Creates a subscription product from the provided request.
     *
     * @param request DTO containing subscription product details
     * @return the ID of the created subscription product
     */
    @PostMapping("/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> createSubscription(@RequestBody @Valid CreateSubscriptionRequest request) {
        Long productId = productService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    /**
     * Retrieves a product by its identifier for authenticated users.
     *
     * @param productId the ID of the product to retrieve
     * @return the product's details as a ProductResponse
     */
    @GetMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    /**
     * Retrieves a paginated list of products available to the authenticated user.
     *
     * @param pageable pagination and sorting information; defaults to size=10 and sort by `createdAt` descending
     * @return a page of ProductResponse objects representing the requested product slice
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    /**
     * Deletes the product identified by the given ID.
     *
     * @param productId the identifier of the product to delete
     * @return a ResponseEntity with HTTP 204 No Content when deletion succeeds
     */
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Pauses the product with the given ID.
     *
     * Sets the product's state to paused; this operation is restricted to administrators.
     *
     * @param productId the ID of the product to pause
     */
    @PatchMapping("/{productId}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> pauseProduct(@PathVariable Long productId) {
        productService.pauseProduct(productId);
        return ResponseEntity.ok().build();
    }

    /**
     * Resumes a paused product so it becomes active again.
     *
     * @param productId the identifier of the product to resume
     * @return a ResponseEntity with HTTP 200 OK and an empty body
     */
    @PatchMapping("/{productId}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resumeProduct(@PathVariable Long productId) {
        productService.resumeProduct(productId);
        return ResponseEntity.ok().build();
    }

    /**
     * Stops the specified product, transitioning it to a finished/inactive state.
     *
     * @param productId the identifier of the product to stop
     * @return HTTP 200 OK with an empty response body
     */
    @PatchMapping("/{productId}/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> stopProduct(@PathVariable Long productId) {
        productService.stopProduct(productId);
        return ResponseEntity.ok().build();
    }
}