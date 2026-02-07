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

        // 백엔드가 redirect URL query(errorMessage=...)로 에러를 전달하는 경우 파싱
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

        return fallback;
    };

    const inferRoleType = (role) => {
        // Backend RoleType 기준:
        // EMPLOYER / CANDIDATE (또는 COMPANY/ROLE_* fallback)
        let roleType = "CANDIDATE";
        if (role && (String(role).includes("EMPLOYER") || String(role).includes("COMPANY"))) {
            roleType = "EMPLOYER";
        }
        return roleType;
    };

    /**
     * 크레딧(지갑) 갱신
     * - roleType 지원(EMPLOYER/CANDIDATE)
     * - 백엔드 응답키(balance 또는 credits) 모두 지원
     * - 실패 시 기본은 "throw" (호출부에서 로그인/로그아웃 정책에 맞게 처리)
     */
    const refreshCredits = useCallback(async (role) => {
        const roleType = inferRoleType(role);

        // getMyWallet이 (roleType)을 받는 버전 / 안 받는 버전 모두 대응
        // JS에서는 extra args 무시되므로 getMyWallet(roleType)만 호출해도 대부분 안전하지만,
        // 혹시 구현에 따라 예외가 날 수 있어 try/catch로 보호.
        let res;
        try {
            res = await getMyWallet(roleType);
        } catch (e) {
            // 파라미터 없는 구현일 수도 있으니 한 번 더 시도
            res = await getMyWallet();
        }

        // Backend returns 'balance' OR legacy 'credits'
        const c = Number(res?.balance ?? res?.credits ?? 0);
        setCredits(Number.isFinite(c) ? c : 0);

        return res;
    }, []);

    // 세션 확인 (외부 노출용)
    const checkSession = useCallback(async () => {
        try {
            const status = await authApi.checkAuthStatus();
            const authed = Boolean(status?.authenticated);

            setUser(authed ? status : null);

            if (authed) {
                try {
                    await refreshCredits(status?.role);
                } catch (error) {
                    console.error("Failed to refresh credits during session check:", error);
                    // 지갑 조회 실패 시: 로그인 상태 유지, credits는 유지(0으로 강제 초기화하지 않음)
                }
            } else {
                // 로그아웃 상태일 때만 credits 0
                setCredits(0);
            }

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

            // 로그인 성공 시 상태 갱신
            const status = await authApi.checkAuthStatus();
            if (!status?.authenticated) {
                setUser(null);
                setCredits(0);
                return { data: null, error: "로그인 상태 확인에 실패했습니다." };
            }

            setUser(status);

            try {
                await refreshCredits(status?.role);
            } catch (error) {
                console.error("Failed to refresh credits during sign in:", error);
                // 로그인은 성공했지만 지갑 조회 실패 시 credits 유지
            }

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
            return { data: null, error: extractErrorMessage(error, "아이디 찾기에 실패했습니다.") };
        }
    };

    // 아이디 찾기 코드 검증
    const verifyUserIdCode = async (phone, code) => {
        try {
            const response = await authApi.verifyUserIdCode(phone, code);
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: extractErrorMessage(error, "인증 코드 확인에 실패했습니다.") };
        }
    };

    // 아이디 찾기 결과
    const getUserIdResult = async () => {
        try {
            const response = await authApi.getUserIdResult();
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: extractErrorMessage(error, "아이디 조회에 실패했습니다.") };
        }
    };

    // 비밀번호 찾기 (코드 발송)
    const findPassword = async (userid, email) => {
        try {
            const response = await authApi.findPassword(userid, email);
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: error.response?.data?.error || "비밀번호 찾기에 실패했습니다." };
        }
    };

    // 비밀번호 코드 검증
    const verifyPasswordCode = async (userid, email, code) => {
        try {
            const response = await authApi.verifyPasswordCode(userid, email, code);
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: error.response?.data?.error || "인증 코드 확인에 실패했습니다." };
        }
    };

    // 비밀번호 재설정
    const setNewPassword = async (userid, email, newPassword) => {
        try {
            const response = await authApi.setNewPassword(userid, email, newPassword);
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: error.response?.data?.error || "비밀번호 재설정에 실패했습니다." };
        }
    };

    // 비밀번호 변경
    const changePassword = async (currentPassword, newPassword) => {
        try {
            const response = await authApi.changePassword(currentPassword, newPassword);
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: error.response?.data?.error || "비밀번호 변경에 실패했습니다." };
        }
    };

    // 휴대폰 인증 요청
    const requestPhoneVerification = async (phone) => {
        try {
            const response = await authApi.requestPhoneVerification(phone);

            // 백엔드가 ok=false 로 내려주면 실패로 통일 처리
            if (!response?.ok) {
                return {
                    data: response ?? null,
                    error: response?.message || "인증번호 발송에 실패했습니다.",
                };
            }

            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: extractErrorMessage(error, "인증번호 발송에 실패했습니다.") };
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
            return { data: null, error: extractErrorMessage(error, "인증 확인에 실패했습니다.") };
        }
    };

    // 프로필 업데이트
    const updateProfile = async (formData) => {
        try {
            const response = await authApi.updateProfile(formData);
            await checkSession();
            return { data: response, error: null };
        } catch (error) {
            return { data: null, error: extractErrorMessage(error, "프로필 업데이트에 실패했습니다.") };
        }
    };

    // Role helpers
    const userRole = user?.role || "";
    const isCompany = userRole === "EMPLOYER" || userRole === "ROLE_COMPANY";
    const isAdmin = ["SERVICEADMIN", "APPROVEADMIN", "MASTER", "ADMIN", "ROLE_ADMIN"].includes(userRole);
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
                const authed = Boolean(status?.authenticated);

                setUser(authed ? status : null);

                if (authed) {
                    try {
                        await refreshCredits(status?.role);
                    } catch (error) {
                        console.error("Failed to refresh credits during initialization:", error);
                        // 초기화 시 지갑 조회 실패해도 로그인 상태 유지, credits 유지
                    }
                } else {
                    setCredits(0);
                }
            } catch {
                setUser(null);
                setCredits(0);
            } finally {
                setLoading(false);
            }
        })();
    }, [refreshCredits]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
    return ctx;
}
