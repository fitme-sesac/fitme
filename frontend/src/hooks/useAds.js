import { useQuery } from "@tanstack/react-query";
import { getMatchedAds, getPublicAds } from "@/api/ads";

/**
 * 광고 서빙 전용 훅
 * - 로그인 시: 맞춤형 광고 매칭 (V3)
 * - 비로그인 시: 입찰가 기반 일반 광고 노출 (V3)
 * 
 * @param {Object} options - { memberId, limit }
 */
export const useAds = (options = {}) => {
    const { memberId, limit = 10 } = options;

    return useQuery({
        queryKey: ["ads", memberId, limit],
        queryFn: () => {
            if (memberId) {
                return getMatchedAds({ memberId, limit });
            }
            return getPublicAds(limit);
        },
        // 광고 데이터는 자주 변경되거나 갱신될 필요가 있으므로 staleTime을 짧게 설정하거나 조절
        staleTime: 1000 * 60 * 5, // 5분
    });
};
