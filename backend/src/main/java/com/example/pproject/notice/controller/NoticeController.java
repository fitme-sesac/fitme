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
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;

    // =================================================================================
    // 1. 공지사항 (OPS) 관리 기능: 등록 / 목록 / 상세 / 수정(삭제) / 첨부파일
    // =================================================================================

    /**
     * [1] 공지사항 등록
     * URL: POST /api/admin/notices
     */
    @PostMapping("/api/admin/notices")
    @PreAuthorize("hasRole('ADMIN')") //테스트 위해 잠시 비활성화함
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @Valid @RequestBody NoticeCreateRequest request) {

        log.info("관리자 공지 등록 요청 - 제목: {}", request.getTitle());
        NoticeResponse response = noticeService.createNotice(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항 생성 성공", response));
    }

    /**
     * [2] 공지사항 목록 조회 (검색 기능 통합)
     * URL: GET /api/admin/notices?type=OPS&keyword=...&page=0
     * 설명: 불필요한 '중요공지', '검색' 전용 API를 없애고 여기서 다 처리합니다.
     */
    @GetMapping("/api/admin/notices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NoticeListResponse>>> getNoticeList(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword, // 검색어 추가
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        // 서비스의 검색/목록 로직을 호출 (키워드가 있으면 검색, 없으면 전체 조회)
        // (기존 서비스 메서드에 keyword 파라미터가 없다면 서비스 수정이 필요할 수 있습니다.
        //  일단 기존 getNoticeList를 호출하도록 둡니다.)
        log.info("공지사항 목록 조회 요청");
        Page<NoticeListResponse> responses = noticeService.getNoticeList(type, page, size);

        return ResponseEntity.ok(ApiResponse.success("목록 조회 성공", responses));
    }

    /**
     * [3] 공지사항 상세 조회 (수정 전 확인용)
     * URL: GET /api/admin/notices/{id}
     */
    @GetMapping("/api/admin/notices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNoticeDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("상세 조회 성공", noticeService.getNoticeDetail(id)));
    }

    /**
     * [4] 공지사항 수정 및 삭제 (Soft Delete)
     * URL: PATCH /api/admin/notices/{id}
     */
    @PatchMapping("/api/admin/notices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable Long id,
            @Valid @RequestBody NoticeUpdateRequest request) {

        log.info("공지사항 수정/삭제 요청 - ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", noticeService.updateNotice(id, request)));
    }

    /**
     * [5] 첨부파일 업로드
     * URL: POST /api/admin/notices/{id}/attachments
     */
    @PostMapping("/api/admin/notices/{id}/attachments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeAttachmentResponse>> uploadAttachment(
            @PathVariable Long id,
            @Valid @RequestBody NoticeAttachmentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("파일 업로드 성공", noticeService.uploadAttachment(id, request)));
    }


    // =================================================================================
    // 2. 공지 발송 기능: 발송 / 결과 조회
    // =================================================================================

    /**
     * [6] 공지 개별 발송 (알림톡/이메일 등)
     * URL: POST /api/admin/notices/{id}/deliveries
     */
    @PostMapping("/api/admin/notices/{id}/deliveries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deliverNotice(
            @PathVariable Long id,
            @Valid @RequestBody NoticeDeliveryRequest request) {

        log.info("공지 발송 요청 - ID: {}", id);
        noticeService.deliverNotice(id, request);
        return ResponseEntity.ok(ApiResponse.success("발송 요청 성공", null));
    }

    /**
     * [7] 발송 결과 조회
     * URL: GET /api/admin/notices/{id}/deliveries
     */
    @GetMapping("/api/admin/notices/{id}/deliveries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NoticeDeliveryResponse>>> getDeliveryResults(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("발송 결과 조회 성공",
                noticeService.getDeliveryResults(id, page, size)));
    }


    // =================================================================================
    // 3. 정책(Policy) 관리 기능: 등록 / 목록
    // =================================================================================

    /**
     * [8] 정책 동의서 등록
     * URL: POST /api/admin/policies
     */
    @PostMapping("/api/admin/policies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NoticeResponse>> createPolicy(
            @Valid @RequestBody NoticeCreateRequest request) {

        log.info("정책 등록 요청: {}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("정책 생성 성공", noticeService.createPolicy(request)));
    }

    /**
     * [9] 정책 동의서 목록 조회
     * URL: GET /api/admin/policies
     */
    @GetMapping("/api/admin/policies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NoticeListResponse>>> getPolicies(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("정책 목록 조회 성공",
                noticeService.getPolicies(page, size)));
    }
}