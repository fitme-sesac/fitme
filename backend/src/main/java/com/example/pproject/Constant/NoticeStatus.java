package com.example.pproject.Constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 공지사항 상태 (notice.status 컬럼)
 *
 * ✅ 지원하는 상태:
 * - ACTIVE: 활성 (공개 중)
 * - PENDING_DELETE: 삭제 예정 (30일 후 파기)
 * - DELETED: 삭제됨 (완전 삭제)
 */
public enum NoticeStatus {
    ACTIVE,          // 활성
    PENDING_DELETE,  // 삭제 예정
    DELETED;         // 삭제됨

    /**
     * JSON/폼 입력에서 대소문자를 허용
     */
    @JsonCreator
    public static NoticeStatus from(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        String upper = v.toUpperCase();
        return switch (upper) {
            case "PENDING_DELETE", "PENDING-DELETE" -> PENDING_DELETE;
            case "DELETED" -> DELETED;
            case "ACTIVE" -> ACTIVE;
            default -> {
                try {
                    yield NoticeStatus.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    // 기본값: ACTIVE
                    yield ACTIVE;
                }
            }
        };
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}