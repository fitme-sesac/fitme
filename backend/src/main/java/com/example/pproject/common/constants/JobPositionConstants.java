package com.example.pproject.common.constants;

import java.util.List;

/**
 * 포지션 카테고리 상수 (단일 소스)
 * - LLM, 프론트엔드, 필터 API 등에서 포지션 목록을 불러올 때 이 클래스를 사용한다.
 * - API용 3종(프론트엔드/백엔드/풀스택)은 JobPositionUtil.derivePosition()과 동일.
 * - UI용 전체 라벨은 필터/채팅/LLM 프롬프트 등에서 동일하게 사용.
 */
public final class JobPositionConstants {

    private JobPositionConstants() {
    }

    /** API/백엔드에서 사용하는 포지션 값 (JobPositionUtil.derivePosition() 반환값과 동일) */
    public static final List<String> API_POSITIONS = List.of(
            "프론트엔드",
            "백엔드",
            "풀스택"
    );

    /**
     * UI/필터용 포지션 라벨 전체 목록 (프론트 필터 옵션과 동일)
     * - LLM에서 포지션 목록이 필요하면 이 값을 사용하거나, GET /api/public/jobs/filter-options 의 positionCategories 로 조회.
     */
    public static final List<String> DISPLAY_POSITION_LABELS = List.of(
            "전체",
            "서버/백엔드",
            "프론트엔드",
            "웹 풀스택",
            "안드로이드",
            "iOS",
            "머신러닝/AI",
            "데이터 엔지니어",
            "DevOps",
            "게임 클라이언트",
            "보안 엔진"
    );

    /** 필터 옵션용 (전체 제외) - "전체" 없이 포지션만 */
    public static final List<String> POSITION_FILTER_LABELS = List.of(
            "서버/백엔드",
            "프론트엔드",
            "웹 풀스택",
            "안드로이드",
            "iOS",
            "머신러닝/AI",
            "데이터 엔지니어",
            "DevOps",
            "게임 클라이언트",
            "보안 엔진"
    );
}
