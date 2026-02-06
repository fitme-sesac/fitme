package com.example.pproject.admin.dto.response;

import lombok.*;

@Getter
@Builder // [핵심] 이 어노테이션이 있어야 .builder()를 쓸 수 있습니다.
@AllArgsConstructor
@NoArgsConstructor
public class MemberManagementStatsResponse {
    private long totalMembers;
    private long activeMembers;
    private long suspendedMembers;
    private long withdrawnMembers;
    private long candidateMembers;
    private long employerMembers;
}