package com.example.pproject.report.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessReportRequest {

    @NotNull(message = "신고 ID는 필수입니다")
    private Long reportId;

    @NotBlank(message = "판정은 필수입니다")
    private String decision;  // ACCEPT, REJECT

    private Integer sanctionLevel;  // 제재 수준
    private Integer restrictDays;   // 제재 기간

    @NotBlank(message = "판단 사유는 필수입니다")
    private String reason;

    @NotNull(message = "관리자 ID는 필수입니다")
    private Long adminMemberId;
}
