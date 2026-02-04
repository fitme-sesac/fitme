import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { useEffect } from "react";
import { AuthProvider } from "@/contexts/AuthContext";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { ProfileSetupModal } from "@/components/auth/ProfileSetupModal";

// ============================================
// Common Pages (공통 기능)
// ============================================
import Index from "./pages/common/Index";
import Jobs from "./pages/common/Jobs";
import JobDetail from "./pages/common/JobDetail";
import CompanyDetail from "./pages/common/CompanyDetail";
import Community from "./pages/common/Community";
import Support from "./pages/common/Support";
import Settings from "./pages/common/Settings";
import Subscription from "./pages/common/Subscription";
import NotFound from "./pages/common/NotFound";

// ============================================
// Guest Pages (비회원/인증)
// ============================================
import Auth from "./pages/guest/Auth";
import FindUserIdPage from "./pages/guest/FindUserIdPage";
import VerifyUserIdCodePage from "./pages/guest/VerifyUserIdCodePage";
import ResultUserIdPage from "./pages/guest/ResultUserIdPage";
import FindPasswordPage from "./pages/guest/FindPasswordPage";
import VerifyCodePage from "./pages/guest/VerifyCodePage";
import NewPasswordPage from "./pages/guest/NewPasswordPage";
import ChangePasswordPage from "./pages/guest/ChangePasswordPage";
import FirstSocialLoginPage from "./pages/guest/FirstSocialLoginPage";
import JobSeekerSignup from "./pages/guest/JobSeekerSignup";

// ============================================
// JobSeeker Pages (구직자 전용)
// ============================================
import Interview from "./pages/jobseeker/Interview";
import Resume from "./pages/jobseeker/Resume";
import MyPage from "./pages/jobseeker/MyPage";
import JobSeekerMyPage from "./pages/jobseeker/JobSeekerMyPage";
import Proposals from "./pages/jobseeker/Proposals";
import Applications from "./pages/jobseeker/Applications";

// ============================================
// Company Pages (기업회원 전용)
// ============================================
import CompanyDashboard from "./pages/company/CompanyDashboard";
import CompanyManagement from "./pages/company/CompanyManagement";
import Talents from "./pages/company/Talents";
import TalentDetail from "./pages/company/TalentDetail";

// ============================================
// Employer Job Pages (채용공고 목록/상세/등록/수정 - /employer/jobs/*)
// ============================================
import JobListPage from "./features/job/pages/JobListPage";
import JobDetailPage from "./features/job/pages/JobDetailPage";
import JobCreatePage from "./features/job/pages/JobCreatePage";
import JobEditPage from "./features/job/pages/JobEditPage";

// ============================================
// Admin Pages (관리자 전용)
// ============================================
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminMembers from "./pages/admin/AdminMembers";
import AdminJobs from "./pages/admin/AdminJobs";
import AdminCompanies from "./pages/admin/AdminCompanies";
import AdminCommunity from "./pages/admin/AdminCommunity";
import AdminReports from "./pages/admin/AdminReports";
import AdminInquiries from "./pages/admin/AdminInquiries";
import AdminSubscription from "./pages/admin/AdminSubscription";

// ============================================
// Payment Pages (결제)
// ============================================
import PaymentSuccessPage from "./pages/payment/PaymentSuccessPage";
import PaymentFailPage from "./pages/payment/PaymentFailPage";
import SubscriptionCheckoutPage from "./pages/payment/SubscriptionCheckoutPage";
import SubscriptionSuccessPage from "./pages/payment/SubscriptionSuccessPage";

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
            <Sonner />
            <ProfileSetupModal />
            <BrowserRouter>
                <Routes>
                    {/* ✅ 레거시 로그인/회원가입 URL은 새 Auth(컴포넌트 기반)로 강제 유도 */}
                    <Route path="/Login" element={<RedirectToAuthTab tab="login" />} />
                    <Route path="/Register" element={<RedirectToAuthTab tab="signup" />} />
                    <Route path="/User/Register" element={<RedirectToAuthTab tab="signup" />} />

                    {/* ✅ 백엔드 redirectFront(..)가 사용하는 프론트 경로 호환 */}
                    <Route path="/FindUserId" element={<FindUserIdPage />} />
                    <Route path="/VerifyUserIdCode" element={<VerifyUserIdCodePage />} />
                    <Route path="/ResultUserId" element={<ResultUserIdPage />} />
                    <Route path="/FindPassword" element={<FindPasswordPage />} />
                    <Route path="/VerifyCode" element={<VerifyCodePage />} />
                    <Route path="/NewPassword" element={<NewPasswordPage />} />

                    {/* ✅ 레거시/호환 URL 유지 */}
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

                    {/* ✅ 홈 */}
                    <Route path="/" element={<Index />} />

                    {/* ✅ 나머지 페이지 */}
                    <Route path="/jobs" element={<Jobs />} />
                    <Route path="/jobs/:jobId" element={<JobDetail />} />
                    <Route path="/talents" element={<Talents />} />
                    <Route
                        path="/talents/:talentId"
                        element={
                            <PrivateRoute requiredRole="EMPLOYER">
                                <TalentDetail />
                            </PrivateRoute>
                        }
                    />
                    <Route path="/companies" element={<CompanyManagement />} />
                    <Route path="/companies/:companyId" element={<CompanyDetail />} />
                    <Route path="/community" element={<Community />} />
                    <Route path="/interview" element={<Interview />} />
                    <Route path="/support" element={<Support />} />

                    <Route path="/payment/success" element={<PaymentSuccessPage />} />
                    <Route path="/payment/fail" element={<PaymentFailPage />} />
                    <Route path="/signup/job-seeker" element={<JobSeekerSignup />} />
                    <Route path="/products" element={<Subscription />} />

                    <Route path="/subscription/success" element={<SubscriptionSuccessPage />} />
                    <Route path="/subscription/fail" element={<PaymentFailPage />} />

                    <Route path="/resume" element={<Resume />} />
                    <Route
                        path="/settings"
                        element={
                            <PrivateRoute>
                                <Settings />
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
                    <Route path="/mypage" element={<MyPage />} />
                    <Route
                        path="/company/dashboard"
                        element={
                            <PrivateRoute requiredRole="EMPLOYER">
                                <CompanyDashboard />
                            </PrivateRoute>
                        }
                    />
                    {/* 기업 채용공고: 목록/상세/등록/수정 (수정하기 링크와 브레드크럼 연동) */}
                    <Route path="/employer/dashboard" element={<Navigate to="/company/dashboard" replace />} />
                    <Route
                        path="/employer/jobs"
                        element={
                            <PrivateRoute requiredRole="EMPLOYER">
                                <JobListPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/employer/jobs/create"
                        element={
                            <PrivateRoute requiredRole="EMPLOYER">
                                <JobCreatePage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/employer/jobs/:jobId"
                        element={
                            <PrivateRoute requiredRole="EMPLOYER">
                                <JobDetailPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/employer/jobs/:jobId/edit"
                        element={
                            <PrivateRoute requiredRole="EMPLOYER">
                                <JobEditPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/jobseeker/mypage"
                        element={
                            <PrivateRoute requiredRole="CANDIDATE">
                                <JobSeekerMyPage />
                            </PrivateRoute>
                        }
                    />
                    {/* 구직자용: 받은 제안 / 입사지원 현황 */}
                    <Route
                        path="/proposals"
                        element={
                            <PrivateRoute requiredRole="CANDIDATE">
                                <Proposals />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/applications"
                        element={
                            <PrivateRoute requiredRole="CANDIDATE">
                                <Applications />
                            </PrivateRoute>
                        }
                    />
                    {/* ✅ Admin Routes (관리자 전용) */}
                    <Route
                        path="/admin"
                        element={
                            <PrivateRoute requiredRole="SERVICEADMIN">
                                <AdminDashboard />
                            </PrivateRoute>
                        }
                    />
                    <Route path="/admin/dashboard" element={<Navigate to="/admin" replace />} />
                    <Route
                        path="/admin/members"
                        element={
                            <PrivateRoute requiredRole="SERVICEADMIN">
                                <AdminMembers />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/admin/jobs"
                        element={
                            <PrivateRoute requiredRole="SERVICEADMIN">
                                <AdminJobs />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/admin/companies"
                        element={
                            <PrivateRoute requiredRole="SERVICEADMIN">
                                <AdminCompanies />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/admin/community"
                        element={
                            <PrivateRoute requiredRole="SERVICEADMIN">
                                <AdminCommunity />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/admin/reports"
                        element={
                            <PrivateRoute requiredRole="SERVICEADMIN">
                                <AdminReports />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/admin/inquiries"
                        element={
                            <PrivateRoute requiredRole="SERVICEADMIN">
                                <AdminInquiries />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/admin/subscriptions"
                        element={
                            <PrivateRoute requiredRole="SERVICEADMIN">
                                <AdminSubscription />
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
