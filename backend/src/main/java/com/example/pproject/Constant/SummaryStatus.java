package com.example.pproject.Constant;

public enum SummaryStatus {
    NONE,       // AI 요약 요청 없음
    PENDING,    // 대기 중 (사용자가 요청함)
    PROCESSING, // AI가 분석 및 처리 중
    COMPLETED,  // 요약 완료
    FAILED      // 분석 실패
}