package com.example.pproject.common.util;

/**
 * PostgreSQL 배열 타입 문자열 처리 유틸리티
 * - text[] 배열이 문자열로 변환되면 {value1,value2} 형태가 됨
 * - 이를 깨끗하게 "value1, value2" 형태로 변환
 */
public class ArrayStringUtil {
    
    private ArrayStringUtil() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }
    
    /**
     * PostgreSQL 배열 문자열에서 {}, "" 제거하고 깔끔하게 변환
     * - "{Java,Python,React}" → "Java, Python, React"
     * - "{"Java Script","Node.js"}" → "Java Script, Node.js"
     * - 이미 깔끔한 문자열이면 그대로 반환
     * - null이면 null 반환
     * 
     * @param arrayString PostgreSQL 배열 형태의 문자열
     * @return 깔끔하게 정리된 문자열
     */
    public static String cleanArrayString(String arrayString) {
        if (arrayString == null || arrayString.isBlank()) {
            return arrayString;
        }
        
        String result = arrayString.trim();
        
        // {로 시작하고 }로 끝나면 제거
        if (result.startsWith("{") && result.endsWith("}")) {
            result = result.substring(1, result.length() - 1);
        }
        
        // 따옴표 제거 ("" 형태로 감싸진 요소들)
        result = result.replace("\"", "");
        
        // 쉼표 뒤에 공백이 없으면 추가하여 가독성 향상
        if (result.contains(",") && !result.contains(", ")) {
            result = result.replace(",", ", ");
        }
        
        // 빈 배열이었으면 null 반환
        if (result.isBlank()) {
            return null;
        }
        
        return result;
    }
    
    /**
     * 배열 문자열을 배열로 변환
     * - "{Java,Python,React}" → ["Java", "Python", "React"]
     * 
     * @param arrayString PostgreSQL 배열 형태의 문자열
     * @return 문자열 배열
     */
    public static String[] toArray(String arrayString) {
        String cleaned = cleanArrayString(arrayString);
        if (cleaned == null || cleaned.isBlank()) {
            return new String[0];
        }
        
        return cleaned.split(",\\s*");
    }
}
