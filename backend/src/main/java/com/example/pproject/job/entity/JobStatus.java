package com.example.pproject.job.entity;

/**
 * 채용공고 상태 Enum
 * - DRAFT: 임시저장 (비공개)
 * - OPEN: 모집중 (공개)
 * - CLOSED: 마감 (비공개)
 */
public enum JobStatus {
    DRAFT("임시저장"),
    OPEN("모집중"),
    CLOSED("마감");

    private final String displayName;

    JobStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 문자열로부터 JobStatus 변환 (대소문자 무시)
     */
    public static JobStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return DRAFT;
        }
        try {
            return JobStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return DRAFT;
        }
    }

    /**
     * 공개 가능한 상태인지 확인
     */
    public boolean isPublic() {
        return this == OPEN;
    }
}
