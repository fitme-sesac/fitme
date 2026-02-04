package com.example.pproject.employer.controller;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.repository.JobEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공개 기업 정보 API (인증 없이 접근 가능)
 */
@Slf4j
@RestController
@RequestMapping("/api/public/employers")
@RequiredArgsConstructor
public class PublicEmployerController {

    private final EmployerRepository employerRepository;
    private final JobEntityRepository jobEntityRepository;

    /**
     * 공개 기업 목록 조회
     */
    @GetMapping
    public ResponseEntity<?> getPublicEmployers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String industry) {
        try {
            log.info("공개 기업 목록 조회 - page: {}, size: {}, industry: {}", page, size, industry);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<EmployerEntity> employers;
            
            if (industry != null && !industry.isBlank()) {
                employers = employerRepository.findByIndustry(industry, pageable);
            } else {
                employers = employerRepository.findActiveEmployers(pageable);
            }

            // DTO로 변환
            Page<Map<String, Object>> result = employers.map(e -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", e.getId());
                dto.put("name", e.getName());
                dto.put("logoUrl", e.getLogoUrl());
                dto.put("industry", e.getIndustry());
                dto.put("location", e.getLocation());
                dto.put("employeeCount", e.getEmployeeCount());
                dto.put("foundedYear", e.getFoundedYear());
                dto.put("description", truncateText(e.getDescription(), 200));
                
                // 채용 중인 공고 수
                long activeJobCount = jobEntityRepository.countActiveByEmployerId(e.getId());
                dto.put("activeJobCount", activeJobCount);
                
                return dto;
            });

            return ResponseEntity.ok(Map.of(
                    "employers", result.getContent(),
                    "page", page,
                    "size", size,
                    "totalElements", result.getTotalElements(),
                    "totalPages", result.getTotalPages()
            ));
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
            
            EmployerEntity employer = employerRepository.findPublicById(employerId)
                    .orElse(null);
            
            if (employer == null) {
                return ResponseEntity.status(404).body(Map.of("error", "기업을 찾을 수 없습니다."));
            }

            Map<String, Object> dto = new HashMap<>();
            dto.put("id", employer.getId());
            dto.put("name", employer.getName());
            dto.put("logoUrl", employer.getLogoUrl());
            dto.put("industry", employer.getIndustry());
            dto.put("location", employer.getLocation());
            dto.put("employeeCount", employer.getEmployeeCount());
            dto.put("foundedYear", employer.getFoundedYear());
            dto.put("description", employer.getDescription());
            dto.put("culture", employer.getCulture());
            dto.put("benefits", employer.getBenefits());
            dto.put("techStack", employer.getTechStack());
            dto.put("websiteUrl", employer.getWebsiteUrl());
            
            // 채용 중인 공고 수
            long activeJobCount = jobEntityRepository.countActiveByEmployerId(employer.getId());
            dto.put("activeJobCount", activeJobCount);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("공개 기업 상세 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * 텍스트 자르기 (요약용)
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
