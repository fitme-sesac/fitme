import { http } from '../../../api/http';

/**
 * 채용공고 관련 API
 * 백엔드 엔드포인트: /api/jobs/*
 */

// 기업의 채용공고 목록 조회
export const getJobPostings = async (params = {}) => {
  const { page = 0, size = 10 } = params;
  const response = await http.get('/api/jobs', {
    params: { page, size },
  });
  return response.data;
};

// 채용공고 상세 조회 (jobUid 사용)
export const getJobPosting = async (jobUid) => {
  const response = await http.get(`/api/jobs/${jobUid}`);
  return response.data;
};

// 채용공고 생성
export const createJobPosting = async (data) => {
  const response = await http.post('/api/jobs', data);
  return response.data;
};

// 채용공고 수정
export const updateJobPosting = async (jobUid, data) => {
  const response = await http.put(`/api/jobs/${jobUid}`, data);
  return response.data;
};

// 채용공고 삭제 (소프트 삭제)
export const deleteJobPosting = async (jobUid) => {
  const response = await http.delete(`/api/jobs/${jobUid}`);
  return response.data;
};

// ===== 스크랩 API =====

// 스크랩 토글 (추가/삭제)
export const toggleScrap = async (jobId) => {
  const response = await http.post(`/api/v1/jobs/${jobId}/scrap`);
  return response.data;
};

// 스크랩 여부 확인
export const getScrapStatus = async (jobId) => {
  const response = await http.get(`/api/v1/jobs/${jobId}/scrap-status`);
  return response.data;
};

// 내 스크랩 목록 조회
export const getMyScrapList = async (page = 0, size = 10) => {
  const response = await http.get('/api/v1/jobs/scraps', {
    params: { page, size },
  });
  return response.data;
};

// 내 스크랩 수 조회
export const getScrapCount = async () => {
  const response = await http.get('/api/v1/jobs/scraps/count');
  return response.data;
};

// 최근 본 공고 목록 조회
export const getRecentViewedJobs = async (limit = 10) => {
  const response = await http.get('/api/v1/jobs/recent-views', {
    params: { limit },
  });
  return response.data;
};

export default {
  getJobPostings,
  getJobPosting,
  createJobPosting,
  updateJobPosting,
  deleteJobPosting,
  toggleScrap,
  getScrapStatus,
  getMyScrapList,
  getScrapCount,
  getRecentViewedJobs,
};
