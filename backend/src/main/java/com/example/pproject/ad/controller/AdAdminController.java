package com.example.pproject.ad.controller;

import com.example.pproject.ad.scheduler.AdBudgetScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * [관리자용] 광고 시스템 수동 조작 API
 * 
 * 주의: 프로덕션에서는 관리자 권한 체크 필요 (현재는 개발/테스트용)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ad")
@RequiredArgsConstructor
public class AdAdminController {

    private final AdBudgetScheduler adBudgetScheduler;

    /**
     * [수동 트리거] 일일 예산 정산 및 Redis 세팅 스케줄러
     * 
     * 호출 시:
     * 1. 전날 사용량 정산/환불
     * 2. 오늘 예산 선차감
     * 3. Redis에 budget/cpc/status 세팅
     */
    @PostMapping("/scheduler/trigger")
    public ResponseEntity<Map<String, String>> triggerBudgetScheduler() {
        log.warn("[ADMIN] Manual trigger of AdBudgetScheduler requested");

        try {
            adBudgetScheduler.resetAndReserveDailyBudgets();
            log.info("[ADMIN] AdBudgetScheduler completed successfully");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "스케줄러 실행 완료. Redis에 예산 데이터가 세팅되었습니다."));
        } catch (Exception e) {
            log.error("[ADMIN] AdBudgetScheduler failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "스케줄러 실행 실패: " + e.getMessage()));
        }
    }
}
