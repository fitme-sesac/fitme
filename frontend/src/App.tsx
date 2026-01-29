import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { ProfileSetupModal } from "@/components/auth/ProfileSetupModal";

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
import PrivateRoute from "./components/auth/PrivateRoute";
import Talents from "./pages/Talents";
import Companies from "./pages/Companies";
import Subscription from "./pages/Subscription";
import CompanyDashboard from "./pages/CompanyDashboard";
// import AdminDashboard from "./pages/AdminDashboard";
import JobSeekerSignup from "./pages/JobSeekerSignup";
import FirstSocialLoginPage from "./pages/FirstSocialLoginPage";

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
            {/* 백엔드 소셜 로그인 리다이렉트: /Login → 로그인 화면 (쿼리 유지) */}
            <Route path="/Login" element={<LoginRedirect />} />
            {/* 구글/카카오 등 소셜 미가입 시 백엔드가 리다이렉트하는 회원가입(추가정보) 페이지 */}
            <Route path="/FirstSocialLogin" element={<FirstSocialLoginPage />} />
            <Route path="/signup/job-seeker" element={<JobSeekerSignup />} />

            <Route path="/jobs" element={<Jobs />} />
            <Route path="/jobs/:jobId" element={<JobDetail />} />
            <Route path="/companies/:employerId" element={<CompanyDetail />} />

            <Route path="/community" element={<Community />} />
            <Route path="/support" element={<Support />} />

            {/* Protected Routes - Pages handle their own auth state */}
            <Route path="/mypage" element={<MyPage />} />
            <Route path="/resume" element={<Resume />} />
            <Route path="/interview" element={<Interview />} />
            <Route path="/settings" element={<Settings />} />

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
