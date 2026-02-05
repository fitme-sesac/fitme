import { http as api } from "./http";

// AI 추천 공고 조회 (이력서 기반)
// 백엔드가 JWT 토큰에서 memberId를 자동으로 추출하므로 별도 파라미터 불필요
export const getAiRecommendations = async (params = {}) => {
    const { limit = 6, location, skills } = params;

    const response = await api.get('/api/v1/resume/recommend', {
        params: {
            limit,
            location,
            skills: skills ? skills.join(',') : undefined,
        }
    });
    return response.data;
};

// 최근 본 공고 조회
export const getRecentlyViewedJobs = async (limit = 6) => {
    const response = await api.get('/public/jobs/recently-viewed', {
        params: { limit }
    });
    return response.data;
};
