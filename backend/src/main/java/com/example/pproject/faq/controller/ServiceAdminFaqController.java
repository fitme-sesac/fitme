package com.example.pproject.faq.controller;

import com.example.pproject.faq.dto.CreateFaqRequest;
import com.example.pproject.faq.dto.FaqResponse;
import com.example.pproject.faq.dto.UpdateFaqRequest;
import com.example.pproject.faq.service.FaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * FAQ 관리 API (SERVICEADMIN)
 * - GET /api/admin/faqs - FAQ 목록 조회
 * - POST /api/admin/faqs - FAQ 신규 등록
 * - PUT /api/admin/faqs/{id} - FAQ 수정
 * - DELETE /api/admin/faqs/{id} - FAQ 삭제
 */
@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ServiceAdminFaqController {

    private final FaqService faqService;

    /**
     * Retrieve a paginated list of FAQs for administrative use.
     *
     * The result includes only FAQs that have not been deleted and can be filtered by
     * a keyword and by public visibility.
     *
     * @param keyword an optional search term to filter FAQs by question or content
     * @param isPublic an optional filter; `true` for public FAQs, `false` for private FAQs, or `null` for both
     * @param page the zero-based page index to retrieve
     * @param size the number of items per page
     * @return a page of FaqResponse objects matching the provided filters
     */
    @GetMapping
    public ResponseEntity<Page<FaqResponse>> getFaqList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("FAQ 목록 조회 - keyword: {}, isPublic: {}, page: {}, size: {}",
                keyword, isPublic, page, size);

        Page<FaqResponse> result = faqService.getFaqList(keyword, isPublic, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieve detailed information for a specific FAQ.
     *
     * @param id the ID of the FAQ to retrieve
     * @return the FAQ details as a {@link FaqResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FaqResponse> getFaqDetail(@PathVariable Long id) {
        log.info("FAQ 상세 조회 - ID: {}", id);
        FaqResponse faq = faqService.getFaqDetail(id);
        return ResponseEntity.ok(faq);
    }

    /**
     * Create a new FAQ entry.
     *
     * @param request the validated request containing `question`, `answer`, `isPublic`, and `locked` fields
     * @return the created FAQ as a FaqResponse
     */
    @PostMapping
    public ResponseEntity<FaqResponse> createFaq(@Valid @RequestBody CreateFaqRequest request) {
        log.info("FAQ 신규 등록 - question: {}", request.getQuestion());
        FaqResponse createdFaq = faqService.createFaq(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFaq);
    }

    /**
     * Update an existing FAQ entry.
     *
     * Updates the FAQ identified by the given ID using the values in the request.
     *
     * @param id      the ID of the FAQ to update
     * @param request the update payload containing new question, answer, and visibility
     * @return        the updated FAQ representation
     */
    @PutMapping("/{id}")
    public ResponseEntity<FaqResponse> updateFaq(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFaqRequest request) {

        log.info("FAQ 수정 - ID: {}", id);
        FaqResponse updatedFaq = faqService.updateFaq(id, request);
        return ResponseEntity.ok(updatedFaq);
    }

    /**
     * Performs a logical delete of an FAQ identified by the given ID.
     *
     * @param id the ID of the FAQ to delete
     * @return a ResponseEntity with HTTP 204 No Content when the FAQ is deleted
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        log.info("FAQ 삭제 - ID: {}", id);
        faqService.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }
}