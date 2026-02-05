package com.example.pproject.Constant;

/**
 * 면접 단계
 */
public enum InterviewStage {
    FIRST("1ST"),    // 1차 면접
    SECOND("2ND"),   // 2차 면접
    FINAL("FINAL");  // 최종 면접

    private final String code;

    InterviewStage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * DB 코드(1ST, 2ND, FINAL)로부터 enum 값을 반환
     * JdbcTemplate 사용 시 활용
     */
    public static InterviewStage fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (InterviewStage stage : values()) {
            if (stage.getCode().equals(code)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Unknown InterviewStage code: " + code);
    }
}
