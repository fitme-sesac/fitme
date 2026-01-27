package com.example.pproject.ad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdGuardService {

    private final StringRedisTemplate redisTemplate;

    // Key Prefix
    private static final String GUARD_KEY_PREFIX = "ad_state:"; // Changed from ad_guard to ad_state
    private static final String ACTIVE_SET_KEY = "ad:active_campaigns";

    /**
     * 광고 예산 차감 (Unified Script: Hash Get -> Check -> Deduct -> Active Set Manage)
     * 
     * [Logic]
     * 1. HGETALL로 상태(budget, cpc, status) 조회
     * 2. status != ACTIVE or budget <= 0 이면 -> Active Set에서 제거 & Return -1
     * 3. budget < cpc 이면 (잔액 털기) -> budget=0으로 만듦 & Active Set에서 제거 & Return
     * actual_deducted
     * 4. budget >= cpc 이면 (정상 차감) -> budget -= cpc
     * - 만약 차감 후 budget == 0 이면 -> Active Set에서 제거
     * - 아니면 -> Active Set 유지 (SADD 호출로 갱신 보장)
     * - Return cpc
     * 
     * @param campaignId 캠페인 ID (CPC는 Redis 내부에 저장됨)
     * @return 실제 차감된 금액 (>= 0), 실패 시 -1
     */
    public long reduceBudget(Long campaignId) {
        String key = getGuardKey(campaignId);

        // Lua Script: Unified Logic
        String script = "local state = redis.call('HMGET', KEYS[1], 'budget', 'cpc', 'status') " +
                "local budget = tonumber(state[1]) " +
                "local cpc = tonumber(state[2]) " +
                "local status = state[3] " +

                // 1. 상태 체크 및 예산 체크 (없거나, 비활성, 0원)
                "if (budget == nil or cpc == nil or status ~= 'ACTIVE' or budget <= 0) then " +
                "  redis.call('SREM', ARGV[1], ARGV[2]) " + // Active Set에서 제거
                "  return -1 " +
                "end " +

                // 2. 차감 로직
                "if (budget >= cpc) then " +
                "  local new_budget = redis.call('HINCRBY', KEYS[1], 'budget', -cpc) " +
                "  if (new_budget == 0) then " +
                "    redis.call('SREM', ARGV[1], ARGV[2]) " + // 잔액 0됨 -> 제거
                "  else " +
                "    redis.call('SADD', ARGV[1], ARGV[2]) " + // 유지 (혹시 빠져있으면 추가)
                "  end " +
                "  return cpc " +
                "else " +
                "  // 3. 잔액 털기 (Partial Deduction) " +
                "  redis.call('HSET', KEYS[1], 'budget', 0) " +
                "  redis.call('SREM', ARGV[1], ARGV[2]) " + // 0됨 -> 제거
                "  return budget " +
                "end";

        Long deducted = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                java.util.Collections.singletonList(key), // KEYS[1]: ad_state:{id}
                ACTIVE_SET_KEY, // ARGV[1]: ad:active_campaigns
                String.valueOf(campaignId) // ARGV[2]: campaignId
        );

        if (deducted == null || deducted < 0) {
            log.debug("Budget exhausted or inactive. CampaignId: {}, Result: {}", campaignId, deducted);
            return -1;
        }

        return deducted;
    }

    /**
     * 예산 및 상태 설정 (Unified Hash)
     * - 매일 0시 또는 캠페인 생성/수정 시 호출
     * - Hash에 budget, cpc, status 저장
     * - 잔액이 있으면 Active Set에 추가
     */
    public void setDailyBudget(Long campaignId, long budget, int cpc, String status) {
        String key = getGuardKey(campaignId);

        // 1. Hash 저장
        redisTemplate.opsForHash().put(key, "budget", String.valueOf(budget));
        redisTemplate.opsForHash().put(key, "cpc", String.valueOf(cpc));
        redisTemplate.opsForHash().put(key, "status", status);
        redisTemplate.expire(key, Duration.ofHours(25)); // TTL

        // 2. Active Set 관리 (잔액 있고 Active면 추가, 아니면 제거)
        if (budget > 0 && "ACTIVE".equals(status)) {
            addToActiveSet(campaignId);
        } else {
            removeFromActiveSet(campaignId);
        }

        log.info("Redis Guard Set (Unified). ID: {}, Budget: {}, CPC: {}, Status: {}", campaignId, budget, cpc, status);
    }

    /**
     * 예산만 업데이트 (재충전 등)
     */
    public void updateBudget(Long campaignId, long newBudget) {
        String key = getGuardKey(campaignId);
        redisTemplate.opsForHash().put(key, "budget", String.valueOf(newBudget));

        // 재충전됐으니 상태 확인 후 Active Set 복구 시도 (Active 상태라고 가정하거나, HGET으로 확인 필요하지만 여기선 간단히
        // 무조건 시도)
        // 안전하게 HGET으로 status 확인 후 추가하는 것이 좋음.
        String status = (String) redisTemplate.opsForHash().get(key, "status");
        if (newBudget > 0 && "ACTIVE".equals(status)) {
            addToActiveSet(campaignId);
        }
    }

    /**
     * 입찰가(CPC)만 업데이트
     */
    public void updateCpc(Long campaignId, int cpc) {
        String key = getGuardKey(campaignId);
        redisTemplate.opsForHash().put(key, "cpc", String.valueOf(cpc));
    }

    /**
     * 상태(Status)만 업데이트
     * - 상태 변경에 따라 Active Set 관리
     */
    public void updateStatus(Long campaignId, String status) {
        String key = getGuardKey(campaignId);
        redisTemplate.opsForHash().put(key, "status", status);

        // 상태 변경 후 Active Set 동기화
        String budgetStr = (String) redisTemplate.opsForHash().get(key, "budget");
        long budget = (budgetStr != null) ? Long.parseLong(budgetStr) : 0;

        if (budget > 0 && "ACTIVE".equals(status)) {
            addToActiveSet(campaignId);
        } else {
            removeFromActiveSet(campaignId);
        }

        log.info("Redis Status Updated. ID: {}, Status: {}", campaignId, status);
    }

    private String getGuardKey(Long campaignId) {
        return GUARD_KEY_PREFIX + campaignId;
    }

    // =================================================================================
    // Active Set Management (Redis Set)
    // =================================================================================

    public void addToActiveSet(Long campaignId) {
        redisTemplate.opsForSet().add(ACTIVE_SET_KEY, String.valueOf(campaignId));
    }

    public void removeFromActiveSet(Long campaignId) {
        redisTemplate.opsForSet().remove(ACTIVE_SET_KEY, String.valueOf(campaignId));
    }

    public java.util.Set<String> getActiveCampaignIds() {
        return redisTemplate.opsForSet().members(ACTIVE_SET_KEY);
    }
}
