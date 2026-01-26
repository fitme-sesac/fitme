import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getPublicJobs,
  getPublicJob,
  getFilterOptions,
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
 * 채용공고 스크랩 mutation hook
 */
export function useScrapJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: scrapJob,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["publicJobs"] });
      queryClient.invalidateQueries({ queryKey: ["publicJob"] });
    },
  });
}

/**
 * 채용공고 스크랩 취소 mutation hook
 */
export function useUnscrapJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: unscrapJob,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["publicJobs"] });
      queryClient.invalidateQueries({ queryKey: ["publicJob"] });
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
