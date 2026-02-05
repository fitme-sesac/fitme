import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getPublicJobs,
  getPublicJob,
  getFilterOptions,
  getPositionCounts,
  scrapJob,
  unscrapJob,
  applyToJob,
} from "@/api/jobs";

/**
 * 공개 채용공고 목록 조회 hook
 * @param {Object} params - 검색/필터 파라미터
 */
export function usePublicJobs(params = {}) {
  return useQuery({
    queryKey: ["publicJobs", params],
    queryFn: () => getPublicJobs(params),
    staleTime: 1000 * 60 * 5, // 5분
  });
}

/**
 * 공개 채용공고 상세 조회 hook
 * @param {number} jobId - 채용공고 ID
 */
export function usePublicJob(jobId) {
  return useQuery({
    queryKey: ["publicJob", jobId],
    queryFn: () => getPublicJob(jobId),
    enabled: !!jobId,
  });
}

/**
 * 필터 옵션 조회 hook
 */
export function useFilterOptions() {
  return useQuery({
    queryKey: ["filterOptions"],
    queryFn: getFilterOptions,
    staleTime: 1000 * 60 * 30, // 30분
  });
}

/**
 * 포지션별 카운트 조회 hook
 * @param {Object} params - 필터 파라미터 (현재 적용된 필터 조건)
 */
export function usePositionCounts(params = {}) {
  return useQuery({
    queryKey: ["positionCounts", params],
    queryFn: () => getPositionCounts(params),
    staleTime: 1000 * 60 * 2, // 2분
  });
}

/**
 * 채용공고 스크랩 mutation hook
 * 스크랩 후 마이페이지 관련 쿼리도 함께 무효화
 */
export function useScrapJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: scrapJob,
    onSuccess: () => {
      // 채용공고 목록 갱신
      queryClient.invalidateQueries({ queryKey: ["publicJobs"] });
      queryClient.invalidateQueries({ queryKey: ["publicJob"] });
      // 마이페이지 관련 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: ["profileSummary"] });
      queryClient.invalidateQueries({ queryKey: ["scrapedJobs"] });
      queryClient.invalidateQueries({ queryKey: ["myScrapedJobs"] });
    },
  });
}

/**
 * 채용공고 스크랩 취소 mutation hook
 * 스크랩 후 마이페이지 관련 쿼리도 함께 무효화
 */
export function useUnscrapJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: unscrapJob,
    onSuccess: () => {
      // 채용공고 목록 갱신
      queryClient.invalidateQueries({ queryKey: ["publicJobs"] });
      queryClient.invalidateQueries({ queryKey: ["publicJob"] });
      // 마이페이지 관련 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: ["profileSummary"] });
      queryClient.invalidateQueries({ queryKey: ["scrapedJobs"] });
      queryClient.invalidateQueries({ queryKey: ["myScrapedJobs"] });
    },
  });
}

/**
 * 채용공고 지원 mutation hook
 */
export function useApplyToJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ jobId, application }) => applyToJob(jobId, application),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["publicJob"] });
    },
  });
}
