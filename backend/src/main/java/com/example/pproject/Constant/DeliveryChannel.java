package com.example.pproject.Constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 공지사항 발송 채널 (notice_delivery.channel 컬럼)
 *
 * ✅ 지원하는 채널:
 * - EMAIL: 이메일
 * - SMS: 문자메시지
 * - LMS: 장문 문자메시지
 * - PUSH: 푸시 알림
 */
public enum DeliveryChannel {
    EMAIL,  // 이메일
    SMS,    // 문자메시지
    LMS,    // 장문 문자메시지
    PUSH;   // 푸시 알림

    /**
     * JSON/폼 입력에서 대소문자를 허용
     */
    @JsonCreator
    public static DeliveryChannel from(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        String upper = v.toUpperCase();
        try {
            return DeliveryChannel.valueOf(upper);
        } catch (IllegalArgumentException e) {
            // 기본값: EMAIL
            return EMAIL;
        }
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}