package com.example.pproject.faq.controller;

import com.example.pproject.faq.dto.*;
import com.example.pproject.faq.service.FAQService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FAQController {

    private final FAQService faqService;

    // ==================== PUBLIC API ====================

    /**
     * 공개된 FAQ 전체 조회 (누구나 접근 가능)
     * GET /api/v1/faqs
     */
    @GetMapping
    public ResponseEntity<Page<FAQListResponse>> getFAQs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<FAQListResponse> faqs = faqService.getFAQsPublic(pageable);

        return ResponseEntity.ok(faqs);
    }

    /**
     * FAQ 상세 조회 (공개만, 누구나 접근 가능)
     * GET /api/v1/faqs/{faqId}
     */
    @GetMapping("/{faqId}")
    public ResponseEntity<FAQDetailResponse> getFAQ(@PathVariable Long faqId) {
        FAQDetailResponse faq = faqService.getFAQPublic(faqId);
        return ResponseEntity.ok(faq);
    }

    /**
     * FAQ 검색 (질문만, 공개만, 누구나 접근 가능)
     * GET /api/v1/faqs/search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<FAQListResponse>> searchFAQs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FAQListResponse> faqs = faqService.searchFAQsPublic(keyword, pageable);

        return ResponseEntity.ok(faqs);
    }

    /**
     * FAQ 검색 (질문 + 답변, 공개만, 누구나 접근 가능)
     * GET /api/v1/faqs/search-full
     */
    @GetMapping("/search-full")
    public ResponseEntity<Page<FAQDetailResponse>> searchFAQsFull(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FAQDetailResponse> faqs = faqService.searchFAQsPublicFull(keyword, pageable);

        return ResponseEntity.ok(faqs);
    }

    /**
     * 최근 FAQ 조회 - 캐시용 (누구나 접근 가능)
     * GET /api/v1/faqs/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<FAQListResponse>> getRecentFAQs(
            @RequestParam(defaultValue = "5") int limit) {

        List<FAQListResponse> faqs = faqService.getRecentFAQs(Math.min(limit, 50));
        return ResponseEntity.ok(faqs);
    }

    /**
     * 최근 업데이트된 FAQ 조회 (누구나 접근 가능)
     * GET /api/v1/faqs/recently-updated
     */
    @GetMapping("/recently-updated")
    public ResponseEntity<List<FAQDetailResponse>> getRecentlyUpdatedFAQs(
            @RequestParam(defaultValue = "5") int limit) {

        List<FAQDetailResponse> faqs = faqService.getRecentlyUpdatedFAQs(Math.min(limit, 50));
        return ResponseEntity.ok(faqs);
    }

    // ==================== ADMIN API ====================

    /**
     * FAQ 생성 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * POST /api/v1/faqs/admin
     */
    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<FAQResponse> createFAQ(
            @Valid @RequestBody FAQCreateRequest request) {

        FAQResponse faq = faqService.createFAQ(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(faq);
    }

    /**
     * 관리자용 FAQ 전체 조회 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * GET /api/v1/faqs/admin/list
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<FAQResponse>> getFAQsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FAQResponse> faqs = faqService.getFAQsAdmin(pageable);

        return ResponseEntity.ok(faqs);
    }

    /**
     * 관리자용 FAQ 상세 조회 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * GET /api/v1/faqs/admin/{faqId}
     */
    @GetMapping("/admin/{faqId}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<FAQResponse> getFAQAdmin(@PathVariable Long faqId) {
        FAQResponse faq = faqService.getFAQAdmin(faqId);
        return ResponseEntity.ok(faq);
    }

    /**
     * 관리자용 FAQ 검색 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * GET /api/v1/faqs/admin/search
     */
    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<FAQResponse>> searchFAQsAdmin(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FAQResponse> faqs = faqService.searchFAQsAdmin(keyword, pageable);

        return ResponseEntity.ok(faqs);
    }

    /**
     * 공개 여부별 FAQ 조회 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * GET /api/v1/faqs/admin/by-public
     */
    @GetMapping("/admin/by-public")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<FAQListResponse>> getFAQsByPublicStatus(
            @RequestParam Boolean isPublic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FAQListResponse> faqs = faqService.getFAQsByPublicStatus(isPublic, pageable);

        return ResponseEntity.ok(faqs);
    }

    /**
     * 잠금 여부별 FAQ 조회 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * GET /api/v1/faqs/admin/by-lock
     */
    @GetMapping("/admin/by-lock")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<FAQListResponse>> getFAQsByLockStatus(
            @RequestParam Boolean locked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FAQListResponse> faqs = faqService.getFAQsByLockStatus(locked, pageable);

        return ResponseEntity.ok(faqs);
    }

    /**
     * FAQ 업데이트 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * PUT /api/v1/faqs/admin/{faqId}
     */
    @PutMapping("/admin/{faqId}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<FAQResponse> updateFAQ(
            @PathVariable Long faqId,
            @Valid @RequestBody FAQUpdateRequest request) {

        FAQResponse faq = faqService.updateFAQ(faqId, request);
        return ResponseEntity.ok(faq);
    }

    /**
     * FAQ 공개/비공개 토글 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * PATCH /api/v1/faqs/admin/{faqId}/toggle-public
     */
    @PatchMapping("/admin/{faqId}/toggle-public")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<FAQResponse> togglePublic(@PathVariable Long faqId) {
        FAQResponse faq = faqService.togglePublic(faqId);
        return ResponseEntity.ok(faq);
    }

    /**
     * FAQ 잠금/해제 토글 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * PATCH /api/v1/faqs/admin/{faqId}/toggle-lock
     */
    @PatchMapping("/admin/{faqId}/toggle-lock")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<FAQResponse> toggleLock(@PathVariable Long faqId) {
        FAQResponse faq = faqService.toggleLock(faqId);
        return ResponseEntity.ok(faq);
    }

    /**
     * FAQ 잠금 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * PATCH /api/v1/faqs/admin/{faqId}/lock
     */
    @PatchMapping("/admin/{faqId}/lock")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<FAQResponse> lockFAQ(@PathVariable Long faqId) {
        FAQResponse faq = faqService.lockFAQ(faqId);
        return ResponseEntity.ok(faq);
    }

    /**
     * FAQ 잠금 해제 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * PATCH /api/v1/faqs/admin/{faqId}/unlock
     */
    @PatchMapping("/admin/{faqId}/unlock")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<FAQResponse> unlockFAQ(@PathVariable Long faqId) {
        FAQResponse faq = faqService.unlockFAQ(faqId);
        return ResponseEntity.ok(faq);
    }

    /**
     * FAQ 삭제 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * DELETE /api/v1/faqs/admin/{faqId}
     */
    @DeleteMapping("/admin/{faqId}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Void> deleteFAQ(@PathVariable Long faqId) {
        faqService.deleteFAQ(faqId);
        return ResponseEntity.ok().build();
    }

    /**
     * 여러 FAQ 삭제 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * POST /api/v1/faqs/admin/bulk-delete
     */
    @PostMapping("/admin/bulk-delete")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Void> bulkDeleteFAQs(@RequestBody List<Long> faqIds) {
        faqService.bulkDeleteFAQs(faqIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 여러 FAQ 공개 여부 일괄 변경 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * POST /api/v1/faqs/admin/bulk-public
     */
    @PostMapping("/admin/bulk-public")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Void> bulkUpdatePublic(
            @RequestBody BulkUpdateRequest request) {

        faqService.bulkUpdatePublic(request.getIds(), request.getIsPublic());
        return ResponseEntity.ok().build();
    }

    /**
     * 여러 FAQ 잠금 여부 일괄 변경 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * POST /api/v1/faqs/admin/bulk-lock
     */
    @PostMapping("/admin/bulk-lock")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Void> bulkUpdateLocked(
            @RequestBody BulkUpdateRequest request) {

        faqService.bulkUpdateLocked(request.getIds(), request.getLocked());
        return ResponseEntity.ok().build();
    }

    /**
     * FAQ 통계 조회 (SERVICEADMIN, APPROVEADMIN, MASTER만)
     * GET /api/v1/faqs/admin/statistics
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<FAQService.FAQStatistics> getStatistics() {
        FAQService.FAQStatistics statistics = faqService.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    // ==================== Inner DTO ====================

    @lombok.Getter
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BulkUpdateRequest {
        private List<Long> ids;
        private Boolean isPublic;
        private Boolean locked;
    }
}