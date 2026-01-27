package com.example.pproject.ad.controller;

import com.example.pproject.ad.dto.AdClickEventCreateDTO;
import com.example.pproject.ad.service.AdClickServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Phase 2] 광고 클릭 API V2 (Redis Guard 적용)
 * - 성능 테스트 비교용 (/api/v1/ad/clicks 와 비교)
 */
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
