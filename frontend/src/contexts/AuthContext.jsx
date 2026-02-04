import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import { http } from "@/api/http";
import * as authApi from "@/api/auth";
import { getMyWallet } from "@/api/wallet";

const AuthContext = createContext(undefined);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [credits, setCredits] = useState(0);

    const extractErrorMessage = (err, fallback) => {
        if (!err) return fallback;
        if (typeof err === "string") return err;

        // Axios-style error
        const axiosMsg = err?.response?.data?.message || err?.response?.data?.error;
        if (axiosMsg) return axiosMsg;

        // ✅ 백엔드가 redirect URL query(errorMessage=...)로 에러를 전달하는 경우
        // - XHR이 redirect를 따라가면서 최종 responseURL에 남는다.
        // - proxy/환경에 따라 최종 요청이 4xx/5xx로 떨어질 수 있으므로 여기서도 파싱한다.
        const finalUrl =
            err?.response?.request?.responseURL ||
            err?.request?.responseURL ||
            err?.config?.url ||
            "";
        if (finalUrl) {
            try {
                const u = new URL(finalUrl, window.location.origin);
                const p = u.searchParams;
                const msg = p.get("errorMessage") || p.get("error") || p.get("message");
                if (msg) return decodeURIComponent(msg);
            } catch {
                // ignore
            }
        }

        // authApi may throw plain object like { ok:false, message: "..." }
        if (typeof err?.message === "string" && err.message.trim()) return err.message;

        // generic
        return fallback;
    };

    const refreshCredits = useCallback(async () => {
        try {
            const res = await getMyWallet();
            const c = Number(res?.credits ?? 0);
            setCredits(Number.isFinite(c) ? c : 0);
        } catch {
            // 지갑 API 실패(401/400 등) 시 크레딧만 0으로 표시, 로그인 상태는 유지
            setCredits(0);
        }
    }, []);

    // 세션 확인 (외부 노출용)
    const checkSession = useCallback(async () => {
        try {
            const status = await authApi.checkAuthStatus();
            setUser(status?.authenticated ? status : null);
            // Pass the user role to ensure we fetch the correct wallet
            await refreshCredits(status?.role);
            return status;
        } catch {
            setUser(null);
            setCredits(0);
            return null;
        }
    }, [refreshCredits]);

    // 로그인
    const signIn = async (userid, password) => {
        try {
            const response = await authApi.login(userid, password);
            // 로그인 성공시 상태 갱신
            const status = await authApi.checkAuthStatus();
            if (!status?.authenticated) {
                setUser(null);
                setCredits(0);
                return { data: null, error: "로그인 상태 확인에 실패했습니다." };
            }
            setUser(status);
            // Pass the user role to ensure we fetch the correct wallet
            await refreshCredits(status?.role);
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: extractErrorMessage(error, "로그인에 실패했습니다.") };
        }
    };

    // 회원가입
    const signUp = async (userData) => {
        try {
            const response = await authApi.register(userData);
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: extractErrorMessage(error, "회원가입에 실패했습니다.") };
        }
    };

    // 로그아웃
    const signOut = async () => {
        try {
            await authApi.logout();
        } catch {
            // ignore
        } finally {
            setUser(null);
            setCredits(0);
        }
    };

    // 구글/카카오/네이버 소셜 로그인 URL로 이동
    const signInWithGoogle = () => {
        window.location.href = authApi.getOAuth2AuthorizationUrl("google");
    };
    const signInWithKakao = () => {
        window.location.href = authApi.getOAuth2AuthorizationUrl("kakao");
    };
    const signInWithNaver = () => {
        window.location.href = authApi.getOAuth2AuthorizationUrl("naver");
    };

    // 아이디 찾기(휴대폰)
    const findUserId = async (name, phone) => {
        try {
            const response = await authApi.findUserId(name, phone);
            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: extractErrorMessage(error, "아이디 찾기에 실패했습니다.")
            };
        }
    };

    // 아이디 찾기 코드 검증
    const verifyUserIdCode = async (phone, code) => {
        try {
            const response = await authApi.verifyUserIdCode(phone, code);
            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: extractErrorMessage(error, "인증 코드 확인에 실패했습니다.")
            };
        }
    };

    // 아이디 찾기 결과
    const getUserIdResult = async () => {
        try {
            const response = await authApi.getUserIdResult();
            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: extractErrorMessage(error, "아이디 조회에 실패했습니다.")
            };
        }
    };

    // 비밀번호 찾기 (코드 발송)
    const findPassword = async (userid, email) => {
        try {
            const response = await authApi.findPassword(userid, email);
            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: error.response?.data?.error || "비밀번호 찾기에 실패했습니다."
            };
        }
    };

    // 비밀번호 코드 검증
    const verifyPasswordCode = async (userid, email, code) => {
        try {
            const response = await authApi.verifyPasswordCode(userid, email, code);
            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: error.response?.data?.error || "인증 코드 확인에 실패했습니다."
            };
        }
    };

    // 비밀번호 재설정
    const setNewPassword = async (userid, email, newPassword) => {
        try {
            const response = await authApi.setNewPassword(userid, email, newPassword);
            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: error.response?.data?.error || "비밀번호 재설정에 실패했습니다."
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

            // ✅ 백엔드가 ok=false 로 내려주면(HTTP 400/502 포함) 프론트에서는 실패로 통일 처리
            if (!response?.ok) {
                return {
                    data: response ?? null,
                    error: response?.message || "인증번호 발송에 실패했습니다.",
                };
            }

            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: extractErrorMessage(error, "인증번호 발송에 실패했습니다."),
            };
        }
    };

    // 휴대폰 인증 확인
    const verifyPhone = async (phone, code) => {
        try {
            const response = await authApi.verifyPhone(phone, code);

            const ok = Boolean(response?.verified ?? response?.ok);

            if (!ok) {
                return {
                    data: response ?? null,
                    error: response?.message || "인증번호가 일치하지 않습니다.",
                };
            }

            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: extractErrorMessage(error, "인증 확인에 실패했습니다."),
            };
        }
    };

    // 프로필 업데이트
    const updateProfile = async (formData) => {
        try {
            const response = await authApi.updateProfile(formData);
            // 업데이트 성공 시 세션 갱신
            await checkSession();
            return { data: response, error: null };
        } catch (error) {
            return {
                data: null,
                error: extractErrorMessage(error, "프로필 업데이트에 실패했습니다."),
            };
        }
    };

    // Role helpers
    const userRole = user?.role || "";
    // Backend RoleType: EMPLOYER
    const isCompany = userRole === "EMPLOYER" || userRole === "ROLE_COMPANY"; // keeping legacy fallback just in case
    // Backend RoleType: SERVICEADMIN, APPROVEADMIN, MASTER
    const isAdmin = ["SERVICEADMIN", "APPROVEADMIN", "MASTER", "ADMIN", "ROLE_ADMIN"].includes(userRole);
    // Backend RoleType: CANDIDATE
    const isJobSeeker = userRole === "CANDIDATE" || userRole === "JOB_SEEKER" || userRole === "USER";

    const value = {
        user,
        userRole,
        isCompany,
        isAdmin,
        isJobSeeker,
        loading,
        credits,
        refreshCredits,
        checkSession,
        updateProfile,
        signIn,
        signUp,
        signOut,
        signInWithGoogle,
        signInWithKakao,
        signInWithNaver,
        findUserId,
        verifyUserIdCode,
        getUserIdResult,
        findPassword,
        verifyPasswordCode,
        setNewPassword,
        changePassword,
        requestPhoneVerification,
        verifyPhone,
    };

    useEffect(() => {
        (async () => {
            try {
                const status = await authApi.checkAuthStatus();
                // authenticated=false인 경우에는 user를 null로 유지해서
                // 라우트 가드/헤더 등에서 로그인 상태가 정확히 표시되도록 함
                setUser(status?.authenticated ? status : null);
                // Pass the user role to ensure we fetch the correct wallet
                await refreshCredits(status?.role);
            } catch {
                setUser(null);
                setCredits(0);
            } finally {
                setLoading(false);
            }
        })();
    }, [refreshCredits]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return ctx;
}
