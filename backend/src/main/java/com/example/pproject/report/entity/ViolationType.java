package com.example.pproject.report.entity;

import lombok.Getter;

@Getter
public enum ViolationType {

    // ==========================================================
    // 🟢 [경미 (Minor): 5 ~ 10점]
    // ==========================================================
    MINOR_SPAM(5, "단순 도배/스팸"),
    MINOR_INAPPROPRIATE_NICKNAME(5, "부적절한 닉네임"),
    MINOR_CATEGORY_MISMATCH(5, "카테고리 오분류"),
    MINOR_ABUSIVE_LANGUAGE(10, "욕설 또는 비속어 사용"),
    MINOR_ETC(10, "기타 커뮤니티 규칙 위반"),

    // ==========================================================
    // 🟡 [중대 (Major): 20 ~ 50점]
    // ==========================================================
    MAJOR_POLITICAL_CONTENT(30, "정치적 발언/분쟁 유발"),
    MAJOR_INFLAMMATORY_CONTENT(30, "분쟁 조장 또는 갈등 유발 게시물"),
    MAJOR_FALSE_INFO(30, "허위 정보 게시(낚시성)"),
    MAJOR_ADVERTISING(30, "허가되지 않은 상업적 홍보 및 광고"),
    MAJOR_COPYRIGHT_INFRINGEMENT(30, "저작권 침해"),
    MAJOR_ABUSIVE_LANGUAGE_SEVERE(40, "심한 욕설/인신공격"),
    MAJOR_IMPERSONATION(40, "운영자 또는 타인 사칭"),
    MAJOR_HATE_SPEECH(50, "혐오 발언/차별 조장"),
    MAJOR_REPEATED_VIOLATION(50, "반복적 규정 위반"),

    // ==========================================================
    // 🔴 [치명 (Critical): 100점]
    // ==========================================================
    CRITICAL_FRAUD(100, "사기 행위"),
    CRITICAL_FRAUDULENT_TRANSACTION(100, "사기성 거래 또는 허위 정보로 인한 금전적 피해 유발"),
    CRITICAL_SEXUAL_CONTENT(100, "음란물/부적절한 콘텐츠"),
    CRITICAL_ILLEGAL_PROMOTION(100, "도박, 마약 등 불법 사이트/물품 홍보"),
    CRITICAL_PRIVACY_INVASION(100, "타인의 개인정보 유출 및 유포 (신상털기)"),
    CRITICAL_HACKING_ATTEMPT(100, "해킹 시도 및 개인정보 탈취"),
    CRITICAL_SECURITY_THREAT(100, "계정 도용 시도 또는 개인정보 무단 수집");

    private final int score;
    private final String description;

    ViolationType(int score, String description) {
        this.score = score;
        this.description = description;
    }
}