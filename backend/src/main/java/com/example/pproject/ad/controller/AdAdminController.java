package com.example.pproject.ad.controller;

import com.example.pproject.ad.scheduler.AdBudgetScheduler;
import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
    private final WalletRepository walletRepository;

    /**
     * [수동 트리거] 일일 예산 정산 및 Redis 세팅 스케줄러
     * 
     * 호출 시:
     * 1. 전날 사용량 정산/환불
     * 2. 오늘 예산 선차감
     * 3. Redis에 budget/cpc/status 세팅
     */
    @PostMapping("/scheduler/trigger")
    public ResponseEntity<Map<String, Object>> triggerBudgetScheduler() {
        log.warn("[ADMIN] Manual trigger of AdBudgetScheduler requested");

        try {
            Map<String, Object> stats = adBudgetScheduler.processDailyBudgets();
            log.info("[ADMIN] AdBudgetScheduler completed successfully");

            Map<String, Object> response = new java.util.HashMap<>(stats);
            response.put("status", "success");
            response.put("message", "스케줄러 실행 완료.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[ADMIN] AdBudgetScheduler failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "스케줄러 실행 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/wallet/charge-all")
    public ResponseEntity<Map<String, Object>> chargeAllWallets() {
        log.warn("[ADMIN] Manual charge of ALL wallets requested");

        try {
            List<Wallet> allWallets = walletRepository.findAll();
            int successCount = 0;
            long chargeAmount = 100_000_000L; // 1억 충전

            for (Wallet wallet : allWallets) {
                if (wallet.getEmployer() != null) {
                    try {
                        // Wallet 엔티티의 charge 메서드 사용 (상태 체크 등 포함됨)
                        wallet.charge(chargeAmount);
                        // Dirty Checking으로 저장됨 (@Transactional 필요)
                        successCount++;
                    } catch (Exception e) {
                        log.error("Failed to charge wallet id: {}", wallet.getWalletId(), e);
                    }
                }
            }

            // 명시적 저장을 위해 flush (Optional, Transactional이 클래스 레벨에 있으면 불필요하지만 안전하게)
            walletRepository.saveAll(allWallets);

            log.info("[ADMIN] Charged {} wallets with {}", successCount, chargeAmount);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", successCount + "개 지갑에 1억씩 충전 완료",
                    "charged_wallets", successCount));
        } catch (Exception e) {
            log.error("[ADMIN] Wallet charge failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "충전 실패: " + e.getMessage()));
        }
    }
}
