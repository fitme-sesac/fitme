package com.example.pproject.report.entity;

import lombok.Getter;

@Getter
public enum ViolationType {

    // === [경미 (Minor): 5 ~ 10점] ===
    MINOR_SPAM(5, "단순 도배"),
    MINOR_INAPPROPRIATE_LANGUAGE(5, "부적절한 언어 사용"),
    MINOR_CATEGORY_MISMATCH(5, "카테고리 오기입"),
    MINOR_ETC(10, "기타 경미한 위반"),

    // === [중대 (Major): 20 ~ 50점] ===
    MAJOR_FALSE_INFO(30, "허위 정보 게시(낚시성)"),
    MAJOR_COPYRIGHT_INFRINGEMENT(30, "저작권 침해"),
    MAJOR_REPEATED_VIOLATION(50, "반복적 규정 위반"),

    // === [치명 (Critical): 100점] ===
    CRITICAL_FRAUD(100, "사기 행위"),
    CRITICAL_PORNOGRAPHY(100, "음란물 유포"),
    CRITICAL_HACKING_ATTEMPT(100, "해킹 시도 및 개인정보 탈취");

    private final int score;
    private final String description;

    ViolationType(int score, String description) {
        this.score = score;
        this.description = description;
    }
}