package com.example.pproject.common.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 채용공고의 stack(기술 스택)으로부터 포지션(프론트엔드/백엔드/풀스택)을 도출하는 유틸.
 * - 프론트엔드 기술 위주 → "프론트엔드"
 * - 백엔드 기술 위주 → "백엔드"
 * - 둘 다 포함 → "풀스택"
 */
public final class JobPositionUtil {

    /** 정렬된 포지션 카테고리 목록 (필터/LLM 등에서 일관된 순서 보장) */
    public static final java.util.List<String> ORDERED_POSITION_CATEGORIES = java.util.List.of(
            "프론트엔드",
            "백엔드",
            "풀스택"
    );

    private static final Set<String> FRONTEND_KEYWORDS = Set.of(
            "react", "vue", "angular", "javascript", "typescript", "js", "ts",
            "next.js", "nextjs", "nuxt", "svelte", "remix", "html", "css", "scss", "sass",
            "frontend", "front-end", "프론트엔드", "프론트", "웹퍼블리싱", "ui", "ux"
    );

    private static final Set<String> BACKEND_KEYWORDS = Set.of(
            "java", "spring", "spring boot", "springboot", "kotlin", "node", "node.js", "nodejs",
            "python", "django", "flask", "fastapi", "go", "golang", "ruby", "rails", "php",
            "c#", "csharp", ".net", "asp.net", "backend", "back-end", "백엔드", "서버",
            "express", "nest", "nestjs", "graphql", "sql", "mysql", "postgresql", "mongodb"
    );

    private JobPositionUtil() {
    }

    /**
     * job_posting.stack(List&lt;String&gt;)으로부터 포지션 문자열 반환.
     * - 프론트엔드 기술만 있으면 "프론트엔드"
     * - 백엔드 기술만 있으면 "백엔드"
     * - 둘 다 있으면 "풀스택"
     * - 없거나 매칭 없으면 null
     */
    public static String derivePosition(List<String> stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Set<String> lower = stack.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (lower.isEmpty()) {
            return null;
        }

        boolean hasFrontend = lower.stream().anyMatch(JobPositionUtil::isFrontend);
        boolean hasBackend = lower.stream().anyMatch(JobPositionUtil::isBackend);

        if (hasFrontend && hasBackend) {
            return "풀스택";
        }
        if (hasFrontend) {
            return "프론트엔드";
        }
        if (hasBackend) {
            return "백엔드";
        }
        return null;
    }

    private static boolean isFrontend(String s) {
        if (s == null || s.isBlank()) return false;
        String lower = s.toLowerCase();
        return FRONTEND_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private static boolean isBackend(String s) {
        if (s == null || s.isBlank()) return false;
        String lower = s.toLowerCase();
        return BACKEND_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
