package com.example.pproject.report.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberReportCreateRequest {

    @NotBlank(message = "신고 대상 타입은 필수입니다")
    @JsonProperty("target_type")
    private String targetType; // JOB_POSTING, MEMBER

    @NotNull(message = "신고 대상 ID는 필수입니다")
    @JsonProperty("target_id")
    private Long targetId;

    @NotBlank(message = "신고 사유 코드는 필수입니다")
    @JsonProperty("reason_code")
    private String reasonCode;

    @JsonProperty("description")
    private String description; // reasonDetail로 매핑
}