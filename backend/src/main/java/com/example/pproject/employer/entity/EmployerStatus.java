package com.example.pproject.employer.entity;

/**
 * 기업 상태 Enum
 * - PENDING: 승인 대기
 * - ACTIVE: 활성 (정상)
 * - INACTIVE: 비활성
 * - SUSPENDED: 정지
 */
public enum EmployerStatus {
    PENDING("승인 대기"),
    ACTIVE("활성"),
    INACTIVE("비활성"),
    SUSPENDED("정지");

    private final String displayName;

    EmployerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 문자열로부터 EmployerStatus 변환 (대소문자 무시)
     */
    public static EmployerStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return PENDING;
        }
        try {
            return EmployerStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }

    /**
     * 채용공고 등록이 가능한 상태인지 확인
     */
    public boolean canPostJob() {
        return this == ACTIVE;
    }
}
