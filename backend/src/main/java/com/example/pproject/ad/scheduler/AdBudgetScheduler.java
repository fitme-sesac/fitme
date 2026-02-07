package com.example.pproject.ad.scheduler;

import com.example.pproject.ad.entity.AdCampaignEntity;
import com.example.pproject.ad.repository.AdCampaignRepository;
import com.example.pproject.ad.repository.AdClickEventRepository;
import com.example.pproject.ad.service.AdGuardService;
import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.repository.WalletRepository;
import com.example.pproject.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdBudgetScheduler {

    private final AdCampaignRepository adCampaignRepository;
    private final WalletService walletService;
    private final AdGuardService adGuardService;
    private final WalletRepository walletRepository;
    private final AdClickEventRepository adClickEventRepository;

    // 매일 자정(00:00:00)에 실행되어야 하는 스케줄러입니다.
    // Cron 표현식: 초 분 시 일 월 요일 -> 매일 00시 00분 00초
    // ApplicationReadyEvent: 서버 시작 직후에도 한 번 실행하여 Redis 상태 복구
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledJob() {
        processDailyBudgets();
    }

    public java.util.Map<String, Object> processDailyBudgets() {
        // [Time Freeze] 배치 시작 시점의 시간을 고정합니다.
        LocalDate today = LocalDate.now();
        LocalDate yesterdayDate = today.minusDays(1);
        String yesterdayStr = yesterdayDate.toString();
        Instant settlementStart = yesterdayDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant settlementEnd = today.atStartOfDay(ZoneId.systemDefault()).toInstant();

        log.info("============== [Daily Ad Budget Scheduler Started] ==============");

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("date", today.toString());
        stats.put("settlement_target_id", "reserved_wallets");

        // STEP 1. Settlement
        List<Wallet> reservedWallets = walletRepository.findAllByReservedBalanceGreaterThan(0L);
        stats.put("settlement_count", reservedWallets.size());

        int settlementSuccess = 0;
        int settlementFail = 0;

        for (Wallet wallet : reservedWallets) {
            if (wallet.getEmployer() == null)
                continue;
            Long empId = wallet.getEmployer().getId();
            try {
                List<AdCampaignEntity> allCampaigns = adCampaignRepository.findByEmployerId(empId);
                long usedAmount = 0L;
                for (AdCampaignEntity campaign : allCampaigns) {
                    long campaignCost = adClickEventRepository
                            .sumCostByCampaignIdAndOccurredAtBetween(campaign.getId(), settlementStart, settlementEnd);
                    usedAmount += campaignCost;
                }
                String idempotencyKey = "DAILY_SETTLE_" + yesterdayStr + "_" + empId;
                walletService.settleDailyUsage(empId, usedAmount, idempotencyKey);
                settlementSuccess++;
            } catch (Exception e) {
                log.error("   -> [Error] Employer ID: {} 정산 실패", empId, e);
                settlementFail++;
            }
        }
        stats.put("settlement_success", settlementSuccess);
        stats.put("settlement_fail", settlementFail);

        // STEP 2. Pre-deduction check
        List<AdCampaignEntity> activeCampaigns = adCampaignRepository.findByStatus("ACTIVE");
        if (activeCampaigns.isEmpty()) {
            log.info(">> [STEP 2] 오늘 활성화된 광고 캠페인이 없습니다. (Skip)");
            stats.put("status", "no_active_campaigns");
            return stats;
        }

        Set<Long> activeEmployerIds = activeCampaigns.stream()
                .map(AdCampaignEntity::getEmployerId)
                .collect(Collectors.toSet());

        stats.put("active_employers", activeEmployerIds.size());
        int budgetHoldSuccess = 0;
        int budgetHoldFail = 0;
        java.util.List<String> failReasons = new java.util.ArrayList<>();

        for (Long empId : activeEmployerIds) {
            long totalDailyBudget = activeCampaigns.stream()
                    .filter(c -> c.getEmployerId().equals(empId))
                    .mapToLong(AdCampaignEntity::getDailyBudget)
                    .sum();

            try {
                String idempotencyKey = "BUDGET_RES_" + today + "_" + empId;

                if (totalDailyBudget > 0) {
                    walletService.holdBudget(empId, totalDailyBudget, idempotencyKey);
                } else {
                    log.warn("   -> [Skip] Employer ID: {} 총 일일 예산이 0원입니다. (지갑 홀드 생략)", empId);
                    activeCampaigns.stream()
                            .filter(c -> c.getEmployerId().equals(empId))
                            .forEach(c -> log.warn("      - Campaign ID: {}, Budget: {}", c.getId(),
                                    c.getDailyBudget()));
                }

                // Redis Charge
                activeCampaigns.stream()
                        .filter(c -> c.getEmployerId().equals(empId))
                        .forEach(c -> adGuardService.setDailyBudget(c.getId(), c.getDailyBudget(), c.getCpcBid(),
                                "ACTIVE"));

                budgetHoldSuccess++;
            } catch (Exception e) {
                log.error("   -> [Error] Employer ID: {} 예산 확보 실패", empId, e);
                budgetHoldFail++;
                failReasons.add("EmpID " + empId + ": " + e.getMessage());
                continue;
            }
        }

        stats.put("budget_hold_success", budgetHoldSuccess);
        stats.put("budget_hold_fail", budgetHoldFail);
        stats.put("fail_reasons", failReasons);

        log.info("============== [Daily Ad Budget Scheduler Completed] ==============");
        return stats;
    }
}
