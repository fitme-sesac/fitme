package com.example.pproject.Constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * member.auth_provider 컬럼과 매핑되는 소셜/인증 제공자 타입.
 *
 * ✅ DDL 체크 제약과 맞춤: NAVER/GOOGLE/KAKAO/OTHER
 *
 * 호환 처리:
 * - 과거 값(google/naver/kakao/nate/Else 등)은 대소문자 무시 + 매핑.
 * - NATE/ELSE/기타 미지정 값은 OTHER로 수렴.
 */
public enum SocialType {
    NAVER,
    GOOGLE,
    KAKAO,
    OTHER;

    @JsonCreator
    public static SocialType from(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        String upper = v.toUpperCase();
        return switch (upper) {
            case "NAVER" -> NAVER;
            case "GOOGLE", "GMAIL" -> GOOGLE;
            case "KAKAO" -> KAKAO;
            case "NATE", "ELSE", "OTHER" -> OTHER;
            default -> {
                try {
                    yield SocialType.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    yield OTHER;
                }
            }
        };
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}
