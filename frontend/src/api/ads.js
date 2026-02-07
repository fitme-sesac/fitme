import { http as api } from "./http";

/**
 * 광고 서빙 API (V3 Hybrid 기반)
 * 
 * @param {Object} params - 검색 필터 및 사용자 정보
 * @returns {Promise<Array>} 광고 목록
 */
export const getMatchedAds = async (params = {}) => {
    const { memberId, limit = 10 } = params;

    const response = await api.get('/api/v2/ad/serve/match', {
        params: {
            memberId,
            limit
        }
    });

    return response.data || [];
};

/**
 * 일반 광고 노출 (비로그인용)
 */
export const getPublicAds = async (limit = 10) => {
    const response = await api.get('/api/v2/ad/serve', {
        params: { limit }
    });

    return response.data || [];
};

/**
 * 광고 클릭 이벤트 추적 (V2 Redis 기반)
 * - 클릭 발생 시 서버로 이벤트 전송
 * - Redis 기반 실시간 과금 처리
 * 
 * @param {Object} params - { campaignId, memberId }
 * @returns {Promise<void>}
 */
export const trackAdClick = async (params = {}) => {
    const { campaignId, memberId } = params;

    // 고유 클릭 키 생성 (중복 클릭 방지용)
    const clickKey = `${campaignId}_${memberId || 'guest'}_${Date.now()}`;

    try {
        await api.post('/api/v2/ad/clicks', {
            campaignId,
            memberId: memberId || null,
            clickKey
        });
    } catch (error) {
        // 클릭 추적 실패는 사용자 경험에 영향을 주지 않도록 조용히 처리
        console.warn('[Ad Click] 클릭 추적 실패:', error);
    }
};
