package com.example.pproject.user.controller;

import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 관리자용 회원 관리 API
 */
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SERVICEADMIN')")
@Slf4j
public class AdminMemberController {

    private final UserRepository userRepository;

    /**
     * GET /api/v1/admin/members
     * 회원 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> getMembers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search) {
        log.info("관리자 회원 목록 조회: search={}", search);

        Page<UserEntity> users = userRepository.findAll(pageable);

        Page<Map<String, Object>> result = users.map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("memberId", u.getId());
            map.put("name", u.getUsername());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("role", u.getRoleType() != null ? u.getRoleType().name() : null);
            map.put("status", u.getStatus() != null ? u.getStatus() : "ACTIVE");
            map.put("createdAt", u.getCreatedAt());
            return map;
        });

        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/v1/admin/members/{memberId}/status
     * 회원 상태 변경 (정지/활성화)
     */
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<Map<String, Object>> updateMemberStatus(
            @PathVariable Long memberId,
            @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        log.info("회원 상태 변경: memberId={}, status={}", memberId, newStatus);

        UserEntity user = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberId));

        // 상태에 따라 처리
        if ("DELETED".equalsIgnoreCase(newStatus) || "SUSPENDED".equalsIgnoreCase(newStatus)) {
            user.setDeletedAt(Instant.now());
            user.setStatus(newStatus.toUpperCase());
        } else {
            user.setDeletedAt(null);
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("memberId", memberId);
        response.put("status", newStatus);
        response.put("message", "회원 상태가 변경되었습니다.");

        return ResponseEntity.ok(response);
    }
}
