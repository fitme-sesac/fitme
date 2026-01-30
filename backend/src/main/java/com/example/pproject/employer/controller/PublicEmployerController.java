package com.example.pproject.employer.controller;

import com.example.pproject.employer.dto.PublicEmployerDetailDTO;
import com.example.pproject.employer.dto.PublicEmployerListResponseDTO;
import com.example.pproject.employer.service.EmployerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 공개 기업 API (인증 없이 접근 가능)
 * - 홈/랜딩 등에서 채용 중인 기업 목록·상세 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/public/employers")
@RequiredArgsConstructor
public class PublicEmployerController {

    private final EmployerService employerService;

    /**
     * 공개 기업 목록 조회 (채용 중인 기업)
     * - OPEN 공고가 1개 이상인 기업만 반환
     */
    @GetMapping
    public ResponseEntity<?> getPublicEmployers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String industry) {
        try {
            PublicEmployerListResponseDTO response = employerService.getPublicEmployers(page, size, industry);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("공개 기업 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 공개 기업 상세 조회
     */
    @GetMapping("/{employerId}")
    public ResponseEntity<?> getPublicEmployer(@PathVariable Long employerId) {
        try {
            PublicEmployerDetailDTO employer = employerService.getPublicEmployer(employerId);
            if (employer == null) {
                return ResponseEntity.status(404).body(Map.of("error", "기업을 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(employer);
        } catch (Exception e) {
            log.error("공개 기업 상세 조회 중 오류 발생: employerId={}", employerId, e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }
}
