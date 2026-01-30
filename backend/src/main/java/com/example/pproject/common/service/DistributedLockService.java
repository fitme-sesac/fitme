package com.example.pproject.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis를 활용한 분산 락 서비스.
 * <p>
 * 중복 요청 방지를 위해 사용됩니다.
 * - tryLock: 락 획득 시도 (SETNX + TTL)
 * - unlock: 락 해제
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(10);
    private static final String LOCK_VALUE = "LOCKED";

    /**
     * 락 획득 시도
     *
     * @param key 락 키 (예: "wallet:use:{userId}:{orderId}")
     * @return true: 락 획득 성공, false: 이미 다른 요청이 처리 중
     */
    public boolean tryLock(String key) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, LOCK_VALUE, DEFAULT_LOCK_TIMEOUT);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 락 획득 시도 (타임아웃 지정)
     *
     * @param key     락 키
     * @param timeout 락 만료 시간
     * @return true: 락 획득 성공, false: 이미 다른 요청이 처리 중
     */
    public boolean tryLock(String key, Duration timeout) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, LOCK_VALUE, timeout);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 락 해제
     *
     * @param key 락 키
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
