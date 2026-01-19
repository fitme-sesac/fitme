package com.example.pproject.faq.controller;

import com.example.pproject.faq.dto.FaqCreateRequest;
import com.example.pproject.faq.dto.FaqListResponse;
import com.example.pproject.faq.dto.FaqResponse;
import com.example.pproject.faq.dto.FaqUpdateRequest;
import com.example.pproject.faq.service.FaqService;
import com.example.pproject.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
@Slf4j
public class FaqController {

    private final FaqService faqService;

    /**
     * ADM-FAQ-001: FAQ 목록 조회
     * GET /api/admin/faqs
     * 쿼리 파라미터: is_public (true/false), keyword (질문 검색)
     */
    @GetMapping("/api/admin/faqs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<FaqListResponse>>> getFaqList(
            @RequestParam(value = "is_public", required = false) Boolean isPublic,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("FAQ 목록 조회 요청 - isPublic: {}, keyword: {}, page: {}, size: {}",
                isPublic, keyword, page, size);

        Page<FaqListResponse> faqs = faqService.getFaqList(isPublic, keyword, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("FAQ 목록 조회 성공", faqs)
        );
    }

    /**
     * ADM-FAQ-002: FAQ 신규 등록
     * POST /api/admin/faqs
     * body: { "question": "질문", "answer": "답변", "is_public": true, "locked": true }
     */
    @PostMapping("/api/admin/faqs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FaqResponse>> createFaq(
            @Valid @RequestBody FaqCreateRequest request) {

        log.info("FAQ 생성 요청 - question: {}", request.getQuestion());

        FaqResponse response = faqService.createFaq(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("FAQ 생성 성공", response));
    }

    /**
     * FAQ 상세 조회
     * GET /api/admin/faqs/{id}
     */
    @GetMapping("/api/admin/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FaqResponse>> getFaqDetail(
            @PathVariable Long id) {

        log.info("FAQ 상세 조회 요청 - ID: {}", id);

        FaqResponse response = faqService.getFaqDetail(id);

        return ResponseEntity.ok(
                ApiResponse.success("FAQ 상세 조회 성공", response)
        );
    }

    /**
     * ADM-FAQ-003: FAQ 수정
     * PUT /api/admin/faqs/{id}
     * body: { "question": "수정 질문", "answer": "수정 답변", "is_public": true }
     */
    @PutMapping("/api/admin/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FaqResponse>> updateFaq(
            @PathVariable Long id,
            @Valid @RequestBody FaqUpdateRequest request) {

        log.info("FAQ 수정 요청 - ID: {}, question: {}", id, request.getQuestion());

        FaqResponse response = faqService.updateFaq(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("FAQ 수정 성공", response)
        );
    }

    /**
     * ADM-FAQ-004: FAQ 삭제
     * DELETE /api/admin/faqs/{id}
     * Path: id (논리 삭제를 위해 deleted_at 필드 업데이트)
     */
    @DeleteMapping("/api/admin/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFaq(
            @PathVariable Long id) {

        log.info("FAQ 삭제 요청 - ID: {}", id);

        faqService.deleteFaq(id);

        return ResponseEntity.ok(
                ApiResponse.success("FAQ 삭제 성공", null)
        );
    }

    /**
     * 공개 FAQ 목록 조회 (사용자용)
     * GET /api/v1/faqs/public
     */
    @GetMapping("/api/v1/faqs/public/list")
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getPublicFaqList() {

        log.info("공개 FAQ 목록 조회 요청");

        List<FaqResponse> faqs = faqService.getPublicFaqList();

        return ResponseEntity.ok(
                ApiResponse.success("공개 FAQ 목록 조회 성공", faqs)
        );
    }
}