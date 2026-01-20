import { http } from '../../../api/http';

/**
 * 기업 관련 API
 * 백엔드 엔드포인트: /api/employer/*
 */

// 현재 로그인한 사용자의 기업 프로필 조회
export const getMyEmployer = async () => {
  const response = await http.get('/api/employer/profile');
  return response.data;
};

// 기업 프로필 등록/수정
export const saveEmployerProfile = async (data) => {
  const response = await http.post('/api/employer/profile', data);
  return response.data;
};

// 대시보드 정보 조회 (통계 포함)
export const getDashboard = async () => {
  const response = await http.get('/api/employer/dashboard');
  return response.data;
};

// 대시보드 통계 조회 (getDashboard의 별칭)
export const getDashboardStats = async () => {
  const response = await http.get('/api/employer/dashboard');
  return response.data;
};

export default {
  getMyEmployer,
  saveEmployerProfile,
  getDashboard,
  getDashboardStats,
};
