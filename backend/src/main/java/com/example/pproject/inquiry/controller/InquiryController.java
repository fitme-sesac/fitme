package com.example.pproject.inquiry.controller;

import com.example.pproject.inquiry.dto.request.MemberInquiryCreateRequest;
import com.example.pproject.inquiry.service.InquiryService;
import com.example.pproject.inquiry.dto.request.CreateInquiryRequest;
import com.example.pproject.inquiry.dto.request.ReplyInquiryRequest;
import com.example.pproject.inquiry.dto.request.UpdateInquiryStatusRequest;
import com.example.pproject.inquiry.dto.response.InquiryResponse;
import com.example.pproject.inquiry.dto.response.InquiryListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // RequestHeader 포함됨
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Slf4j
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * [수정됨] 회원 문의 등록
     * @AuthenticationPrincipal -> @RequestHeader("x-user-id") 로 변경
     * 이유: Swagger에서 테스트 시 직접 ID를 입력하기 위함
     */
    @PostMapping
    public ResponseEntity<InquiryResponse> createInquiry(
            @Valid @RequestBody MemberInquiryCreateRequest request,
            @RequestHeader(value = "x-user-id", required = true) Long currentMemberId) { // 👈 여기 수정됨!

        log.info("회원 문의 생성 요청: memberId={}, title={}", currentMemberId, request.getTitle());

        // DTO 변환
        CreateInquiryRequest serviceRequest = CreateInquiryRequest.builder()
                .memberId(currentMemberId)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .build();

        InquiryResponse response = inquiryService.createInquiry(serviceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ... (아래는 기존 코드와 동일하지만, 혹시 몰라 전체 드립니다) ...

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryResponse> getInquiry(
            @PathVariable Long inquiryId) {
        log.info("문의 조회: inquiryId={}", inquiryId);
        InquiryResponse response = inquiryService.getInquiry(inquiryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<Page<InquiryListResponse>> getInquiriesByMember(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("회원 문의 목록 조회: memberId={}", memberId);
        Pageable pageable = PageRequest.of(page, size);
        Page<InquiryListResponse> response = inquiryService.getInquiriesByMember(memberId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<InquiryListResponse>> getInquiriesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("상태별 문의 조회: status={}", status);
        Pageable pageable = PageRequest.of(page, size);
        Page<InquiryListResponse> response = inquiryService.getInquiriesByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/member/{memberId}/open-count")
    public ResponseEntity<Map<String, Long>> getOpenInquiryCount(
            @PathVariable Long memberId) {
        log.info("응답 대기 문의 개수 조회: memberId={}", memberId);
        long count = inquiryService.getOpenInquiryCount(memberId);

        Map<String, Long> response = new HashMap<>();
        response.put("memberId", memberId);
        response.put("openCount", count);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{inquiryId}/reply")
    public ResponseEntity<InquiryResponse> replyToInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody ReplyInquiryRequest request) {
        log.info("문의 답변 추가: inquiryId={}", inquiryId);
        request.setInquiryId(inquiryId);
        InquiryResponse response = inquiryService.replyToInquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{inquiryId}/status")
    public ResponseEntity<InquiryResponse> updateInquiryStatus(
            @PathVariable Long inquiryId,
            @Valid @RequestBody UpdateInquiryStatusRequest request) {
        log.info("문의 상태 업데이트: inquiryId={}, status={}", inquiryId, request.getStatus());
        request.setInquiryId(inquiryId);
        InquiryResponse response = inquiryService.updateInquiryStatus(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Map<String, String>> deleteInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long memberId) {
        log.info("문의 삭제: inquiryId={}, memberId={}", inquiryId, memberId);
        inquiryService.deleteInquiry(inquiryId, memberId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "문의가 삭제되었습니다.");

        return ResponseEntity.ok(response);
    }
}