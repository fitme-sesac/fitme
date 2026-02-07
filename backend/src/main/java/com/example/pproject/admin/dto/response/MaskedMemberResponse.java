package com.example.pproject.admin.dto.response;

import com.example.pproject.admin.entity.Member;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaskedMemberResponse {

    private Long memberId;
    private String memberUid;
    private String name;
    private String loginId;
    private String email;
    private String phone;
    private String role;
    private String status;
    private Integer penaltyPoint;
    private String memberGrade;
    private String gender;
    private java.time.LocalDate birthDate;
    private Boolean marketingOptIn;
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gradeUpdatedAt;

    public static MaskedMemberResponse from(Member member) {
        return MaskedMemberResponse.builder()
                .memberId(member.getMemberId())
                .memberUid(member.getMemberUid().toString())
                .name(member.getName())
                .loginId(member.getLoginId())

                // ✅ [수정 1] 마스킹 메서드 적용
                .email(maskEmail(member.getEmail()))
                .phone(maskPhone(member.getPhone()))

                .role(member.getRole())
                .status(member.getStatus())

                // ✅ [수정 2] 하드코딩 제거 -> 실제 DB 값 매핑
                .memberGrade(member.getMemberGrade()) // 실제 등급 사용
                .penaltyPoint(member.getFailedLoginCount()) // 임시: 실패횟수를 점수로? (벌점 테이블 조인 전까지 대체)

                .gender(member.getGender())
                .birthDate(member.getBirthDate())
                .marketingOptIn(member.getMarketingOptIn())
                .lastLoginAt(member.getLastLoginAt())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .deletedAt(member.getDeletedAt()) // 탈퇴일 추가
                .gradeUpdatedAt(member.getGradeUpdatedAt())
                .build();
    }

    // 🔒 전화번호 마스킹 (010-1234-5678 -> 010-****-5678)
    private static String maskPhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("(\\d{3})-?(\\d{3,4})-?(\\d{4})", "$1-****-$3");
    }

    // 🔒 이메일 마스킹 (tester@gmail.com -> te****@gmail.com)
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String id = parts[0];
        if (id.length() <= 3) return email;
        return id.substring(0, 2) + "****@" + parts[1];
    }
}