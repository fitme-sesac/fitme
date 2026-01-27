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
    // 매일 자정(00:00:00)에 실행되어야 하는 스케줄러입니다.
    // Cron 표현식: 초 분 시 일 월 요일 -> 매일 00시 00분 00초
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetAndReserveDailyBudgets() {
        // [Time Freeze] 배치 시작 시점의 시간을 고정합니다. (스케줄러가 늦게 돌거나, 실행 시간이 길어져도 기준점은 변하지 않게 하기
        // 위함)
        // 예: 1월 2일 00:00:01에 실행됨.
        LocalDate today = LocalDate.now();

        // 정산 대상일(Yesterday): 1월 1일
        LocalDate yesterdayDate = today.minusDays(1);
        String yesterdayStr = yesterdayDate.toString();

        // 어제 00:00:00 ~ 오늘 00:00:00 (정확히 24시간 범위)
        // ZoneId를 시스템 기본값으로 설정 (KST 등)
        Instant settlementStart = yesterdayDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant settlementEnd = today.atStartOfDay(ZoneId.systemDefault()).toInstant();

        log.info("============== [Daily Ad Budget Scheduler Started] ==============");
        log.info("기준 일자(Today): {}", today);
        log.info("정산 대상일(Yesterday): {}", yesterdayStr);
        log.info("집계 시간 범위: {} ~ {}", settlementStart, settlementEnd);

        // -----------------------------------------------------------------------------
        // STEP 1. [Batch Settlement] 전날 사용량 정산 및 환불
        // -----------------------------------------------------------------------------
        // 목표: "어제 광고 예약을 걸어놨던 모든 지갑"을 찾아서 실제 사용한 만큼만 차감하고 나머지는 돌려준다.
        // 대상: Active/Inactive 상관없이, 현재 Wallet에 'reserved_balance'가 0원보다 많은 모든 업체.
        // (광고를 껐더라도 어제 쓰다 남은 돈이 묶여있을 수 있으므로 반드시 환불해야 함)

        List<Wallet> reservedWallets = walletRepository.findAllByReservedBalanceGreaterThan(0L);
        log.info(">> [STEP 1] 정산 대상 지갑 수: {}개 (예약금이 남아있는 지갑들)", reservedWallets.size());

        for (Wallet wallet : reservedWallets) {
            if (wallet.getEmployer() == null)
                continue; // 방어 로직
            Long empId = wallet.getEmployer().getId();

            try {
                // 1-1. 이 업체의 모든 캠페인(Active/Inactive/Deleted 등)을 조회
                // 정산은 '내가 만든 모든 광고'에서 발생한 클릭 비용을 다 합쳐야 함.
                List<AdCampaignEntity> allCampaigns = adCampaignRepository.findByEmployerId(empId);

                // 1-2. 실제 사용 금액 집계 (DB Log 기반)
                long usedAmount = 0L;
                for (AdCampaignEntity campaign : allCampaigns) {
                    // 해당 캠페인에서 '어제 하루 동안' 발생한 클릭 비용 합계 조회 (SUM(cost))
                    // 정확한 과금액(chargedPrice)을 기반으로 정산합니다.
                    long campaignCost = adClickEventRepository
                            .sumCostByCampaignIdAndOccurredAtBetween(campaign.getId(), settlementStart, settlementEnd);

                    usedAmount += campaignCost;
                }

                // 1-3. WalletService에 정산 요청 (Idempotency Key로 중복 정산 방지)
                // "업체에게(empId), 어제 총 usedAmount원 쓰셨네요. 예약금에서 까고 나머지 돌려드릴게요."
                String idempotencyKey = "DAILY_SETTLE_" + yesterdayStr + "_" + empId;
                walletService.settleDailyUsage(empId, usedAmount, idempotencyKey);

                log.info("   -> [Success] Employer ID: {}, Used: {}원 정산 완료", empId, usedAmount);
            } catch (Exception e) {
                log.error("   -> [Error] Employer ID: {} 정산 실패", empId, e);
                // 개별 실패가 전체 배치를 멈추지 않도록 catch만 하고 진행 (추후 재시도 로직 필요)
            }
        }

        // -----------------------------------------------------------------------------
        // STEP 2. [Pre-deduction] 오늘 예산 선차감 및 Redis 충전
        // -----------------------------------------------------------------------------
        // 목표: "오늘 광고를 틀어놓은(ACTIVE)" 사장님들의 일 예산을 지갑에서 미리 빼서 확보해둔다.
        // 그리고 확보된 금액만큼 Redis에 충전해서 실시간 차감 준비를 마친다.

        List<AdCampaignEntity> activeCampaigns = adCampaignRepository.findByStatus("ACTIVE");

        if (activeCampaigns.isEmpty()) {
            log.info(">> [STEP 2] 오늘 활성화된 광고 캠페인이 없습니다. (Skip)");
            log.info("============== [Daily Ad Budget Scheduler Finished] ==============");
            return;
        }

        // 중복 없이 사장님 ID 목록 추출
        Set<Long> activeEmployerIds = activeCampaigns.stream()
                .map(AdCampaignEntity::getEmployerId)
                .collect(Collectors.toSet());

        log.info(">> [STEP 2] 예산 차감 대상 사장님 수: {}명", activeEmployerIds.size());

        for (Long empId : activeEmployerIds) {
            // 2-1. 이 사장님이 오늘 켜놓은 광고들의 하루 예산 총합 계산
            long totalDailyBudget = activeCampaigns.stream()
                    .filter(c -> c.getEmployerId().equals(empId))
                    .mapToLong(AdCampaignEntity::getDailyBudget)
                    .sum();

            try {
                // 2-2. 지갑에서 예산 선차감 (Balance -> Reserved 이동)
                // "업체에게 오늘 쓸 예산 totalDailyBudget원 미리 확보하겠습니다."
                String idempotencyKey = "BUDGET_RES_" + today + "_" + empId;
                walletService.holdBudget(empId, totalDailyBudget, idempotencyKey);

                log.info("   -> [Hold Budget] Employer ID: {}, Amount: {}원 확보 성공", empId, totalDailyBudget);
            } catch (Exception e) {
                log.error("   -> [Error] Employer ID: {} 예산 확보 실패 (잔액 부족 등). Redis 충전 Skip.", empId, e);
                // 예산 확보 실패 시, Redis 충전도 하면 안 됨 (광고 나가면 먹튀 발생)
                continue;
            }

            // 2-3. Redis Guard 충전 (실시간 트래픽 제어용)
            // 확보 성공한 캠페인들만 Redis에 세팅
            activeCampaigns.stream()
                    .filter(c -> c.getEmployerId().equals(empId))
                    .forEach(c -> {
                        // Redis Guard 충전 및 메타데이터 설정 (Unified Hash)
                        adGuardService.setDailyBudget(c.getId(), c.getDailyBudget(), c.getCpcBid(), "ACTIVE");
                    });

            log.info("   -> [Redis Charge] Employer ID: {} 관련 캠페인 Redis 세팅 완료", empId);
        }

        log.info("============== [Daily Ad Budget Scheduler Completed Successfully] ==============");
    }
}
