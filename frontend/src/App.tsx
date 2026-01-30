// App.tsx
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { useEffect } from "react";

// ✅ 추가
import { AuthProvider } from "@/contexts/AuthContext";

import AppLayout from "./layout/AppLayout";
import HomePage from "./pages/HomePage";
import Jobs from "./pages/Jobs";
import JobDetail from "./pages/JobDetail";
import Talents from "./pages/Talents";
import Companies from "./pages/Companies";
import CompanyDetail from "./pages/CompanyDetail";
import Community from "./pages/Community";
import Interview from "./pages/Interview";
import Resume from "./pages/Resume";
import Subscription from "./pages/Subscription";
import MyPage from "./pages/MyPage";
import CompanyDashboard from "./pages/CompanyDashboard";
import JobSeekerMyPage from "./pages/JobSeekerMyPage";
import AdminDashboard from "./pages/AdminDashboard";
import NotFound from "./pages/NotFound";
import Support from "./pages/Support";
import Auth from "./pages/Auth";

import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import FindUserIdPage from "./pages/FindUserIdPage";
import VerifyUserIdCodePage from "./pages/VerifyUserIdCodePage";
import ResultUserIdPage from "./pages/ResultUserIdPage";
import FindPasswordPage from "./pages/FindPasswordPage";
import VerifyCodePage from "./pages/VerifyCodePage";
import NewPasswordPage from "./pages/NewPasswordPage";
import ChangePasswordPage from "./pages/ChangePasswordPage";
import FirstSocialLoginPage from "./pages/FirstSocialLoginPage";

import PrivateRoute from "./components/auth/PrivateRoute";

// querystring 유지 리다이렉트
function RedirectWithQuery({ to }: { to: string }) {
    const loc = useLocation();
    return <Navigate to={`${to}${loc.search}${loc.hash || ""}`} replace />;
}

// (필요 시) 백엔드로 강제 점프: /User/First_Social_Login 같은 서버 로직이 필요한 경로용
function JumpToBackendSamePath() {
    const loc = useLocation();
    useEffect(() => {
        const backendPublic =
            (import.meta as any).env?.VITE_API_BASE_URL?.trim() || "http://localhost:8080";
        const base = backendPublic.replace(/\/+$/, "");
        window.location.replace(`${base}${loc.pathname}${loc.search}${loc.hash || ""}`);
    }, [loc.pathname, loc.search, loc.hash]);
    return null;
}

export default function App() {
    return (
        // ✅ 1) 이게 핵심: AuthProvider로 전체 감싸기
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* ✅ 2) 레거시 /User/* 별칭 라우트: 로그인/회원가입/휴대폰 인증 흐름 막히는 것 해결 */}
                    <Route path="/User/Register" element={<RegisterPage />} />
                    <Route path="/User/Find_Userid" element={<FindUserIdPage />} />
                    <Route path="/User/Find_Password" element={<FindPasswordPage />} />

                    {/* 아이디/비번 찾기 단계별 레거시 경로도 같이 커버 */}
                    <Route path="/User/Verify_Userid_Code" element={<VerifyUserIdCodePage />} />
                    <Route path="/User/Result_Userid" element={<ResultUserIdPage />} />
                    <Route path="/User/Verify_Code" element={<VerifyCodePage />} />
                    <Route path="/User/New_Password" element={<NewPasswordPage />} />
                    <Route path="/User/Change_Password" element={<ChangePasswordPage />} />

                    {/* 소셜 최초 로그인: 서버가 HttpOnly 쿠키로 query 만들어주는 흐름이면 백엔드로 점프가 안전 */}
                    <Route path="/User/First_Social_Login" element={<JumpToBackendSamePath />} />

                    {/* 기존 라우트들 */}
                    <Route path="/auth" element={<Auth />} />
                    <Route path="/Login" element={<LoginPage />} />
                    <Route path="/Register" element={<RegisterPage />} />

                    <Route path="/FindUserId" element={<FindUserIdPage />} />
                    <Route path="/VerifyUserIdCode" element={<VerifyUserIdCodePage />} />
                    <Route path="/ResultUserId" element={<ResultUserIdPage />} />

                    <Route path="/FindPassword" element={<FindPasswordPage />} />
                    <Route path="/VerifyCode" element={<VerifyCodePage />} />
                    <Route path="/NewPassword" element={<NewPasswordPage />} />
                    <Route path="/ChangePassword" element={<ChangePasswordPage />} />

                    <Route path="/FirstSocialLogin" element={<FirstSocialLoginPage />} />

                    <Route element={<AppLayout />}>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/jobs" element={<Jobs />} />
                        <Route path="/jobs/:jobId" element={<JobDetail />} />
                        <Route path="/talents" element={<Talents />} />
                        <Route path="/companies" element={<Companies />} />
                        <Route path="/companies/:companyId" element={<CompanyDetail />} />
                        <Route path="/community" element={<Community />} />
                        <Route path="/interview" element={<Interview />} />
                        <Route path="/support" element={<Support />} />

                        <Route
                            path="/resume"
                            element={
                                <PrivateRoute>
                                    <Resume />
                                </PrivateRoute>
                            }
                        />
                        <Route
                            path="/subscription"
                            element={
                                <PrivateRoute>
                                    <Subscription />
                                </PrivateRoute>
                            }
                        />
                        <Route
                            path="/mypage"
                            element={
                                <PrivateRoute>
                                    <MyPage />
                                </PrivateRoute>
                            }
                        />
                        <Route
                            path="/company/dashboard"
                            element={
                                <PrivateRoute requiredRole="EMPLOYER">
                                    <CompanyDashboard />
                                </PrivateRoute>
                            }
                        />
                        <Route
                            path="/jobseeker/mypage"
                            element={
                                <PrivateRoute requiredRole="JOB_SEEKER">
                                    <JobSeekerMyPage />
                                </PrivateRoute>
                            }
                        />
                        <Route
                            path="/admin"
                            element={
                                <PrivateRoute requiredRole="ADMIN">
                                    <AdminDashboard />
                                </PrivateRoute>
                            }
                        />
                    </Route>

                    <Route path="*" element={<NotFound />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}
