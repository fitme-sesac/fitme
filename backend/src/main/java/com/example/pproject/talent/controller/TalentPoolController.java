package com.example.pproject.talent.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.talent.dto.TalentDetailDTO;
import com.example.pproject.talent.dto.TalentListItemDTO;
import com.example.pproject.talent.service.TalentPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인재풀 API - DB의 구직자(이력서) 데이터 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/talents")
@RequiredArgsConstructor
public class TalentPoolController {

    private final TalentPoolService talentPoolService;

    /**
     * 인재풀 목록 조회 (페이지네이션 + 매칭율)
     * @param principal 로그인한 기업회원인 경우 job_posting stack 기반 매칭율 계산
     */
    @GetMapping
    public ResponseEntity<Page<TalentListItemDTO>> getTalentPool(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 12, sort = "lastModifiedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Long employerMemberId = null;
        if (principal != null && principal.getAuthorities() != null &&
                principal.getAuthorities().stream()
                        .anyMatch(a -> a != null && "ROLE_EMPLOYER".equals(a.getAuthority()))) {
            employerMemberId = principal.getId();
            log.info("기업회원 인재풀 조회 - memberId: {}", employerMemberId);
        }
        
        Page<TalentListItemDTO> page = talentPoolService.getTalentPool(pageable, employerMemberId);
        return ResponseEntity.ok(page);
    }

    /**
     * 인재 상세 조회 (memberId = 구직자 PK)
     * 기업 회원은 연락처(이메일/전화) 확인 가능
     */
    @GetMapping("/{memberId}")
    public ResponseEntity<TalentDetailDTO> getTalentDetail(
            @PathVariable Long memberId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        boolean isCompany = principal != null
                && principal.getAuthorities() != null
                && principal.getAuthorities().stream()
                .anyMatch(a -> a != null && "ROLE_EMPLOYER".equals(a.getAuthority()));
        return talentPoolService.getTalentDetail(memberId, isCompany)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
