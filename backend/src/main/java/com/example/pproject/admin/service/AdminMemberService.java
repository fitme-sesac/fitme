package com.example.pproject.admin.service;

import com.example.pproject.admin.dto.request.MemberFilterRequest;
import com.example.pproject.admin.dto.request.MemberGradeChangeRequest;
import com.example.pproject.admin.dto.response.*;
import com.example.pproject.admin.entity.Member;
import com.example.pproject.admin.entity.MemberGradeHistory;
import com.example.pproject.admin.exception.MemberNotFoundException;
import com.example.pproject.admin.repository.*;
import com.example.pproject.audit.service.AuditLogService; // ✅ [추가] 감사 로그 서비스 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMemberService {

    private final AdminMemberRepository memberRepository;
    private final MemberGradeHistoryRepository historyRepository;
    private final AuditLogService auditLogService; // ✅ [추가] 서비스 주입

    // 1. 필터 검색
    @Transactional(readOnly = true)
    public Page<MaskedMemberResponse> getMembersWithFilters(MemberFilterRequest filterRequest, Pageable pageable) {
        Specification<Member> spec = MemberSpecification.getFilter(filterRequest);
        return memberRepository.findAll(spec, pageable).map(MaskedMemberResponse::from);
    }

    // 2. 통합 관리 (승급/탈퇴/정지)
    @Transactional
    public MaskedMemberResponse updateMemberStatusAndGrade(Long memberId, MemberGradeChangeRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다. ID: " + memberId));

        // ✅ [Audit Log] 변경 전 데이터 스냅샷 (DTO로 변환하여 보관)
        MaskedMemberResponse beforeData = MaskedMemberResponse.from(member);

        String prevGrade = member.getMemberGrade() != null ? member.getMemberGrade() : "BASIC";
        String prevStatus = member.getStatus();
        boolean isChanged = false;

        // 등급 변경
        if (StringUtils.hasText(request.getTargetGrade()) && !request.getTargetGrade().equals(prevGrade)) {
            member.setMemberGrade(request.getTargetGrade());
            member.setGradeUpdatedAt(LocalDateTime.now());
            isChanged = true;
        }

        // 상태 변경
        if (StringUtils.hasText(request.getTargetStatus()) && !request.getTargetStatus().equals(prevStatus)) {
            member.setStatus(request.getTargetStatus());

            // 탈퇴(WITHDRAWN) 시 개인정보 파기 및 유니크 제약조건 회피
            if ("WITHDRAWN".equals(request.getTargetStatus())) {
                member.setDeletedAt(LocalDateTime.now());

                // 1. 휴대폰 번호 삭제
                member.setPhone(null);
                member.setPhoneVerifiedAt(null);

                // 2. 이메일 변경
                String deletedEmail = "deleted_" + member.getMemberId() + "_" + member.getEmail();
                if (deletedEmail.length() > 320) {
                    deletedEmail = deletedEmail.substring(0, 320);
                }
                member.setEmail(deletedEmail);
            }
            isChanged = true;
        }

        if (!isChanged) {
            log.info("회원 정보 변경 요청이 있었으나 실질적인 변화가 없습니다. MemberID: {}", memberId);
        } else {
            // 이력(History) 저장
            MemberGradeHistory history = MemberGradeHistory.builder()
                    .targetMemberId(member.getMemberId())
                    .adminMemberId(request.getAdminMemberId())
                    .prevGrade(prevGrade)
                    .newGrade(member.getMemberGrade())
                    .prevStatus(prevStatus)
                    .newStatus(member.getStatus())
                    .changeType(determineChangeType(request))
                    .changeReason(request.getChangeReason())
                    .adminNotes(request.getAdminNotes())
                    .build();

            historyRepository.save(history);

            // ✅ [Audit Log] 변경 후 데이터 스냅샷 & 로그 저장
            MaskedMemberResponse afterData = MaskedMemberResponse.from(member); // 변경된 상태의 DTO

            auditLogService.logAction(
                    request.getAdminMemberId(),   // Actor (관리자)
                    "MEMBER",                     // Target Type
                    member.getMemberId(),         // Target ID
                    determineChangeType(request), // Action (예: GRADE_CHANGE, SUSPEND)
                    beforeData,                   // 변경 전 JSON 데이터
                    afterData,                    // 변경 후 JSON 데이터
                    null                          // IP (Controller에서 받아오지 않았다면 null)
            );
        }

        return MaskedMemberResponse.from(member);
    }

    // 변경 타입 결정 (History & Audit Log용)
    private String determineChangeType(MemberGradeChangeRequest request) {
        if ("WITHDRAWN".equals(request.getTargetStatus())) return "FORCE_WITHDRAWAL";
        if ("SUSPENDED".equals(request.getTargetStatus())) return "SUSPEND";
        if (StringUtils.hasText(request.getTargetGrade())) return "GRADE_CHANGE";
        return "STATUS_CHANGE";
    }

    // 3. 통계 조회
    @Transactional(readOnly = true)
    public MemberManagementStatsResponse getMemberManagementStats() {
        return MemberManagementStatsResponse.builder()
                .totalMembers(memberRepository.count())
                .activeMembers(memberRepository.countByStatus("ACTIVE"))
                .suspendedMembers(memberRepository.countByStatus("SUSPENDED"))
                .withdrawnMembers(memberRepository.countByStatus("WITHDRAWN"))
                .candidateMembers(memberRepository.countByRole("CANDIDATE"))
                .employerMembers(memberRepository.countByRole("EMPLOYER"))
                .build();
    }

    // 4. 이력 조회
    @Transactional(readOnly = true)
    public Page<MemberGradeHistoryResponse> getGradeHistory(Long memberId, Pageable pageable) {
        return historyRepository.findByTargetMemberId(memberId, pageable)
                .map(this::toHistoryResponse);
    }

    private MemberGradeHistoryResponse toHistoryResponse(MemberGradeHistory h) {
        return MemberGradeHistoryResponse.builder()
                .historyId(h.getHistoryId())
                .memberId(h.getTargetMemberId())
                .adminMemberId(h.getAdminMemberId())
                .previousGrade(h.getPrevGrade())
                .newGrade(h.getNewGrade())
                .changeType(h.getChangeType())
                .changeReason(h.getChangeReason())
                .adminNotes(h.getAdminNotes())
                .createdAt(h.getCreatedAt())
                .build();
    }
}