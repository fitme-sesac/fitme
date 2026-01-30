package com.example.pproject.ad.controller;

import com.example.pproject.ad.dto.AdClickEventCreateDTO;
import com.example.pproject.ad.service.AdClickService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 광고 클릭 이벤트 API
 * - 프론트엔드에서 광고 클릭 시 호출하여 클릭 기록 및 과금 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ad/clicks")
@RequiredArgsConstructor
public class AdClickController {

    private final AdClickService adClickService;

    /**
     * 광고 클릭 이벤트 기록
     * - 중복 클릭 방지 (clickKey)
     * - CPC 과금 처리
     */
    @PostMapping
    public ResponseEntity<Void> trackClick(@RequestBody AdClickEventCreateDTO dto) {
        adClickService.trackClick(dto);
        return ResponseEntity.ok().build();
    }
}
