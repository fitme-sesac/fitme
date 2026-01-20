package com.example.pproject.faq.controller;

import com.example.pproject.faq.dto.FaqCreateRequest;
import com.example.pproject.faq.dto.FaqListResponse;
import com.example.pproject.faq.dto.FaqResponse;
import com.example.pproject.faq.dto.FaqUpdateRequest;
import com.example.pproject.faq.service.FaqService;
import com.example.pproject.global.response.ApiResponse; // 패키지 경로 확인 필요 (global.response일 수도 있음)
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
@RequiredArgsConstructor
@Slf4j
public class FaqController {

    private final FaqService faqService;

    /**
     * ADM-FAQ-001: FAQ 목록 조회 (관리자)
     * URL: /api/admin/faqs
     */
    @GetMapping("/api/admin/faqs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<FaqListResponse>>> getFaqList(
            @RequestParam(value = "is_public", required = false) Boolean isPublic,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("FAQ 목록 조회 요청");
        Page<FaqListResponse> faqs = faqService.getFaqList(isPublic, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("FAQ 목록 조회 성공", faqs));
    }

    /**
     * ADM-FAQ-002: FAQ 신규 등록 (관리자)
     * URL: /api/admin/faqs
     */
    @PostMapping("/api/admin/faqs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FaqResponse>> createFaq(
            @Valid @RequestBody FaqCreateRequest request) {

        log.info("FAQ 생성 요청");
        FaqResponse response = faqService.createFaq(request);

        // 테스트 통과를 위해 201 Created 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("FAQ 생성 성공", response));
    }

    /**
     * FAQ 상세 조회 (관리자)
     * URL: /api/admin/faqs/{id}
     */
    @GetMapping("/api/admin/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FaqResponse>> getFaqDetail(@PathVariable Long id) {
        log.info("FAQ 상세 조회 요청");
        FaqResponse response = faqService.getFaqDetail(id);
        return ResponseEntity.ok(ApiResponse.success("FAQ 상세 조회 성공", response));
    }

    /**
     * ADM-FAQ-003: FAQ 수정 (관리자)
     * URL: /api/admin/faqs/{id}
     */
    @PutMapping("/api/admin/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FaqResponse>> updateFaq(
            @PathVariable Long id,
            @Valid @RequestBody FaqUpdateRequest request) {
        log.info("FAQ 수정 요청");
        FaqResponse response = faqService.updateFaq(id, request);
        return ResponseEntity.ok(ApiResponse.success("FAQ 수정 성공", response));
    }

    /**
     * ADM-FAQ-004: FAQ 삭제 (관리자)
     * URL: /api/admin/faqs/{id}
     */
    @DeleteMapping("/api/admin/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFaq(@PathVariable Long id) {
        log.info("FAQ 삭제 요청");
        faqService.deleteFaq(id);
        return ResponseEntity.ok(ApiResponse.success("FAQ 삭제 성공", null));
    }

    /**
     * 공개 FAQ 목록 조회 (사용자)
     * URL: /api/v1/faqs/public/list
     * 이 메서드 때문에 클래스 레벨에 /api/admin을 붙이면 안 되는 것입니다!
     */
    @GetMapping("/api/v1/faqs/public/list")
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getPublicFaqList() {
        log.info("공개 FAQ 목록 조회 요청");
        List<FaqResponse> faqs = faqService.getPublicFaqList();
        return ResponseEntity.ok(ApiResponse.success("공개 FAQ 목록 조회 성공", faqs));
    }
}