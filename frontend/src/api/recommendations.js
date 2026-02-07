import { http as api } from "./http";

// AI 추천 공고 조회 (이력서 기반)
// - 기존: /api/v1/resume/recommend (백엔드가 JWT에서 memberId 추출)
// - 신규: /api/v1/resume/match/recommend (memberId를 쿼리로 받는 흐름)
// ✅ 병합 전략: memberId가 있으면 match/recommend 사용, 없으면 기존 recommend 사용
export const getAiRecommendations = async (params = {}) => {
    const { limit = 6, location, skills, memberId, ...rest } = params;

    const queryParams = {
        limit,
        location,
        ...(typeof memberId !== "undefined" && memberId !== null ? { memberId } : {}),
        ...(skills ? { skills: skills.join(",") } : {}),
        ...rest, // 추가 필터가 있으면 그대로 전달
    };

    const endpoint = (typeof memberId !== "undefined" && memberId !== null)
        ? "/api/v1/resume/match/recommend"
        : "/api/v1/resume/recommend";

    const response = await api.get(endpoint, { params: queryParams });
    return response.data;
};

// 최근 본 공고 조회
export const getRecentlyViewedJobs = async (limit = 6) => {
    const response = await api.get("/public/jobs/recently-viewed", {
        params: { limit },
    });
    return response.data;
};
