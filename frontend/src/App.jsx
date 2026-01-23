import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import { useEffect } from "react";
import AppLayout from "./layout/AppLayout";

import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import FindUserIdPage from "./pages/FindUserIdPage";
import ResultUserIdPage from "./pages/ResultUserIdPage";
import VerifyUserIdCodePage from "./pages/VerifyUserIdCodePage";
import FindPasswordPage from "./pages/FindPasswordPage";
import VerifyCodePage from "./pages/VerifyCodePage";
import NewPasswordPage from "./pages/NewPasswordPage";
import ChangePasswordPage from "./pages/ChangePasswordPage";
import FirstSocialLoginPage from "./pages/FirstSocialLoginPage";
import ReEnterCredentialsPage from "./pages/ReEnterCredentialsPage";

// 기업(Employer) 관련 페이지
import EmployerDashboardPage from "./features/employer/pages/EmployerDashboardPage";
import EmployerProfilePage from "./features/employer/pages/EmployerProfilePage";

// 채용공고(Job) 관련 페이지
import JobListPage from "./features/job/pages/JobListPage";
import JobCreatePage from "./features/job/pages/JobCreatePage";
import JobEditPage from "./features/job/pages/JobEditPage";
import JobDetailPage from "./features/job/pages/JobDetailPage";

// 공개 채용공고 페이지 (일반 사용자용)
import PublicJobListPage from "./features/job/pages/PublicJobListPage";
import PublicJobDetailPage from "./features/job/pages/PublicJobDetailPage";

// 알림(Notification) 페이지
import NotificationPage from "./features/notification/pages/NotificationPage";

/**
 * React 라우트에 없는 경로는 기존 백엔드(8080)가 처리하도록 위임.
 * (React로 안 옮긴 기존 페이지/기능을 깨지지 않게 유지)
 */
function BackendFallback() {
  const loc = useLocation();

  useEffect(() => {
    const backendBase = import.meta.env.VITE_BACKEND_URL || "http://localhost:8080";
    const url = backendBase + loc.pathname + loc.search + loc.hash;
    window.location.replace(url);
  }, [loc]);

  return <div className="container py-5">Redirecting...</div>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />

          {/* ===== Legacy-friendly paths (match original Thymeleaf URLs) ===== */}
          <Route path="/Login" element={<LoginPage />} />
          <Route path="/Register" element={<RegisterPage />} />
          <Route path="/Find_Userid" element={<FindUserIdPage />} />
          <Route path="/Verify_Userid_Code" element={<VerifyUserIdCodePage />} />
          <Route path="/Result_Userid" element={<ResultUserIdPage />} />

          <Route path="/Find_password" element={<FindPasswordPage />} />
          <Route path="/Verify_Code" element={<VerifyCodePage />} />
          <Route path="/New_Password" element={<NewPasswordPage />} />
          <Route path="/Change_Password" element={<ChangePasswordPage />} />
          <Route path="/First_Social_Login" element={<FirstSocialLoginPage />} />
          <Route path="/Re_Enter_Credentials" element={<ReEnterCredentialsPage />} />

          {/* ===== camelCase aliases (redirectFrontWithQuery / SPA direct links) ===== */}
          <Route path="/FindUserId" element={<FindUserIdPage />} />
          <Route path="/VerifyUserIdCode" element={<VerifyUserIdCodePage />} />
          <Route path="/ResultUserId" element={<ResultUserIdPage />} />

          <Route path="/FindPassword" element={<FindPasswordPage />} />
          <Route path="/VerifyCode" element={<VerifyCodePage />} />
          <Route path="/NewPassword" element={<NewPasswordPage />} />
          <Route path="/ChangePassword" element={<ChangePasswordPage />} />
          <Route path="/FirstSocialLogin" element={<FirstSocialLoginPage />} />
          <Route path="/ReEnterCredentials" element={<ReEnterCredentialsPage />} />

          {/* ===== /User/... legacy links (header/login template compatibility) ===== */}
          <Route path="/User/Login" element={<LoginPage />} />
          <Route path="/User/Register" element={<RegisterPage />} />
          <Route path="/User/Find_Userid" element={<FindUserIdPage />} />
          <Route path="/User/Verify_Userid_Code" element={<VerifyUserIdCodePage />} />
          <Route path="/User/Result_Userid" element={<ResultUserIdPage />} />

          <Route path="/User/Find_Password" element={<FindPasswordPage />} />
          <Route path="/User/Verify_Code" element={<VerifyCodePage />} />
          <Route path="/User/New_Password" element={<NewPasswordPage />} />
          <Route path="/User/Change_Password" element={<ChangePasswordPage />} />
          <Route path="/User/First_Social_Login" element={<FirstSocialLoginPage />} />
          <Route path="/User/Re_Enter_Credentials" element={<ReEnterCredentialsPage />} />

          {/* camelCase under /User */}
          <Route path="/User/FindUserId" element={<FindUserIdPage />} />
          <Route path="/User/VerifyUserIdCode" element={<VerifyUserIdCodePage />} />
          <Route path="/User/ResultUserId" element={<ResultUserIdPage />} />
          <Route path="/User/FindPassword" element={<FindPasswordPage />} />
          <Route path="/User/VerifyCode" element={<VerifyCodePage />} />
          <Route path="/User/NewPassword" element={<NewPasswordPage />} />
          <Route path="/User/ChangePassword" element={<ChangePasswordPage />} />
          <Route path="/User/FirstSocialLogin" element={<FirstSocialLoginPage />} />
          <Route path="/User/ReEnterCredentials" element={<ReEnterCredentialsPage />} />

          {/* 백엔드에서 아직 처리하는 화면/기능 경로들 (React 페이지가 없으므로 백엔드로 위임) */}
          <Route path="/MyPage" element={<BackendFallback />} />
          <Route path="/User/MyPage" element={<BackendFallback />} />
          <Route path="/User/Update" element={<BackendFallback />} />
          <Route path="/user/update" element={<BackendFallback />} />

          {/* 소문자 alias (선택) */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* ===== 공개 채용공고 (일반 사용자용) ===== */}
          <Route path="/jobs" element={<PublicJobListPage />} />
          <Route path="/jobs/:jobId" element={<PublicJobDetailPage />} />

          {/* ===== 기업(Employer) 관련 라우트 ===== */}
          <Route path="/employer/dashboard" element={<EmployerDashboardPage />} />
          <Route path="/employer/profile" element={<EmployerProfilePage />} />
          <Route path="/employer" element={<EmployerDashboardPage />} />
          
          {/* ===== 채용공고(Job) 관련 라우트 (기업회원용) ===== */}
          <Route path="/employer/jobs" element={<JobListPage />} />
          <Route path="/employer/jobs/create" element={<JobCreatePage />} />
          <Route path="/employer/jobs/:jobId" element={<JobDetailPage />} />
          <Route path="/employer/jobs/:jobId/edit" element={<JobEditPage />} />

          {/* ===== 알림(Notification) 라우트 ===== */}
          <Route path="/notifications" element={<NotificationPage />} />

          {/* 나머지 경로는 백엔드가 처리 */}
          <Route path="*" element={<BackendFallback />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
