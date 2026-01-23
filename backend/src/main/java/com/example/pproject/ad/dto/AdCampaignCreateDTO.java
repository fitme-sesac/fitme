package com.example.pproject.ad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdCampaignCreateDTO {
    private Long employerId;
    private Long jobId;
    private Integer cpcBid;
    private Integer dailyBudget;
    private Instant startAt;
    private Instant endAt;
}
