package com.example.pproject.audit.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogFilterRequest {
    private Long actor_member_id;
    private String target_type;
    private String action;
    private String startDate; // YYYY-MM-DD
    private String endDate;   // YYYY-MM-DD
}