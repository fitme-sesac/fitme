package com.example.pproject.ad.controller;

import com.example.pproject.ad.dto.AdCampaignCreateDTO;
import com.example.pproject.ad.dto.AdCampaignResponseDTO;
import com.example.pproject.ad.service.AdCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ad/campaigns")
@RequiredArgsConstructor
public class AdCampaignController {

    private final AdCampaignService adCampaignService;

    // 캠페인 생성
    @PostMapping
    public ResponseEntity<AdCampaignResponseDTO> createCampaign(@RequestBody AdCampaignCreateDTO dto) {
        AdCampaignResponseDTO response = adCampaignService.createCampaign(dto);
        return ResponseEntity.ok(response);
    }

    // 기업별 캠페인 목록 조회
    @GetMapping("/employer/{employerId}")
    public ResponseEntity<Page<AdCampaignResponseDTO>> getCampaignsByEmployer(
            @PathVariable Long employerId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<AdCampaignResponseDTO> response = adCampaignService.getCampaignsByEmployer(employerId, pageable);
        return ResponseEntity.ok(response);
    }

    // 단일 조회
    @GetMapping("/{id}")
    public ResponseEntity<AdCampaignResponseDTO> getCampaign(@PathVariable Long id) {
        AdCampaignResponseDTO response = adCampaignService.getCampaign(id);
        return ResponseEntity.ok(response);
    }

    // 상태 변경 (ACTIVE, PAUSED, ENDED)
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdCampaignResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        AdCampaignResponseDTO response = adCampaignService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }
}
