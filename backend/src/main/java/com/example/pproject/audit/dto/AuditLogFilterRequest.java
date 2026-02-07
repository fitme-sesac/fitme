package com.example.pproject.audit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogFilterRequest {
    @JsonProperty("actor_member_id")
    private Long actorMemberId;

    @JsonProperty("target_type")
    private String targetType;

    private String action;

    private String startDate; // YYYY-MM-DD

    private String endDate; // YYYY-MM-DD
}