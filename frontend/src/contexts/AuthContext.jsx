import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import { http } from "@/api/http";
import * as authApi from "@/api/auth";

const AuthContext = createContext(undefined);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // 세션 확인
  const checkSession = useCallback(async () => {
    try {
      const res = await http.get('/api/auth/status');
      if (res.data?.authenticated) {
        setUser({
          id: res.data.memberId,
          email: res.data.email,
          role: res.data.role,
          user_metadata: {
            display_name: res.data.name,
            avatar_url: res.data.avatarUrl,
          }
        });
      } else {
        setUser(null);
      }
    } catch (error) {
      console.error('Session check failed:', error);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkSession();
  }, [checkSession]);

  // 회원가입
  const signUp = async (userData) => {
    try {
      const response = await authApi.register(userData);
      // 회원가입 성공 후 자동 로그인 시도
      if (response && !response.error) {
        await checkSession();
      }
      return { data: response, error: null };
    } catch (error) {
      console.error('Sign up failed:', error);
      return {
        data: null,
        error: error.response?.data?.error || error.message || "회원가입에 실패했습니다."
      };
    }
  };

  // 로그인
  const signIn = async (loginId, password) => {
    try {
      const formData = new URLSearchParams();
      formData.append("userid", loginId);
      formData.append("password", password);

      const response = await http.post("/Login", formData, {
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
      });

      // 로그인 성공 후 세션 확인
      await checkSession();
      return { data: response.data, error: null };
    } catch (error) {
      console.error('Sign in failed:', error);
      return {
        data: null,
        error: error.response?.data?.error || "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요."
      };
    }
  };

  // 소셜 로그인 (리다이렉트)
  const signInWithGoogle = () => {
    window.location.href = authApi.getSocialLoginUrl("google");
  };

  const signInWithKakao = () => {
    window.location.href = authApi.getSocialLoginUrl("kakao");
  };

  const signInWithNaver = () => {
    window.location.href = authApi.getSocialLoginUrl("naver");
  };

  // 로그아웃
  const signOut = async () => {
    try {
      await http.post('/Logout');
      setUser(null);
      window.location.href = '/';
    } catch (error) {
      console.error('Logout failed:', error);
      // 에러가 발생해도 로컬 상태는 클리어
      setUser(null);
      window.location.href = '/';
    }
  };

  // 아이디 찾기
  const findUserId = async (name, phone) => {
    try {
      const response = await authApi.findUserId(name, phone);
      return { data: response, error: null };
    } catch (error) {
      return {
        data: null,
        error: error.response?.data?.error || "아이디 찾기에 실패했습니다."
      };
    }
  };

  // 비밀번호 찾기
  const findPassword = async (loginId, phone) => {
    try {
      const response = await authApi.findPassword(loginId, phone);
      return { data: response, error: null };
    } catch (error) {
      return {
        data: null,
        error: error.response?.data?.error || "비밀번호 찾기에 실패했습니다."
      };
    }
  };

  // 비밀번호 변경
  const changePassword = async (currentPassword, newPassword) => {
    try {
      const response = await authApi.changePassword(currentPassword, newPassword);
      return { data: response, error: null };
    } catch (error) {
      return {
        data: null,
        error: error.response?.data?.error || "비밀번호 변경에 실패했습니다."
      };
    }
  };

  // 휴대폰 인증 요청
  const requestPhoneVerification = async (phone) => {
    try {
      const response = await authApi.requestPhoneVerification(phone);
      return { data: response, error: null };
    } catch (error) {
      return {
        data: null,
        error: error.response?.data?.error || "인증번호 발송에 실패했습니다."
      };
    }
  };

  // 휴대폰 인증 확인
  const verifyPhone = async (phone, code) => {
    try {
      const response = await authApi.verifyPhone(phone, code);
      return { data: response, error: null };
    } catch (error) {
      return {
        data: null,
        error: error.response?.data?.error || "인증에 실패했습니다."
      };
    }
  };

  // page-making-friend 호환용 프로퍼티
  const isCompany = user?.role === 'EMPLOYER';
  const isAdmin = ['SERVICEADMIN', 'APPROVEADMIN', 'MASTER'].includes(user?.role);
  const userRole = user?.role || null;
  const profile = user ? {
    display_name: user.user_metadata?.display_name || user.email,
    user_type: user.role === 'EMPLOYER' ? 'company' : 'job_seeker',
  } : null;

  const value = {
    user,
    loading,
    isAuthenticated: !!user,
    isCompany,  // 기업 회원 여부
    isAdmin,    // 관리자 여부
    userRole,   // 원본 역할 문자열
    profile,    // page-making-friend 호환용
    signUp,
    signIn,
    signOut,
    signInWithGoogle,
    signInWithKakao,
    signInWithNaver,
    findUserId,
    findPassword,
    changePassword,
    requestPhoneVerification,
    verifyPhone,
    updateProfile: authApi.updateProfile,
    checkSession,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
