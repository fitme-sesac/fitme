package com.example.pproject.notice.controller;

import com.example.pproject.notice.dto.*;
import com.example.pproject.notice.service.NoticeService;
import com.example.pproject.notice.service.NoticeDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 공지사항 관리 API (SERVICEADMIN)
 * - GET /api/admin/notices - 공지사항 목록 조회
 * - POST /api/admin/notices - 공지사항 신규 등록
 * - PATCH /api/admin/notices/{id} - 공지사항 수정/삭제
 * - POST /api/admin/notices/{id}/attachments - 첨부파일 추가
 * - POST /api/admin/notices/{id}/deliveries - 공지사항 발송
 * - GET /api/admin/notices/{id}/deliveries - 발송 결과 조회
 */
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ServiceAdminNoticeController {

    private final NoticeService noticeService;
    private final NoticeDeliveryService noticeDeliveryService;

    /**
     * 공지사항 목록 조회 (관리자)
     *
     * GET /api/admin/notices?keyword=검색어&noticeType=OPS&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<NoticeResponse>> getNoticeList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String noticeType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("공지사항 목록 조회 - keyword: {}, noticeType: {}, page: {}, size: {}",
                keyword, noticeType, page, size);

        Page<NoticeResponse> result = noticeService.getNoticeList(keyword, noticeType, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 공지사항 상세 조회
     *
     * GET /api/admin/notices/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNoticeDetail(@PathVariable Long id) {
        log.info("공지사항 상세 조회 - ID: {}", id);
        NoticeResponse notice = noticeService.getNoticeDetail(id);
        return ResponseEntity.ok(notice);
    }

    /**
     * 공지사항 신규 등록
     *
     * POST /api/admin/notices
     * {
     *   "title": "공지 제목",
     *   "body": "공지 본문",
     *   "isPublic": true,
     *   "noticeType": "OPS"
     * }
     */
    @PostMapping
    public ResponseEntity<NoticeResponse> createNotice(
            @Valid @RequestBody CreateNoticeRequest request,
            Authentication authentication) {

        log.info("공지사항 신규 등록 - title: {}", request.getTitle());

        // JWT 토큰에서 관리자 ID 추출
        Long adminId = extractAdminId(authentication);
        NoticeResponse createdNotice = noticeService.createNotice(request, adminId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdNotice);
    }

    /**
     * 공지사항 수정
     *
     * PATCH /api/admin/notices/{id}
     * {
     *   "title": "수정된 제목",
     *   "body": "수정된 본문",
     *   "isPublic": true,
     *   "status": "ACTIVE"
     * }
     */
    @PatchMapping("/{id}")
    public ResponseEntity<NoticeResponse> updateNotice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoticeRequest request,
            Authentication authentication) {

        log.info("공지사항 수정 - ID: {}", id);

        Long adminId = extractAdminId(authentication);
        NoticeResponse updatedNotice = noticeService.updateNotice(id, request, adminId);

        return ResponseEntity.ok(updatedNotice);
    }

    /**
     * 공지사항 삭제 (논리 삭제 - 30일 후 파기)
     *
     * DELETE /api/admin/notices/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        log.info("공지사항 삭제 - ID: {}", id);
        noticeService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 첨부파일 추가
     *
     * POST /api/admin/notices/{id}/attachments
     * {
     *   "fileUrl": "https://...",
     *   "fileName": "파일명.pdf"
     * }
     */
    @PostMapping("/{id}/attachments")
    public ResponseEntity<AttachmentResponse> addAttachment(
            @PathVariable Long id,
            @Valid @RequestBody AddAttachmentRequest request) {

        log.info("첨부파일 추가 - 공지ID: {}, 파일명: {}", id, request.getFileName());
        AttachmentResponse attachment = noticeService.addAttachment(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }

    /**
     * 첨부파일 제거
     *
     * DELETE /api/admin/notices/attachments/{attachmentId}
     */
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> removeAttachment(@PathVariable Long attachmentId) {
        log.info("첨부파일 제거 - ID: {}", attachmentId);
        noticeService.removeAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 공지사항을 회원들에게 발송
     *
     * POST /api/admin/notices/{id}/deliveries
     * {
     *   "channel": "EMAIL",
     *   "targetMemberIds": [1, 2, 3, 4, 5]
     * }
     */
    @PostMapping("/{id}/deliveries")
    public ResponseEntity<Void> sendNoticeToMembers(
            @PathVariable Long id,
            @Valid @RequestBody BatchDeliveryRequest request) {

        log.info("공지사항 발송 - 공지ID: {}, 채널: {}, 회원수: {}",
                id, request.getChannel(), request.getTargetMemberIds().size());

        noticeDeliveryService.sendNoticeToMembers(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 공지사항의 발송 결과 조회
     *
     * GET /api/admin/notices/{id}/deliveries?page=0&size=10
     */
    @GetMapping("/{id}/deliveries")
    public ResponseEntity<Page<DeliveryResultResponse>> getDeliveryResults(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("발송 결과 조회 - 공지ID: {}, page: {}, size: {}", id, page, size);
        Page<DeliveryResultResponse> results = noticeDeliveryService.getDeliveryResults(id, page, size);

        return ResponseEntity.ok(results);
    }

    /**
     * JWT 토큰에서 관리자 ID 추출
     * JWT 토큰 구조에 맞게 커스터마이징 필요
     */
    private Long extractAdminId(Authentication authentication) {
        // TODO: JWT 토큰 구조에 맞게 구현
        // 예: UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        //     return principal.getMemberId();
        return 1L;  // 플레이스홀더
    }
}