package com.example.pproject.ad.dto;

import com.example.pproject.ad.entity.AdCampaignEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdCampaignResponseDTO {
    private Long id;
    private Long employerId;
    private Long jobId;
    private Integer cpcBid;
    private Integer dailyBudget;
    private String status;
    private Instant startAt;
    private Instant endAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static AdCampaignResponseDTO fromEntity(AdCampaignEntity entity) {
        return AdCampaignResponseDTO.builder()
                .id(entity.getId())
                .employerId(entity.getEmployerId())
                .jobId(entity.getJobId())
                .cpcBid(entity.getCpcBid())
                .dailyBudget(entity.getDailyBudget())
                .status(entity.getStatus())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
