import { http as api } from "./http";

// AI 추천 공고 조회 (이력서 기반)
export const getAiRecommendations = async (params = {}) => {
    const { limit = 6, location, skills } = params;

    // memberId는 백엔드에서 토큰으로 추출하므로 여기서는 필요 시 query param으로 보낼 수도 있음
    // 하지만 ResumeMatchController가 memberId를 RequestParam으로 요구하므로, 
    // 현재 로그인한 사용자 ID를 알아야 함.
    // 프론트엔드에서 user 객체에서 id를 가져와서 넘겨야 함.

    const response = await api.get('/api/v1/resume/recommend', {
        params: {
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
