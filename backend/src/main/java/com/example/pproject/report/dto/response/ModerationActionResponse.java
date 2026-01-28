package com.example.pproject.report.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationActionResponse {

    private Long actionId;
    private Long reportId;
    private Long adminMemberId;
    private String decision;
    private Integer sanctionLevel;
    private Integer restrictDays;
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime decidedAt;
}