package com.example.pproject.Constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 공지사항 타입 (notice.notice_type 컬럼)
 *
 * ✅ 지원하는 타입:
 * - OPS: 일반 공지사항
 * - TERMS: 서비스 약관
 * - PRIVACY: 개인정보처리방침
 * - POLICY: 정책/규칙
 */
public enum NoticeType {
    OPS,      // 공지사항
    TERMS,    // 약관
    PRIVACY,  // 개인정보처리방침
    POLICY;   // 정책

    /**
     * JSON/폼 입력에서 대소문자를 허용
     */
    @JsonCreator
    public static NoticeType from(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        String upper = v.toUpperCase();
        try {
            return NoticeType.valueOf(upper);
        } catch (IllegalArgumentException e) {
            // 알 수 없는 값은 기본 OPS로 수렴
            return OPS;
        }
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}