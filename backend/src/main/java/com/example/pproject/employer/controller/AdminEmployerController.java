package com.example.pproject.employer.controller;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 관리자용 기업 관리 API
 */
@RestController
@RequestMapping("/api/v1/admin/employers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SERVICEADMIN')")
@Slf4j
public class AdminEmployerController {

    private final EmployerRepository employerRepository;

    /**
     * GET /api/v1/admin/employers
     * 기업 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> getEmployers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search) {
        log.info("관리자 기업 목록 조회: search={}", search);

        Page<EmployerEntity> employers = employerRepository.findAll(pageable);

        Page<Map<String, Object>> result = employers.map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("employerId", e.getId());
            map.put("companyName", e.getName());
            map.put("status", e.getStatus());
            map.put("createdAt", e.getCreatedAt());
            // map.put("businessNumber", e.getBusinessNumber()); // Entity에 없음
            return map;
        });

        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/v1/admin/employers/{employerId}/verify
     * 기업 인증 승인/거절
     */
    @PatchMapping("/{employerId}/verify")
    public ResponseEntity<Map<String, Object>> verifyEmployer(
            @PathVariable Long employerId,
            @RequestBody Map<String, String> request) {
        String action = request.get("action"); // APPROVE or REJECT
        log.info("기업 인증 처리: employerId={}, action={}", employerId, action);

        EmployerEntity employer = employerRepository.findById(employerId)
                .orElseThrow(() -> new RuntimeException("기업을 찾을 수 없습니다: " + employerId));

        if ("APPROVE".equalsIgnoreCase(action)) {
            employer.setStatus("ACTIVE"); // OR VERIFIED
        } else if ("REJECT".equalsIgnoreCase(action)) {
            employer.setStatus("REJECTED");
        }

        employerRepository.save(employer);

        Map<String, Object> response = new HashMap<>();
        response.put("employerId", employerId);
        response.put("status", employer.getStatus());
        response.put("message", "기업 상태가 변경되었습니다.");

        return ResponseEntity.ok(response);
    }
}
