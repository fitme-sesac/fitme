package com.example.pproject.employer.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.employer.dto.EmployerDashboardDTO;
import com.example.pproject.employer.dto.EmployerProfileDTO;
import com.example.pproject.employer.service.EmployerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final EmployerService employerService;

    /**
     * 기업 대시보드 조회
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            EmployerDashboardDTO dashboard = employerService.getDashboard(principal.getUserid());
            return ResponseEntity.ok(dashboard);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 기업 프로필 조회
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            EmployerProfileDTO profile = employerService.getProfile(principal.getUserid());
            return ResponseEntity.ok(profile);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 기업 프로필 등록/수정
     */
    @PostMapping("/profile")
    public ResponseEntity<?> saveProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody EmployerProfileDTO dto) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            EmployerProfileDTO saved = employerService.saveProfile(principal.getUserid(), dto);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
