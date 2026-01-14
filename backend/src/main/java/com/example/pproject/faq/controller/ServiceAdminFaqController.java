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
     * FAQ 목록 조회 (관리자)
     * - 모든 FAQ 조회 (삭제되지 않은 것만)
     * - 키워드 검색 가능
     * - 공개/비공개 필터링
     *
     * GET /api/admin/faqs?keyword=검색어&isPublic=true&page=0&size=10
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
     * FAQ 상세 조회
     *
     * GET /api/admin/faqs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FaqResponse> getFaqDetail(@PathVariable Long id) {
        log.info("FAQ 상세 조회 - ID: {}", id);
        FaqResponse faq = faqService.getFaqDetail(id);
        return ResponseEntity.ok(faq);
    }

    /**
     * FAQ 신규 등록
     *
     * POST /api/admin/faqs
     * {
     *   "question": "질문",
     *   "answer": "답변",
     *   "isPublic": true,
     *   "locked": true
     * }
     */
    @PostMapping
    public ResponseEntity<FaqResponse> createFaq(@Valid @RequestBody CreateFaqRequest request) {
        log.info("FAQ 신규 등록 - question: {}", request.getQuestion());
        FaqResponse createdFaq = faqService.createFaq(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFaq);
    }

    /**
     * FAQ 수정
     *
     * PUT /api/admin/faqs/{id}
     * {
     *   "question": "수정된 질문",
     *   "answer": "수정된 답변",
     *   "isPublic": true
     * }
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
     * FAQ 삭제 (논리 삭제)
     *
     * DELETE /api/admin/faqs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        log.info("FAQ 삭제 - ID: {}", id);
        faqService.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }
}