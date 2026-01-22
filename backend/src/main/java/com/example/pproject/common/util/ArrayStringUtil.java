package com.example.pproject.common.util;

/**
 * PostgreSQL 배열 타입 문자열 처리 유틸리티
 * - PostgreSQL 배열이 문자열로 변환될 때 {value1,value2} 형식으로 나오는 것을 처리
 */
public class ArrayStringUtil {

    /**
     * PostgreSQL 배열 문자열에서 { } 제거
     * 예: "{Java,Python,React}" -> "Java,Python,React"
     * 
     * @param arrayString PostgreSQL 배열 문자열 (null 가능)
     * @return { } 제거된 문자열, null이면 null 반환
     */
    public static String cleanArrayString(String arrayString) {
        if (arrayString == null || arrayString.isBlank()) {
            return arrayString;
        }
        
        String cleaned = arrayString.trim();
        
        // { } 제거
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        return cleaned.trim();
    }
}
