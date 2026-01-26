package com.example.pproject.employer.controller;

import com.example.pproject.employer.dto.PublicEmployerDTO;
import com.example.pproject.employer.dto.PublicEmployerListDTO;
import com.example.pproject.employer.service.PublicEmployerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 공개 기업 API (인증 없이 접근 가능)
 * - 채용 중인 기업 목록 조회
 * - 기업 상세 정보 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/public/employers")
@RequiredArgsConstructor
public class PublicEmployerController {

    private final PublicEmployerService publicEmployerService;

    /**
     * 공개 기업 목록 조회
     * - 채용 공고가 있는 기업만 반환
     * - 채용 중인 공고 수와 함께 반환
     */
    @GetMapping
    public ResponseEntity<?> getPublicEmployers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String industry) {
        try {
            log.info("공개 기업 목록 조회 - page: {}, size: {}, industry: {}", page, size, industry);
            PublicEmployerListDTO response = publicEmployerService.getPublicEmployers(page, size, industry);
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
            log.info("공개 기업 상세 조회 - employerId: {}", employerId);
            PublicEmployerDTO employer = publicEmployerService.getPublicEmployer(employerId);
            return ResponseEntity.ok(employer);
        } catch (IllegalStateException e) {
            log.warn("공개 기업 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("공개 기업 상세 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }
}
