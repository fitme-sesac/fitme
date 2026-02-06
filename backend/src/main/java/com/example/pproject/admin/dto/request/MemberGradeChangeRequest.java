package com.example.pproject.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberGradeChangeRequest {

    private Long memberId;

    // 등급 변경 시 유효성 검증 (Null 허용, 값이 있으면 검증)
    @Pattern(regexp = "^(VIP|PREMIUM|BASIC)$", message = "등급은 VIP, PREMIUM, BASIC 중 하나여야 합니다.")
    private String targetGrade; // VIP, PREMIUM, BASIC

    // ✅ 필수 제거 -> 선택 입력 가능
    @Pattern(regexp = "^(ACTIVE|SUSPENDED|WITHDRAWN)$", message = "상태는 ACTIVE, SUSPENDED, WITHDRAWN 중 하나여야 합니다.")
    private String targetStatus;

    // ✅ 사유는 필수 (이력 관리용)
    @NotBlank(message = "변경 사유는 필수입니다")
    private String changeReason;

    private String adminNotes;
    private Long adminMemberId;
}