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

export default {
  getJobPostings,
  getJobPosting,
  createJobPosting,
  updateJobPosting,
  deleteJobPosting,
};
