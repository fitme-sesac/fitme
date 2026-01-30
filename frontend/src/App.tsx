import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
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
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Index />} />
            <Route path="/auth" element={<Auth />} />
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
