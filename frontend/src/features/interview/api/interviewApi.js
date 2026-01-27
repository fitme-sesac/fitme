import http from '../../../api/http';

/**
 * 면접 일정 API
 */

// 면접 생성 (기업용)
export const createInterview = async (data) => {
  const response = await http.post('/api/v1/interviews', data);
  return response.data;
};

// 면접 응답 (지원자용)
export const respondToInterview = async (interviewId, data) => {
  const response = await http.post(`/api/v1/interviews/${interviewId}/respond`, data);
  return response.data;
};

// 면접 취소
export const cancelInterview = async (interviewId) => {
  const response = await http.put(`/api/v1/interviews/${interviewId}/cancel`);
  return response.data;
};

// 면접 완료 (기업용)
export const completeInterview = async (interviewId) => {
  const response = await http.put(`/api/v1/interviews/${interviewId}/complete`);
  return response.data;
};

// 내 면접 일정 조회 (지원자용)
export const getMyInterviews = async () => {
  const response = await http.get('/api/v1/interviews/me');
  return response.data;
};

// 다가오는 면접 조회 (지원자용)
export const getUpcomingInterviews = async () => {
  const response = await http.get('/api/v1/interviews/me/upcoming');
  return response.data;
};

// 기업 면접 일정 조회
export const getEmployerInterviews = async () => {
  const response = await http.get('/api/v1/interviews/employer');
  return response.data;
};

// 기업 다가오는 면접 조회
export const getEmployerUpcomingInterviews = async () => {
  const response = await http.get('/api/v1/interviews/employer/upcoming');
  return response.data;
};

// 특정 지원의 면접 일정 조회
export const getInterviewsByApplication = async (applicationId) => {
  const response = await http.get(`/api/v1/interviews/application/${applicationId}`);
  return response.data;
};

// 면접 상세 조회
export const getInterview = async (interviewId) => {
  const response = await http.get(`/api/v1/interviews/${interviewId}`);
  return response.data;
};

export default {
  createInterview,
  respondToInterview,
  cancelInterview,
  completeInterview,
  getMyInterviews,
  getUpcomingInterviews,
  getEmployerInterviews,
  getEmployerUpcomingInterviews,
  getInterviewsByApplication,
  getInterview,
};
