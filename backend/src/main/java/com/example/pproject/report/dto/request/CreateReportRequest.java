package com.example.pproject.report.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {

    @NotNull(message = "신고자 ID는 필수입니다")
    private Long reporterMemberId;

    @NotBlank(message = "신고 대상 타입은 필수입니다")
    private String targetType;  // JOB_POSTING, MEMBER, ETC

    private Long targetJobId;
    private Long targetMemberId;

    @NotBlank(message = "신고 사유 코드는 필수입니다")
    private String reasonCode;

    private String reasonDetail;
}
