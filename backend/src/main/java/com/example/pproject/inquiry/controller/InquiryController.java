package com.example.pproject.inquiry.controller;

import com.example.pproject.Config.JwtUserPrincipal;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
     * 회원 문의 등록 - JWT 인증에서 사용자 ID 추출
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER')")
    public ResponseEntity<InquiryResponse> createInquiry(
            @Valid @RequestBody MemberInquiryCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long currentMemberId = principal.getId();
        log.info("회원 문의 생성 요청: memberId={}, title={}", currentMemberId, request.getTitle());

        CreateInquiryRequest serviceRequest = CreateInquiryRequest.builder()
                .memberId(currentMemberId)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .build();

        InquiryResponse response = inquiryService.createInquiry(serviceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 문의 상세 조회 - 본인 또는 관리자만 가능
     */
    @GetMapping("/{inquiryId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<InquiryResponse> getInquiry(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("문의 조회: inquiryId={}, requesterId={}", inquiryId, principal.getId());
        InquiryResponse response = inquiryService.getInquiry(inquiryId);

        // 본인 확인 (관리자가 아닌 경우)
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SERVICEADMIN")
                        || auth.getAuthority().equals("ROLE_APPROVEADMIN")
                        || auth.getAuthority().equals("ROLE_MASTER"));

        if (!isAdmin && !response.getMemberId().equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("본인의 문의만 조회할 수 있습니다.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 회원별 문의 목록 조회 - 본인 또는 관리자만 가능
     */
    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<InquiryListResponse>> getInquiriesByMember(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        // 본인 확인 (관리자가 아닌 경우)
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SERVICEADMIN")
                        || auth.getAuthority().equals("ROLE_APPROVEADMIN")
                        || auth.getAuthority().equals("ROLE_MASTER"));

        if (!isAdmin && !memberId.equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("본인의 문의 목록만 조회할 수 있습니다.");
        }

        log.info("회원 문의 목록 조회: memberId={}, requesterId={}", memberId, principal.getId());
        Pageable pageable = PageRequest.of(page, size);
        Page<InquiryListResponse> response = inquiryService.getInquiriesByMember(memberId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 상태별 문의 조회 - 관리자만 가능
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<InquiryListResponse>> getInquiriesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("상태별 문의 조회: status={}", status);
        Pageable pageable = PageRequest.of(page, size);
        Page<InquiryListResponse> response = inquiryService.getInquiriesByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 응답 대기 문의 개수 조회 - 본인 또는 관리자만 가능
     */
    @GetMapping("/member/{memberId}/open-count")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Map<String, Long>> getOpenInquiryCount(
            @PathVariable Long memberId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        // 본인 확인 (관리자가 아닌 경우)
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SERVICEADMIN")
                        || auth.getAuthority().equals("ROLE_APPROVEADMIN")
                        || auth.getAuthority().equals("ROLE_MASTER"));

        if (!isAdmin && !memberId.equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("본인의 문의 정보만 조회할 수 있습니다.");
        }

        log.info("응답 대기 문의 개수 조회: memberId={}", memberId);
        long count = inquiryService.getOpenInquiryCount(memberId);

        Map<String, Long> response = new HashMap<>();
        response.put("memberId", memberId);
        response.put("openCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * 문의 답변 추가 - 관리자만 가능
     */
    @PostMapping("/{inquiryId}/reply")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<InquiryResponse> replyToInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody ReplyInquiryRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("문의 답변 추가: inquiryId={}, adminId={}", inquiryId, principal.getId());
        request.setInquiryId(inquiryId);
        InquiryResponse response = inquiryService.replyToInquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 문의 상태 업데이트 - 관리자만 가능
     */
    @PutMapping("/{inquiryId}/status")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<InquiryResponse> updateInquiryStatus(
            @PathVariable Long inquiryId,
            @Valid @RequestBody UpdateInquiryStatusRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("문의 상태 업데이트: inquiryId={}, status={}, adminId={}", inquiryId, request.getStatus(), principal.getId());
        request.setInquiryId(inquiryId);
        InquiryResponse response = inquiryService.updateInquiryStatus(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 문의 삭제 - 본인 또는 관리자만 가능 (JWT에서 사용자 ID 추출)
     */
    @DeleteMapping("/{inquiryId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Void> deleteInquiry(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long memberId = principal.getId();
        log.info("문의 삭제: inquiryId={}, memberId={}", inquiryId, memberId);

        // Service에서 본인 확인 또는 관리자 확인 로직 처리
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SERVICEADMIN")
                        || auth.getAuthority().equals("ROLE_APPROVEADMIN")
                        || auth.getAuthority().equals("ROLE_MASTER"));

        if (isAdmin) {
            // 관리자는 모든 문의 삭제 가능
            inquiryService.deleteInquiry(inquiryId, null);
        } else {
            // 일반 사용자는 본인 문의만 삭제 가능
            inquiryService.deleteInquiry(inquiryId, memberId);
        }

        return ResponseEntity.noContent().build();
    }
}