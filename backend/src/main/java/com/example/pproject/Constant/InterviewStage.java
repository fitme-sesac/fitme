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
}
