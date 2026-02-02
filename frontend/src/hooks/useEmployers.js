import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getPublicEmployers,
  getPublicEmployer,
  getEmployerDashboard,
  getEmployerProfile,
  saveEmployerProfile,
  getApplicants,
  updateApplicantStatus,
  getInterviews,
  createInterview,
  updateInterview,
  deleteInterview,
  getAdStats,
} from "@/api/employers";

/**
 * 공개 기업 목록 조회 hook
 * @param {Object} params - 검색/필터 파라미터
 */
export function usePublicEmployers(params = {}) {
  return useQuery({
    queryKey: ["publicEmployers", params],
    queryFn: () => getPublicEmployers(params),
    staleTime: 1000 * 60 * 5, // 5분
  });
}

/**
 * 공개 기업 상세 조회 hook
 * @param {number} employerId - 기업 ID
 */
export function usePublicEmployer(employerId) {
  return useQuery({
    queryKey: ["publicEmployer", employerId],
    queryFn: () => getPublicEmployer(employerId),
    enabled: !!employerId,
  });
}

/**
 * 기업 대시보드 조회 hook (로그인 필요)
 */
export function useEmployerDashboard() {
  return useQuery({
    queryKey: ["employerDashboard"],
    queryFn: getEmployerDashboard,
    staleTime: 1000 * 60 * 2, // 2분
  });
}

/**
 * 기업 프로필 조회 hook (로그인 필요)
 */
export function useEmployerProfile() {
  return useQuery({
    queryKey: ["employerProfile"],
    queryFn: getEmployerProfile,
  });
}

/**
 * 기업 프로필 저장 mutation hook
 */
export function useSaveEmployerProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: saveEmployerProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["employerProfile"] });
      queryClient.invalidateQueries({ queryKey: ["employerDashboard"] });
    },
  });
}

/**
 * 지원자 목록 조회 hook (로그인 필요)
 * @param {string} status - 상태 필터
 */
export function useApplicants(status = "") {
  return useQuery({
    queryKey: ["applicants", status],
    queryFn: () => getApplicants(status),
  });
}

/**
 * 지원자 상태 변경 mutation hook
 */
export function useUpdateApplicantStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ applicationId, status }) =>
      updateApplicantStatus(applicationId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["applicants"] });
      queryClient.invalidateQueries({ queryKey: ["employerDashboard"] });
    },
  });
}

/**
 * 면접 일정 목록 조회 hook (로그인 필요)
 * @param {number} year - 연도
 * @param {number} month - 월
 */
export function useInterviews(year, month) {
  return useQuery({
    queryKey: ["interviews", year, month],
    queryFn: () => getInterviews(year, month),
  });
}

/**
 * 면접 일정 생성 mutation hook
 */
export function useCreateInterview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createInterview,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["interviews"] });
    },
  });
}

/**
 * 면접 일정 수정 mutation hook
 */
export function useUpdateInterview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ interviewId, interview }) =>
      updateInterview(interviewId, interview),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["interviews"] });
    },
  });
}

/**
 * 면접 일정 삭제 mutation hook
 */
export function useDeleteInterview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteInterview,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["interviews"] });
    },
  });
}

/**
 * 광고 통계 조회 hook (로그인 필요)
 */
export function useAdStats() {
  return useQuery({
    queryKey: ["adStats"],
    queryFn: getAdStats,
    staleTime: 1000 * 60 * 5, // 5분
  });
}
