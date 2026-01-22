package com.example.pproject.user.dto;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.SocialType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {
    // =========================
    // ✅ member 테이블 구조(요구사항) 기준
    // - 기존 코드 호환을 위해 userid/username/birthday 필드는 유지
    // =========================
    private Integer id;                 // (member_id) 일련번호

    private String userid;              // (login_id) 아이디(일반 가입). 소셜 가입은 NULL 가능
    private String password;            // (password_hash) 비밀번호(요청에서만 사용)
    private String passwordConfirm;     // 비밀번호 재확인(요청 전용)

    private String email;               // 이메일
    private String username;            // (name) 이름

    // ✅ 추가 입력(요구사항)
    private String gender;              // MALE/FEMALE/OTHER/UNDISCLOSED (문자열로 유지, enum은 추후)
    private String birthday;            // (birth_date) YYYY-MM-DD 권장 (기존 코드 호환 위해 String 유지)

    private String phone;               // 휴대폰 번호(숫자만 권장)
    private String phoneOtpCode;        // 휴대폰 인증번호(요청 전용)
    private Instant phoneVerifiedAt; // 휴대폰 인증 완료 시각(서버에서만 세팅)

    // ✅ 약관 동의 체크박스(요청 전용)
    private Boolean agreeTerms;
    private Boolean agreePrivacy;
    private Boolean agreePolicy;

    // ✅ 정책 동의(테이블 추가 없이 member에 저장)
    // - 실제 저장은 서버에서 ACTIVE 정책 notice_id를 조회해 (notice_id + agreed_at)로 세팅하는 것을 권장
    private Long termsNoticeId;
    private Instant termsAgreedAt;
    private Long privacyNoticeId;
    private Instant privacyAgreedAt;
    private Long policyNoticeId;
    private Instant policyAgreedAt;

    private Boolean marketingOptIn;
    private Instant marketingAgreedAt;

    private String consentIp;
    private String consentUserAgent;

    // ✅ 기존 필드(호환 유지): 주소는 추후 제거/분리 가능
    private String postcode;
    private String address;
    private String detailAddress;
    private String extraAddress;

    private RoleType roleType;          // 권한(일반적으로 서버에서 세팅)
    private SocialType socialType;      // 소셜 타입(일반적으로 서버에서 세팅)

    private Instant modDate;      // 레거시 호환용(추후 created_at/updated_at로 대체)
}
