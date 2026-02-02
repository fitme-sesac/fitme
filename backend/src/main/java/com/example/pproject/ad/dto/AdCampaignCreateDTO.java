package com.example.pproject.ad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdCampaignCreateDTO {
    private Long employerId;
    private Long jobId;
    private Integer cpcBid;
    private Integer dailyBudget;
    private LocalDate startDate; // 예: "2026-01-24"
    private LocalDate endDate;   // 예: "2026-01-31"
}
