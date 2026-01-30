import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { ProfileSetupModal } from "@/components/auth/ProfileSetupModal";
import PrivateRoute from "./components/auth/PrivateRoute";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import FirstSocialLoginPage from "./pages/FirstSocialLoginPage";
import FindUserIdPage from "./pages/FindUserIdPage";
import VerifyUserIdCodePage from "./pages/VerifyUserIdCodePage";
import ResultUserIdPage from "./pages/ResultUserIdPage";
import FindPasswordPage from "./pages/FindPasswordPage";
import VerifyCodePage from "./pages/VerifyCodePage";
import NewPasswordPage from "./pages/NewPasswordPage";
import ChangePasswordPage from "./pages/ChangePasswordPage";
import JobSeekerSignup from "./pages/JobSeekerSignup";
import PaymentSuccessPage from "./pages/payment/PaymentSuccessPage";
import PaymentFailPage from "./pages/payment/PaymentFailPage";

// Suspects commented out for isolation
import Index from "./pages/Index";
import Auth from "./pages/Auth";
import NotFound from "./pages/NotFound";
import Jobs from "./pages/Jobs";
import JobDetail from "./pages/JobDetail";
import CompanyDetail from "./pages/CompanyDetail";
import MyPage from "./pages/MyPage";
import Community from "./pages/Community";
import Support from "./pages/Support";
import Resume from "./pages/Resume";
import Interview from "./pages/Interview";
import Settings from "./pages/Settings";
import Talents from "./pages/Talents";
import Companies from "./pages/Companies";
import Subscription from "./pages/Subscription";
import CompanyDashboard from "./pages/CompanyDashboard";
// import AdminDashboard from "./pages/AdminDashboard";

/** /Login → /auth 로 이동 (백엔드 리다이렉트 시 쿼리 유지) */
function LoginRedirect() {
  const { search } = useLocation();
  return <Navigate to={"/auth" + (search || "")} replace />;
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const App = () => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <TooltipProvider>
        <Toaster />
        <Sonner />
        <ProfileSetupModal />
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Index />} />
            <Route path="/auth" element={<Auth />} />
            {/* 백엔드 소셜 로그인 리다이렉트: /login-redirect → /auth (쿼리 유지) */}
            <Route path="/login-redirect" element={<LoginRedirect />} />
            <Route path="/Login" element={<LoginPage />} />
            <Route path="/Register" element={<RegisterPage />} />
            <Route path="/FirstSocialLogin" element={<FirstSocialLoginPage />} />

            <Route path="/FindUserId" element={<FindUserIdPage />} />
            <Route path="/VerifyUserIdCode" element={<VerifyUserIdCodePage />} />
            <Route path="/ResultUserId" element={<ResultUserIdPage />} />

            <Route path="/FindPassword" element={<FindPasswordPage />} />
            <Route path="/VerifyCode" element={<VerifyCodePage />} />
            <Route path="/NewPassword" element={<NewPasswordPage />} />

            <Route path="/ChangePassword" element={<ChangePasswordPage />} />

            {/* Find ID/Password (auth 하위 경로) */}
            <Route path="/auth/find-id" element={<FindUserIdPage />} />
            <Route path="/auth/find-password" element={<FindPasswordPage />} />
            {/* 구글/카카오 등 소셜 미가입 시 회원가입(추가정보) */}
            <Route path="/signup/job-seeker" element={<JobSeekerSignup />} />

            {/* Protected Job Routes */}
            <Route path="/jobs" element={<PrivateRoute><Jobs /></PrivateRoute>} />
            <Route path="/jobs/:jobId" element={<PrivateRoute><JobDetail /></PrivateRoute>} />
            <Route path="/companies/:employerId" element={<CompanyDetail />} />

            <Route path="/community" element={<Community />} />
            <Route path="/support" element={<Support />} />

            {/* Payment Result Pages */}
            <Route path="/payment/success" element={<PrivateRoute><PaymentSuccessPage /></PrivateRoute>} />
            <Route path="/payment/fail" element={<PrivateRoute><PaymentFailPage /></PrivateRoute>} />

            {/* Protected Routes - Pages handle their own auth state */}
            <Route path="/mypage" element={<MyPage />} />
            <Route path="/resume" element={<Resume />} />
            <Route path="/interview" element={<Interview />} />
            <Route path="/settings" element={<PrivateRoute><Settings /></PrivateRoute>} />

            {/* Company Routes */}
            <Route path="/talents" element={<Talents />} />
            <Route path="/companies" element={<Companies />} />
            <Route path="/payment/products" element={<Subscription />} />
            <Route path="/company/dashboard" element={<CompanyDashboard />} />

            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </TooltipProvider>
    </AuthProvider>
  </QueryClientProvider>
);

export default App;
