package com.example.pproject.admin.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.admin.dto.request.MemberFilterRequest;
import com.example.pproject.admin.dto.request.MemberGradeChangeRequest;
import com.example.pproject.admin.dto.response.*;
import com.example.pproject.admin.service.AdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
@Slf4j
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    // 1. 회원 목록 조회
    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<MaskedMemberResponse>> getMembers(
            @ModelAttribute MemberFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminMemberService.getMembersWithFilters(filterRequest, pageable));
    }

    // 2. 통합 관리 (승급/강등/탈퇴/정지)
    @PutMapping("/{memberId}/manage")
    @PreAuthorize("hasAnyRole('APPROVEADMIN', 'MASTER')")
    public ResponseEntity<MaskedMemberResponse> manageMember(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberGradeChangeRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("회원 관리 요청: memberId={}, adminId={}, action={}",
                memberId, principal.getId(), request.getTargetStatus());

        request.setMemberId(memberId);
        request.setAdminMemberId(principal.getId()); // JWT에서 관리자 ID 추출
        return ResponseEntity.ok(adminMemberService.updateMemberStatusAndGrade(memberId, request));
    }

    // 3. 통계 조회
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<MemberManagementStatsResponse> getStats() {
        return ResponseEntity.ok(adminMemberService.getMemberManagementStats());
    }

    // 4. 이력 조회
    @GetMapping("/{memberId}/history")
    @PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
    public ResponseEntity<Page<MemberGradeHistoryResponse>> getHistory(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminMemberService.getGradeHistory(memberId, pageable));
    }
}