import { http } from '../../../api/http';

/**
 * 회원 관련 API
 * 백엔드 엔드포인트: /api/user/*
 */

// 현재 로그인한 사용자의 회원 정보 조회
export const getMyProfile = async () => {
  const response = await http.get('/api/user/profile');
  return response.data;
};

// 회원 정보 수정
export const updateProfile = async (data) => {
  const response = await http.put('/api/user/profile', data);
  return response.data;
};

// 비밀번호 변경
export const updatePassword = async (data) => {
  const response = await http.put('/api/user/password', data);
  return response.data;
};

// 휴대폰 인증번호 발송 (회원정보 수정용)
export const sendPhoneOtp = async (phone) => {
  const response = await http.post('/api/phone/otp/send', { 
    phone, 
    purpose: 'PROFILE_UPDATE' 
  });
  return response.data;
};

// 휴대폰 인증번호 확인
export const verifyPhoneOtp = async (phone, code) => {
  const response = await http.post('/api/phone/otp/verify', { phone, code });
  return response.data;
};

export default {
  getMyProfile,
  updateProfile,
  updatePassword,
  sendPhoneOtp,
  verifyPhoneOtp,
};
