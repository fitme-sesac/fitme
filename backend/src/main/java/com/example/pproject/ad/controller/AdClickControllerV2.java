package com.example.pproject.ad.controller;

import com.example.pproject.ad.dto.AdClickEventCreateDTO;
import com.example.pproject.ad.service.AdClickServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [V2] Redis 기반 광고 클릭 API
 * - nGrinder 부하 테스트용 (V1 vs V2 성능 비교)
 * - DB 트랜잭션 없이 Redis Atomic 연산으로 초고속 차감 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/ad/clicks")
@RequiredArgsConstructor
public class AdClickControllerV2 {

    private final AdClickServiceV2 adClickServiceV2;

    @PostMapping
    public ResponseEntity<Void> trackClick(@RequestBody AdClickEventCreateDTO dto) {
        adClickServiceV2.trackClick(dto);
        return ResponseEntity.ok().build();
    }
}
