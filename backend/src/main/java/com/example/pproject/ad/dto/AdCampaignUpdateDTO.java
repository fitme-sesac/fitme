package com.example.pproject.ad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 광고 캠페인 수정 요청 DTO
 * - null인 필드는 수정하지 않음 (부분 업데이트)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdCampaignUpdateDTO {

    private Integer cpcBid; // null이면 수정 안 함
    private Integer dailyBudget; // null이면 수정 안 함
    private LocalDate startDate; // null이면 수정 안 함
    private LocalDate endDate; // null이면 수정 안 함
}
