package com.example.pproject.notice.controller;

import com.example.pproject.global.response.ApiResponse;
import com.example.pproject.notice.dto.*;
import com.example.pproject.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * ADM-NTC-001: 공지 등록
     * POST /api/admin/notices
     * body: { "title": "제목", "body": "본문", "notice_type": "POLICY/OPS", "is_important": true }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @Valid @RequestBody NoticeCreateRequest request) {

        log.info("공지사항 생성 요청 - 제목: {}", request.getTitle());

        NoticeResponse response = noticeService.createNotice(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항 생성 성공", response));
    }

    /**
     * ADM-NTC-002: 첨부파일 업로드
     * POST /api/admin/notices/{id}/attachments
     * body: { "file_url": URL, "file_name": "파일명" }
     */
    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeAttachmentResponse>> uploadAttachment(
            @PathVariable Long id,
            @Valid @RequestBody NoticeAttachmentRequest request) {

        log.info("공지사항 첨부파일 업로드 요청 - 공지ID: {}, 파일명: {}", id, request.getFileName());

        NoticeAttachmentResponse response = noticeService.uploadAttachment(id, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("첨부파일 업로드 성공", response));
    }

    /**
     * ADM-NTC-003: 공지 수정/삭제
     * PATCH /api/admin/notices/{id}
     * body: { "status": "PENDING_DELETE", "title": "수정제목" }
     * 참고: 삭제 시 purge_after를 현재+30일로 설정
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable Long id,
            @Valid @RequestBody NoticeUpdateRequest request) {

        log.info("공지사항 수정 요청 - ID: {}", id);

        NoticeResponse response = noticeService.updateNotice(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("공지사항 수정 성공", response)
        );
    }

    /**
     * ADM-NTC-004: 공지 개별 발송
     * POST /api/admin/notices/{id}/deliveries
     * body: { "channel": "EMAIL/SMS", "target_member_ids": [1, 2, 3] }
     */
    @PostMapping("/{id}/deliveries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deliverNotice(
            @PathVariable Long id,
            @Valid @RequestBody NoticeDeliveryRequest request) {

        log.info("공지사항 배송 요청 - ID: {}, 채널: {}, 대상 인원: {}",
                id, request.getChannel(), request.getTargetMemberIds().size());

        noticeService.deliverNotice(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("공지사항 발송 성공", null)
        );
    }

    /**
     * ADM-NTC-005: 발송 결과 조회
     * GET /api/admin/notices/{id}/deliveries
     * 해당 공지의 회원별 전송 성공/실패 결과 목록
     */
    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NoticeDeliveryResponse>>> getDeliveryResults(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("공지사항 배송 결과 조회 요청 - ID: {}, 페이지: {}", id, page);

        Page<NoticeDeliveryResponse> responses = noticeService.getDeliveryResults(id, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("발송 결과 조회 성공", responses)
        );
    }

    /**
     * 공지사항 목록 조회
     * GET /api/admin/notices?type=OPS&page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NoticeListResponse>>> getNoticeList(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("공지사항 목록 조회 요청 - 타입: {}, 페이지: {}", type, page);

        Page<NoticeListResponse> responses = noticeService.getNoticeList(type, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("공지사항 목록 조회 성공", responses)
        );
    }

    /**
     * 공지사항 상세 조회
     * GET /api/admin/notices/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNoticeDetail(
            @PathVariable Long id) {

        log.info("공지사항 상세 조회 요청 - ID: {}", id);

        NoticeResponse response = noticeService.getNoticeDetail(id);

        return ResponseEntity.ok(
                ApiResponse.success("공지사항 상세 조회 성공", response)
        );
    }

    /**
     * ADM-POL-001: 정책동의서 등록
     * POST /api/admin/policies
     */
    @PostMapping("/policies/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> createPolicy(
            @Valid @RequestBody NoticeCreateRequest request) {

        log.info("정책동의서 생성 요청 - 제목: {}", request.getTitle());

        NoticeResponse response = noticeService.createPolicy(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("정책동의서 생성 성공", response));
    }

    /**
     * ADM-POL-002: 정책 동의서 목록 조회
     * GET /api/admin/policies/list
     */
    @GetMapping("/policies/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NoticeListResponse>>> getPolicies(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("정책 동의서 목록 조회 요청 - 페이지: {}", page);

        Page<NoticeListResponse> responses = noticeService.getPolicies(page, size);

        return ResponseEntity.ok(
                ApiResponse.success("정책 동의서 목록 조회 성공", responses)
        );
    }

    /**
     * 공개 공지사항 목록 (사용자용)
     * GET /api/v1/notices/public
     */
    @GetMapping("/public/list")
    public ResponseEntity<ApiResponse<Page<NoticeListResponse>>> getPublicNoticeList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("공개 공지사항 목록 조회 요청 - 페이지: {}", page);

        Page<NoticeListResponse> responses = noticeService.getPublicNoticeList(page, size);

        return ResponseEntity.ok(
                ApiResponse.success("공개 공지사항 목록 조회 성공", responses)
        );
    }

    /**
     * 중요 공지사항 조회
     * GET /api/admin/notices/important
     */
    @GetMapping("/important")
    public ResponseEntity<ApiResponse<Page<NoticeListResponse>>> getImportantNotices(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("중요 공지사항 조회 요청 - 페이지: {}", page);

        Page<NoticeListResponse> responses = noticeService.getImportantNotices(page, size);

        return ResponseEntity.ok(
                ApiResponse.success("중요 공지사항 조회 성공", responses)
        );
    }

    /**
     * 공지사항 검색 (제목)
     * GET /api/admin/notices/search?keyword=채용
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<NoticeListResponse>>> searchNotices(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("공지사항 검색 요청 - 키워드: {}, 페이지: {}", keyword, page);

        Page<NoticeListResponse> responses = noticeService.searchNotices(keyword, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("공지사항 검색 성공", responses)
        );
    }
}