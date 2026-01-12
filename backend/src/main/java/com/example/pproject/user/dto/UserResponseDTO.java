package com.example.pproject.user.dto;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.SocialType;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    // =========================
    // ✅ member 테이블 구조(요구사항) 기준
    // - 기존 코드 호환을 위해 userid/username/birthday 필드는 유지
    // =========================
    private Integer id;                // (member_id) 일련번호

    private String userid;             // (login_id) 아이디(일반 가입). 소셜 가입은 NULL 가능
    private String email;              // 이메일
    private String username;           // (name) 이름

    private String gender;             // MALE/FEMALE/OTHER/UNDISCLOSED
    private String birthday;           // (birth_date) YYYY-MM-DD 권장
    private String phone;              // 휴대폰 번호

    // ✅ 정책 동의 저장 컬럼(서버 세팅 권장)
    private Long termsNoticeId;
    private LocalDateTime termsAgreedAt;
    private Long privacyNoticeId;
    private LocalDateTime privacyAgreedAt;
    private Long policyNoticeId;
    private LocalDateTime policyAgreedAt;

    private Boolean marketingOptIn;
    private LocalDateTime marketingAgreedAt;

    private RoleType roleType;         // 권한(추후 enum 재정의 가능)
    private SocialType socialType;     // 소셜 타입(추후 auth_provider로 정리)

    // ✅ 레거시 호환용 주소(추후 제거/분리 가능)
    private String postcode;
    private String address;
    private String detailAddress;
    private String extraAddress;

    private LocalDateTime modDate;     // 레거시 호환용
}
