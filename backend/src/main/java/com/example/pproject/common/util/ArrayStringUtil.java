package com.example.pproject.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL 배열 타입 문자열 처리 유틸리티
 * - PostgreSQL 배열이 문자열로 변환될 때 {value1,value2} 형식으로 나오는 것을 처리
 * - List<String> ↔ String 변환 지원
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

    /**
     * List<String>을 쉼표로 구분된 문자열로 변환
     * 예: ["Java", "Python", "React"] -> "Java,Python,React"
     * 
     * @param list 문자열 리스트 (null 가능)
     * @return 쉼표로 구분된 문자열, null이면 null 반환
     */
    public static String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    /**
     * 쉼표로 구분된 문자열을 List<String>으로 변환
     * 예: "Java,Python,React" -> ["Java", "Python", "React"]
     * 
     * @param str 쉼표로 구분된 문자열 (null 가능)
     * @return 문자열 리스트, null이면 null 반환
     */
    public static List<String> stringToList(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        
        // { } 제거 (PostgreSQL 배열 형식인 경우)
        String cleaned = cleanArrayString(str);
        
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * List<String>에서 { } 제거 후 문자열로 변환 (프론트엔드 표시용)
     * 
     * @param list 문자열 리스트
     * @return 쉼표로 구분된 문자열
     */
    public static String listToCleanString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> cleanArrayString(s.trim()))
                .collect(Collectors.joining(","));
    }
}
