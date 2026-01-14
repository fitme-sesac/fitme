package com.example.pproject.Constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 공지사항 발송 상태 (notice_delivery.status 컬럼)
 *
 * ✅ 지원하는 상태:
 * - PENDING: 발송 대기 중
 * - SENT: 발송 완료
 * - FAILED: 발송 실패
 */
public enum DeliveryStatus {
    PENDING,  // 대기
    SENT,     // 발송완료
    FAILED;   // 발송실패

    /**
     * JSON/폼 입력에서 대소문자를 허용
     */
    @JsonCreator
    public static DeliveryStatus from(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        String upper = v.toUpperCase();
        try {
            return DeliveryStatus.valueOf(upper);
        } catch (IllegalArgumentException e) {
            // 기본값: PENDING
            return PENDING;
        }
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}