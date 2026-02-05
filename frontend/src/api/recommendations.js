import { http as api } from "./http";

// AI 추천 공고 조회 (이력서 기반)
export const getAiRecommendations = async (params = {}) => {
    const { limit = 6, location, skills, memberId } = params;

    const response = await api.get('/api/v1/resume/match/recommend', {
        params: {
            memberId,
            limit,
            location,
            skills: skills ? skills.join(',') : undefined,
            ...params
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
