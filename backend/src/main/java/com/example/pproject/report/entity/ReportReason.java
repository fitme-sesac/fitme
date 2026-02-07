package com.example.pproject.report.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ReportReason {
    // 팝업창 드롭다운 메뉴와 일치해야 함
    POLITICAL("정치적 발언"),
    HATE_SPEECH("혐오 발언"),
    ABUSIVE_LANGUAGE("욕설/비하"),
    SPAM("스팸/홍보"),
    INAPPROPRIATE_NICKNAME("부적절한 닉네임"),
    SEXUAL_CONTENT("음란물/부적절한 콘텐츠"),
    OTHER("기타");

    private final String description;
}