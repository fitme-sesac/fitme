package com.example.pproject.Constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * DB(role 컬럼) 및 Spring Security 역할과 1:1 매핑되는 RoleType.
 *
 * ✅ 사용 값(요구사항):
 * - CANDIDATE
 * - EMPLOYER
 * - SERVICEADMIN
 * - APPROVEADMIN
 * - MASTER
 *
 * 호환 처리:
 * - 과거 값(user/admin/master 등)이 들어오면 각각 CANDIDATE/SERVICEADMIN/MASTER로 매핑한다.
 */
public enum RoleType {
    CANDIDATE,
    EMPLOYER,
    SERVICEADMIN,
    APPROVEADMIN,
    MASTER;

    /**
     * JSON/폼 입력에서 대소문자/레거시 값을 허용.
     */
    @JsonCreator
    public static RoleType from(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;

        // legacy aliases
        String upper = v.toUpperCase();
        return switch (upper) {
            case "USER" -> CANDIDATE;
            case "ADMIN" -> SERVICEADMIN;
            case "MASTER" -> MASTER;
            default -> {
                try {
                    yield RoleType.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    // 안전장치: 알 수 없는 값은 기본 CANDIDATE로 수렴
                    yield CANDIDATE;
                }
            }
        };
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}
