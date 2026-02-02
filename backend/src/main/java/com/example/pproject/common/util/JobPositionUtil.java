package com.example.pproject.common.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 채용공고의 stack(기술 스택)으로부터 포지션(프론트엔드/백엔드/풀스택)을 도출하는 유틸.
 * - 프론트엔드 기술 위주 → "프론트엔드"
 * - 백엔드 기술 위주 → "백엔드"
 * - 둘 다 포함 → "풀스택"
 */
public final class JobPositionUtil {

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

    // UI 포지션별 키워드 매핑 (필터 옵션용) - 확장된 카테고리
    private static final Map<String, Set<String>> POSITION_KEYWORDS = new LinkedHashMap<>();
    
    // UI 포지션 표시 순서
    private static final List<String> POSITION_ORDER = new ArrayList<>();

    static {
        // 개발 직군
        addPosition("서버/백엔드", Set.of(
                "java", "spring", "springboot", "kotlin", "node", "nodejs", "python", "django", "flask",
                "fastapi", "go", "golang", "ruby", "rails", "php", "c#", "csharp", ".net", "asp.net",
                "backend", "back-end", "백엔드", "서버", "express", "nest", "nestjs", "mysql", "postgresql", 
                "mongodb", "redis", "graphql", "rest api", "microservice", "마이크로서비스"
        ));
        addPosition("프론트엔드", Set.of(
                "react", "vue", "angular", "javascript", "typescript", "next.js", "nextjs", "nuxt",
                "svelte", "remix", "html", "css", "scss", "sass", "frontend", "front-end", "프론트엔드", 
                "프론트", "tailwind", "styled-components", "webpack", "vite", "웹퍼블리셔", "퍼블리셔"
        ));
        addPosition("웹 풀스택", Set.of(
                "fullstack", "full-stack", "풀스택", "full stack"
        ));
        addPosition("안드로이드", Set.of(
                "android", "안드로이드", "kotlin", "java", "jetpack", "compose", "안드로이드 개발"
        ));
        addPosition("iOS", Set.of(
                "ios", "swift", "objective-c", "xcode", "아이폰", "swiftui", "uikit", "ios 개발"
        ));
        addPosition("크로스플랫폼", Set.of(
                "flutter", "react native", "reactnative", "dart", "xamarin", "ionic", "cordova", "하이브리드앱"
        ));
        
        // 데이터/AI 직군
        addPosition("머신러닝/AI", Set.of(
                "tensorflow", "pytorch", "machine learning", "ml", "ai", "keras", "머신러닝", "딥러닝",
                "deep learning", "nlp", "computer vision", "huggingface", "llm", "gpt", "인공지능",
                "scikit-learn", "opencv", "langchain", "prompt engineering", "생성형 ai"
        ));
        addPosition("데이터 엔지니어", Set.of(
                "spark", "airflow", "kafka", "bigquery", "data engineer", "데이터 엔지니어", "etl", "hadoop",
                "hive", "presto", "databricks", "snowflake", "redshift", "데이터 파이프라인", "data pipeline"
        ));
        addPosition("데이터 분석가", Set.of(
                "data analyst", "데이터 분석", "bi", "tableau", "power bi", "looker", "sql", "analytics",
                "통계", "statistics", "r", "사업분석", "business analyst"
        ));
        addPosition("데이터 사이언티스트", Set.of(
                "data scientist", "데이터 사이언티스트", "데이터 과학", "predictive modeling", "a/b test",
                "실험 설계", "추천 시스템", "recommendation"
        ));
        
        // 인프라/시스템 직군
        addPosition("DevOps", Set.of(
                "docker", "kubernetes", "k8s", "terraform", "ci/cd", "devops", "aws", "gcp", "azure",
                "jenkins", "github actions", "gitlab ci", "argocd", "helm", "ansible", "puppet", "chef"
        ));
        addPosition("시스템 엔지니어", Set.of(
                "system engineer", "시스템 엔지니어", "linux", "unix", "network", "네트워크", "infra",
                "인프라", "vmware", "시스템 관리", "서버 관리"
        ));
        addPosition("클라우드 엔지니어", Set.of(
                "cloud engineer", "클라우드 엔지니어", "aws", "gcp", "azure", "cloud", "클라우드",
                "lambda", "serverless", "서버리스", "eks", "ecs", "fargate"
        ));
        addPosition("DBA", Set.of(
                "dba", "database administrator", "데이터베이스 관리자", "mysql dba", "postgresql dba",
                "oracle", "mssql", "db 관리", "database", "db 튜닝"
        ));
        addPosition("SRE", Set.of(
                "sre", "site reliability", "사이트 신뢰성", "monitoring", "모니터링", "prometheus",
                "grafana", "observability", "incident", "장애 대응"
        ));
        
        // 보안 직군
        addPosition("보안 엔지니어", Set.of(
                "security", "보안", "penetration", "pentesting", "vulnerability", "취약점", "정보보안",
                "침투 테스트", "보안 취약점", "시큐리티", "cybersecurity", "soc", "cert"
        ));
        
        // 게임 직군
        addPosition("게임 클라이언트", Set.of(
                "unity", "unreal", "game", "게임", "cocos", "godot", "게임 개발", "c++", "게임 클라이언트"
        ));
        addPosition("게임 서버", Set.of(
                "game server", "게임 서버", "photon", "mirror", "netcode", "멀티플레이어", "multiplayer"
        ));
        
        // 임베디드/시스템 직군
        addPosition("임베디드", Set.of(
                "embedded", "임베디드", "firmware", "펌웨어", "rtos", "arm", "mcu", "아두이노", "라즈베리파이",
                "stm32", "esp32", "iot", "사물인터넷"
        ));
        addPosition("시스템 프로그래머", Set.of(
                "system programmer", "시스템 프로그래머", "c", "c++", "rust", "low-level", "커널",
                "kernel", "driver", "드라이버", "os"
        ));
        
        // QA/테스트 직군
        addPosition("QA 엔지니어", Set.of(
                "qa", "quality assurance", "품질 보증", "test", "테스트", "automation test", "자동화 테스트",
                "selenium", "cypress", "playwright", "appium", "테스트 엔지니어", "sdet"
        ));
        
        // 기획/디자인/PM 직군 (기술 스택과 연관된 경우만)
        addPosition("기술 PM", Set.of(
                "technical pm", "기술 pm", "tpm", "tech lead", "테크 리드", "개발팀장", "engineering manager",
                "엔지니어링 매니저", "agile", "scrum", "스크럼 마스터"
        ));
        addPosition("프로덕트 매니저", Set.of(
                "product manager", "pm", "프로덕트 매니저", "서비스 기획", "product owner", "po",
                "프로덕트 오너", "기획자", "서비스 기획자"
        ));
        addPosition("UX/UI 디자이너", Set.of(
                "ux", "ui", "ux/ui", "ui/ux", "디자이너", "designer", "figma", "sketch", "adobe xd",
                "프로토타이핑", "interaction design", "인터랙션 디자인"
        ));
        
        // 블록체인
        addPosition("블록체인", Set.of(
                "blockchain", "블록체인", "solidity", "web3", "smart contract", "스마트 컨트랙트",
                "ethereum", "이더리움", "defi", "nft", "crypto"
        ));
    }
    
    private static void addPosition(String positionName, Set<String> keywords) {
        POSITION_KEYWORDS.put(positionName, keywords);
        POSITION_ORDER.add(positionName);
    }

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

    /**
     * stack 목록으로부터 매칭되는 UI 포지션 라벨 목록 반환 (필터용)
     * - 서버/백엔드, 프론트엔드, 웹 풀스택, 안드로이드, iOS 등
     */
    public static Set<String> deriveUIPositionLabels(List<String> stack) {
        if (stack == null || stack.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> lower = stack.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (lower.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> labels = new LinkedHashSet<>();

        // 각 포지션 카테고리별로 매칭 확인
        for (String position : POSITION_ORDER) {
            Set<String> keywords = POSITION_KEYWORDS.get(position);
            if (keywords != null && hasMatch(lower, keywords)) {
                labels.add(position);
            }
        }

        // 풀스택: 프론트엔드와 백엔드 둘 다 있으면 추가
        if (labels.contains("서버/백엔드") && labels.contains("프론트엔드")) {
            labels.add("웹 풀스택");
        }

        return labels;
    }

    /**
     * 여러 채용공고의 스택들로부터 존재하는 모든 UI 포지션 라벨 수집
     * @param allStacks 모든 채용공고의 스택 목록 (각 스택은 쉼표로 구분된 문자열 또는 List)
     * @return 정렬된 UI 포지션 라벨 목록 (전체 포함)
     */
    public static List<String> deriveAvailablePositions(List<List<String>> allStacks) {
        if (allStacks == null || allStacks.isEmpty()) {
            return List.of("전체");
        }

        Set<String> allPositions = new LinkedHashSet<>();
        for (List<String> stack : allStacks) {
            allPositions.addAll(deriveUIPositionLabels(stack));
        }

        // 정렬된 순서로 반환 (전체 먼저)
        List<String> result = new ArrayList<>();
        result.add("전체");
        for (String pos : POSITION_ORDER) {
            if (allPositions.contains(pos)) {
                result.add(pos);
            }
        }
        return result;
    }

    /**
     * 단일 스택 문자열(쉼표 구분)을 List로 변환
     */
    public static List<String> parseStackString(String stackStr) {
        if (stackStr == null || stackStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(stackStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static boolean hasMatch(Set<String> stackLower, Set<String> keywords) {
        return stackLower.stream().anyMatch(s -> 
                keywords.stream().anyMatch(kw -> s.contains(kw) || kw.contains(s)));
    }
}
