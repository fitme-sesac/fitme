package com.example.pproject.common.util;

import java.util.*;

/**
 * 기업 업종(industry) 관련 유틸리티
 * - 영어 → 한글 매핑
 * - DB에서 조회한 업종을 한글로 변환
 */
public final class IndustryUtil {

    private IndustryUtil() {
    }

    // 영어 → 한글 매핑 (DB에 저장된 영어값 → 프론트엔드 표시용 한글)
    private static final Map<String, String> INDUSTRY_EN_TO_KR = new LinkedHashMap<>();
    
    // 한글 업종 표시 순서
    private static final List<String> INDUSTRY_ORDER = new ArrayList<>();

    static {
        // 영어 → 한글 매핑 정의 (순서대로 추가)
        addMapping("E-commerce", "커머스");
        addMapping("Commerce", "커머스");
        addMapping("Ecommerce", "커머스");
        addMapping("Retail", "커머스");
        addMapping("Shopping", "커머스");
        
        addMapping("Fintech", "금융/핀테크");
        addMapping("Finance", "금융/핀테크");
        addMapping("Banking", "금융/핀테크");
        addMapping("Insurance", "금융/핀테크");
        addMapping("Payment", "금융/핀테크");
        
        addMapping("O2O", "O2O");
        addMapping("Delivery", "O2O");
        addMapping("Logistics", "O2O");
        
        addMapping("Social", "소셜/커뮤니티");
        addMapping("Community", "소셜/커뮤니티");
        addMapping("SNS", "소셜/커뮤니티");
        addMapping("Media", "소셜/커뮤니티");
        addMapping("Contents", "소셜/커뮤니티");
        
        addMapping("Game", "게임");
        addMapping("Gaming", "게임");
        addMapping("Entertainment", "게임");
        
        addMapping("SaaS", "SaaS");
        addMapping("B2B", "SaaS");
        addMapping("Enterprise", "SaaS");
        addMapping("Cloud", "SaaS");
        
        addMapping("AI", "인공지능");
        addMapping("Artificial Intelligence", "인공지능");
        addMapping("Machine Learning", "인공지능");
        addMapping("ML", "인공지능");
        addMapping("Data", "인공지능");
        
        addMapping("Blockchain", "블록체인");
        addMapping("Crypto", "블록체인");
        addMapping("Web3", "블록체인");
        addMapping("NFT", "블록체인");
        
        addMapping("Mobility", "모빌리티");
        addMapping("Transportation", "모빌리티");
        addMapping("Automotive", "모빌리티");
        addMapping("EV", "모빌리티");
        
        addMapping("Travel", "여행");
        addMapping("Tourism", "여행");
        addMapping("Hospitality", "여행");
        
        addMapping("Healthcare", "헬스케어");
        addMapping("Health", "헬스케어");
        addMapping("Medical", "헬스케어");
        addMapping("Biotech", "헬스케어");
        addMapping("Pharma", "헬스케어");
        
        addMapping("Education", "에듀테크");
        addMapping("EdTech", "에듀테크");
        addMapping("Learning", "에듀테크");
        
        addMapping("HR", "HR테크");
        addMapping("HRTech", "HR테크");
        addMapping("Recruitment", "HR테크");
        
        addMapping("Real Estate", "부동산");
        addMapping("PropTech", "부동산");
        addMapping("Property", "부동산");
        
        addMapping("Food", "푸드테크");
        addMapping("FoodTech", "푸드테크");
        addMapping("F&B", "푸드테크");
        addMapping("Restaurant", "푸드테크");
        
        addMapping("Security", "보안");
        addMapping("Cybersecurity", "보안");
        
        addMapping("IoT", "IoT");
        addMapping("Hardware", "IoT");
        addMapping("Embedded", "IoT");
        
        addMapping("IT", "IT서비스");
        addMapping("Software", "IT서비스");
        addMapping("Tech", "IT서비스");
        addMapping("Technology", "IT서비스");
        
        // 한글 → 한글 (이미 한글인 경우)
        INDUSTRY_ORDER.forEach(kr -> INDUSTRY_EN_TO_KR.put(kr, kr));
    }
    
    private static void addMapping(String en, String kr) {
        INDUSTRY_EN_TO_KR.put(en.toLowerCase(), kr);
        if (!INDUSTRY_ORDER.contains(kr)) {
            INDUSTRY_ORDER.add(kr);
        }
    }

    /**
     * 영어 업종명을 한글로 변환
     * @param industry 영어 업종명
     * @return 한글 업종명 (매핑 없으면 원본 반환)
     */
    public static String toKorean(String industry) {
        if (industry == null || industry.isBlank()) {
            return null;
        }
        String lower = industry.toLowerCase().trim();
        return INDUSTRY_EN_TO_KR.getOrDefault(lower, industry);
    }

    /**
     * DB에서 조회한 업종 목록을 한글로 변환하고 중복 제거 후 정렬
     * @param industries DB에서 조회한 업종 목록
     * @return 한글 변환된 업종 목록 (전체 포함)
     */
    public static List<String> convertToKoreanList(List<String> industries) {
        if (industries == null || industries.isEmpty()) {
            return List.of("전체");
        }

        Set<String> koreanSet = new LinkedHashSet<>();
        for (String industry : industries) {
            if (industry != null && !industry.isBlank()) {
                String kr = toKorean(industry);
                if (kr != null) {
                    koreanSet.add(kr);
                }
            }
        }

        // 정렬: INDUSTRY_ORDER 순서대로
        List<String> result = new ArrayList<>();
        result.add("전체");
        for (String ordered : INDUSTRY_ORDER) {
            if (koreanSet.contains(ordered)) {
                result.add(ordered);
            }
        }
        // ORDER에 없는 항목은 뒤에 추가 (알파벳순)
        koreanSet.stream()
                .filter(k -> !INDUSTRY_ORDER.contains(k))
                .sorted()
                .forEach(result::add);

        return result;
    }

    /**
     * 한글 업종명으로 DB 검색할 때 사용할 영어 키워드 목록 반환
     * @param koreanIndustry 한글 업종명
     * @return 매칭되는 영어 키워드 목록
     */
    public static List<String> getEnglishKeywords(String koreanIndustry) {
        if (koreanIndustry == null || koreanIndustry.isBlank()) {
            return Collections.emptyList();
        }
        
        List<String> keywords = new ArrayList<>();
        keywords.add(koreanIndustry); // 한글도 포함
        
        // 역방향 매핑 (한글 → 영어들)
        for (Map.Entry<String, String> entry : INDUSTRY_EN_TO_KR.entrySet()) {
            if (entry.getValue().equals(koreanIndustry)) {
                keywords.add(entry.getKey());
            }
        }
        
        return keywords;
    }

    /**
     * 모든 한글 업종 목록 반환 (순서대로)
     */
    public static List<String> getAllKoreanIndustries() {
        List<String> result = new ArrayList<>();
        result.add("전체");
        result.addAll(INDUSTRY_ORDER);
        return result;
    }
}
