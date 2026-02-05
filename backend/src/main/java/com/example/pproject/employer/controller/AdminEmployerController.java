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
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
@Slf4j
public class AdminEmployerController {

    private final EmployerRepository employerRepository;

    /**
     * GET /api/v1/admin/employers
     * 기업 목록 조회 (상태 필터: null=전체, ACTIVE=승인, REJECTED=거절)
     */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> getEmployers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        log.info("관리자 기업 목록 조회: search={}, status={}", search, status);

        String statusFilter = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;
        String keyword = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<EmployerEntity> employers;
        if (keyword != null && statusFilter != null) {
            employers = employerRepository.findByStatusAndNameContainingIgnoreCase(statusFilter, keyword, pageable);
        } else if (statusFilter != null) {
            employers = employerRepository.findByStatus(statusFilter, pageable);
        } else if (keyword != null) {
            employers = employerRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            employers = employerRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = employers.map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("employerId", e.getId());
            map.put("companyName", e.getName());
            map.put("status", e.getStatus());
            map.put("createdAt", e.getCreatedAt());
            return map;
        });

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/admin/employers/{employerId}
     * 기업 단건 조회 (관리자용 프로필)
     */
    @GetMapping("/{employerId}")
    public ResponseEntity<Map<String, Object>> getEmployer(@PathVariable Long employerId) {
        log.info("관리자 기업 단건 조회: employerId={}", employerId);

        EmployerEntity e = employerRepository.findById(employerId)
                .orElseThrow(() -> new RuntimeException("기업을 찾을 수 없습니다: " + employerId));

        Map<String, Object> map = new HashMap<>();
        map.put("employerId", e.getId());
        map.put("companyName", e.getName());
        map.put("status", e.getStatus());
        map.put("createdAt", e.getCreatedAt());
        map.put("logoUrl", e.getLogoUrl());
        map.put("industry", e.getIndustry());
        map.put("foundedYear", e.getFoundedYear());
        map.put("employeeCount", e.getEmployeeCount());
        map.put("location", e.getLocation());
        map.put("description", e.getDescription());
        map.put("culture", e.getCulture());
        map.put("benefits", e.getBenefits());
        map.put("techStack", e.getTechStack());
        map.put("contactEmail", e.getContactEmail());
        map.put("contactPhone", e.getContactPhone());
        map.put("websiteUrl", e.getWebsiteUrl());

        return ResponseEntity.ok(map);
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
