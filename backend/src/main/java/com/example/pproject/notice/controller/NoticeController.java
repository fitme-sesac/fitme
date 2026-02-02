package com.example.pproject.notice.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.notice.dto.*;
import com.example.pproject.notice.entity.NoticeDelivery;
import com.example.pproject.notice.entity.Notice.NoticeType;
import com.example.pproject.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;

    // ==================== PUBLIC API (누구나 접근 가능) ====================

    @GetMapping
    public ResponseEntity<Page<NoticeListResponse>> getNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(noticeService.getNoticesPublic(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNoticePublic(noticeId));
    }

    @GetMapping("/policy/{type}")
    public ResponseEntity<NoticeResponse> getLatestPolicy(@PathVariable NoticeType type) {
        return ResponseEntity.ok(noticeService.getLatestPolicy(type));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Page<NoticeListResponse>> getNoticesByType(
            @PathVariable NoticeType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(noticeService.getNoticesByTypePublic(
                type, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<NoticeListResponse>> searchNotices(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(noticeService.searchNoticesPublic(
                keyword, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    // ==================== ADMIN API (관리자 권한 필요) ====================

    /**
     * 공지사항 생성
     * NoticeCreateRequest에 attachments(첨부파일) 리스트가 포함되어 있음
     */
    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<NoticeResponse> createNotice(
            @Valid @RequestBody NoticeCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long adminId = principal.getId();
        log.info("공지사항 생성: adminId={}, title={}", adminId, request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noticeService.createNotice(request, adminId));
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<NoticeListResponse>> getNoticesAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(noticeService.getNoticesAdmin(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/admin/{noticeId}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<NoticeResponse> getNoticeAdmin(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNoticeAdmin(noticeId));
    }

    @PutMapping("/admin/{noticeId}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<NoticeResponse> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long adminId = principal.getId();
        log.info("공지사항 수정: noticeId={}, adminId={}", noticeId, adminId);
        return ResponseEntity.ok(noticeService.updateNotice(noticeId, request, adminId));
    }

    @DeleteMapping("/admin/{noticeId}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        log.info("공지사항 삭제: noticeId={}", noticeId);
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<NoticeService.NoticeStatistics> getStatistics() {
        return ResponseEntity.ok(noticeService.getStatistics());
    }

    // [추가] 공지사항 발송 요청 (NoticeDelivery 활용)
    // 예: POST /api/v1/notices/admin/1/send?channel=PUSH
    @PostMapping("/admin/{noticeId}/send")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Void> sendNoticeNotification(
            @PathVariable Long noticeId,
            @RequestParam NoticeDelivery.DeliveryChannel channel,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long adminId = principal.getId();
        log.info("공지사항 발송 요청: noticeId={}, channel={}, adminId={}", noticeId, channel, adminId);
        // 실제 발송 로직은 Service에 구현 필요
        // noticeService.sendNotice(noticeId, channel, adminId);
        return ResponseEntity.ok().build();
    }
}