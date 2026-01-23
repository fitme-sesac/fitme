package com.example.pproject.employer.dto;

import lombok.*;
import java.util.List;

/**
 * 광고 통계 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdStatsDTO {
    private int activeCampaigns;
    private long totalClicks;
    private long totalImpressions;
    private long totalSpent;
    private double ctr;
    private List<CampaignDTO> campaigns;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CampaignDTO {
        private Long campaignId;
        private Long jobId;
        private String jobTitle;
        private String status;
        private int clicks;
        private int impressions;
        private double ctr;
        private int dailyBudget;
        private int cpcBid;
    }
}
