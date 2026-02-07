package com.example.pproject.report.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private Long reportId;
    private Long reporterMemberId;

    private String targetType;
    private Long targetJobId;
    private Long targetMemberId;
    private String reasonCode;
    private String reasonDetail;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}