// App.tsx
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { useEffect } from "react";
import { AuthProvider } from "@/contexts/AuthContext";

import Index from "./pages/Index";
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
import Support from "./pages/Support";
import NotFound from "./pages/NotFound";

import Auth from "./pages/Auth";
import FindUserIdPage from "./pages/FindUserIdPage";
import VerifyUserIdCodePage from "./pages/VerifyUserIdCodePage";
import ResultUserIdPage from "./pages/ResultUserIdPage";
import FindPasswordPage from "./pages/FindPasswordPage";
import VerifyCodePage from "./pages/VerifyCodePage";
import NewPasswordPage from "./pages/NewPasswordPage";
import ChangePasswordPage from "./pages/ChangePasswordPage";
import FirstSocialLoginPage from "./pages/FirstSocialLoginPage";

import PrivateRoute from "./components/auth/PrivateRoute";

function RedirectToAuthTab({ tab }: { tab: "login" | "signup" }) {
    const loc = useLocation();
    const params = new URLSearchParams(loc.search);
    params.set("tab", tab);
    return <Navigate to={`/auth?${params.toString()}`} replace />;
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
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* ✅ 레거시 로그인/회원가입 URL은 새 Auth(컴포넌트 기반)로 강제 유도 */}
                    <Route path="/Login" element={<RedirectToAuthTab tab="login" />} />
                    <Route path="/Register" element={<RedirectToAuthTab tab="signup" />} />
                    <Route path="/User/Register" element={<RedirectToAuthTab tab="signup" />} />

                    {/* ✅ 레거시/호환 URL 유지 (필요한 기능 페이지는 그대로 유지) */}
                    <Route path="/User/Find_Userid" element={<FindUserIdPage />} />
                    <Route path="/User/Find_Password" element={<FindPasswordPage />} />
                    <Route path="/User/Verify_Userid_Code" element={<VerifyUserIdCodePage />} />
                    <Route path="/User/Result_Userid" element={<ResultUserIdPage />} />
                    <Route path="/User/Verify_Code" element={<VerifyCodePage />} />
                    <Route path="/User/New_Password" element={<NewPasswordPage />} />
                    <Route path="/User/Change_Password" element={<ChangePasswordPage />} />
                    <Route path="/User/First_Social_Login" element={<JumpToBackendSamePath />} />

                    {/* ✅ 새 Auth 페이지 */}
                    <Route path="/auth" element={<Auth />} />

                    {/* ✅ 새 Auth 하위 경로(로그인 폼의 링크와 일치) */}
                    <Route path="/auth/find-id" element={<FindUserIdPage />} />
                    <Route path="/auth/find-id/result" element={<ResultUserIdPage />} />
                    <Route path="/auth/find-password" element={<FindPasswordPage />} />

                    {/* ✅ 홈: 레거시 HomePage 제거 -> 컴포넌트 기반 Index로 교체 */}
                    <Route path="/" element={<Index />} />

                    {/* ✅ 나머지: 각 페이지가 이미 Sidebar/Header/Footer(components/layout)를 사용 */}
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

                    <Route path="/FirstSocialLogin" element={<FirstSocialLoginPage />} />

                    <Route path="*" element={<NotFound />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}
