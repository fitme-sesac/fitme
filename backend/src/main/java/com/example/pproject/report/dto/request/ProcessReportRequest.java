package com.example.pproject.report.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessReportRequest {
    private Long reportId;       // 신고 ID
    private Long adminMemberId;  // 관리자 ID

    private String decision;     // ACCEPT, REJECT

    // ✅ [수정] 위반 유형 코드
    private String violationType;

    // (선택 사항: sanctionLevel은 지워도 됩니다)
    private Integer sanctionLevel;

    private Integer restrictDays;
    private String reason;
}