package com.example.pproject.ad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdClickEventCreateDTO {
    private Long campaignId;
    private Long memberId; // Nullable (비로그인)
    private String clickKey; // 프론트에서 생성 or 서버 생성
}
