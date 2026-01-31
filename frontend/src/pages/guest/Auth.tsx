import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { Target, Sparkles, ArrowLeft } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { LoginForm } from "@/components/auth/LoginForm";
import { SignUpForm } from "@/components/auth/SignUpForm";

export default function Auth() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get("tab");
  const [activeTab, setActiveTab] = useState<"login" | "signup">(() =>
    tabParam === "signup" || tabParam === "register" ? "signup" : "login"
  );
  const { toast } = useToast();
  const [flashError, setFlashError] = useState<string | null>(() =>
    searchParams.get("errorMessage") || searchParams.get("error")
  );
  const [flashSuccess, setFlashSuccess] = useState<string | null>(() =>
    searchParams.get("message") || searchParams.get("successMessage") || searchParams.get("success")
  );
  const { user, loading, isCompany, isAdmin } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const t = searchParams.get("tab");
    if (t === "signup" || t === "register") setActiveTab("signup");

    const err = searchParams.get("errorMessage") || searchParams.get("error");
    const ok = searchParams.get("message") || searchParams.get("successMessage") || searchParams.get("success");
    setFlashError(err);
    setFlashSuccess(ok);
  }, [searchParams]);

  useEffect(() => {
    if (flashError) {
      toast({ variant: "destructive", title: "오류", description: flashError });
    } else if (flashSuccess) {
      toast({ title: "안내", description: flashSuccess });
    }
  }, [flashError, flashSuccess, toast]);

  useEffect(() => {
    // 이미 로그인되어 있다면 역할에 맞는 대시보드로 이동
    if (!loading && user) {
      if (isCompany) {
        navigate("/company/dashboard");
      } else if (isAdmin) {
        navigate("/admin");
      } else {
        navigate("/");
      }
    }
  }, [user, loading, navigate, isCompany, isAdmin]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex w-full bg-slate-50 overflow-hidden relative">

      {/* Background Texture Overlay - soft paper feel */}
      <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-[0.03] mix-blend-multiply pointer-events-none"></div>

      {/* 1. Left Panel: Brand Visual (Hidden on mobile) */}
      <div className="hidden lg:flex w-1/2 items-center justify-center relative overflow-hidden z-10">
        {/* Abstract Background Elements */}
        {/* Soft floating blobs matching the palette */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[#0EA5E9] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob" />
        <div className="absolute top-1/3 right-1/4 w-96 h-96 bg-[#2DD4BF] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-2000" />
        <div className="absolute bottom-1/4 left-1/3 w-96 h-96 bg-[#38BDF8] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-4000" />

        {/* Content */}
        <div className="relative z-10 flex flex-col items-center justify-center h-full p-12">
          <div className="relative group animate-slow-bounce">
            {/* Glowing Effect around Logo */}
            <div className="absolute inset-0 bg-white/60 rounded-full blur-[60px] opacity-60" />
            <Target className="w-[300px] h-[300px] text-slate-800 drop-shadow-2xl opacity-90" strokeWidth={0.5} />
            <Target className="absolute inset-0 w-[300px] h-[300px] text-sky-500/30 drop-shadow-xl mix-blend-overlay" strokeWidth={1} />
          </div>

          <div className="mt-12 text-center">
            <h2 className="text-4xl font-extrabold text-slate-900 mb-4 tracking-tight drop-shadow-sm">FitMe</h2>
            <p className="text-slate-600 text-lg font-medium max-w-sm mx-auto leading-relaxed">
              당신의 커리어를 위한<br />가장 스마트한 시작
            </p>
          </div>
        </div>
      </div>

      {/* 2. Right Panel: Scrollable Form Area */}
      {/* Seamless Transparent Background */}
      <div className="w-full lg:w-1/2 flex flex-col h-screen bg-transparent relative z-20">
        {/* Scrollable Container - Removed items-center to prevent top clipping on long forms */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden p-6 md:p-12">
          <div className="w-full mx-auto max-w-2xl transition-all duration-500 ease-in-out">

            {/* Mobile Heading */}
            <div className="flex lg:hidden flex-col items-center justify-center gap-4 mb-8 text-center">
              <div className="p-3 bg-white rounded-2xl border border-slate-200 shadow-lg">
                <Target className="h-8 w-8 text-sky-500" />
              </div>
              <h1 className="text-2xl font-bold text-slate-900 drop-shadow-sm">
                가장 스마트한 방법으로<br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-sky-500 to-teal-400 font-extrabold drop-shadow-sm">시작하세요.</span>
              </h1>
            </div>

            {/* Desktop Heading */}
            <div className="hidden lg:block mb-8">
              <h1 className="text-4xl font-extrabold text-slate-900 leading-tight mb-2 drop-shadow-sm">
                가장 스마트한 방법으로<br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-sky-500 to-teal-400 font-extrabold">시작하세요.</span>
              </h1>
            </div>

            {/* Tabs & Form Card */}
            {/* Background: White for card to match theme. */}
            <div className="bg-white/95 backdrop-blur-md rounded-2xl p-6 md:p-8 shadow-xl border border-slate-200">
              {(flashError || flashSuccess) && (
                <div
                  className={`mb-4 rounded-xl border p-3 text-sm ${flashError
                      ? "border-red-200 bg-red-50 text-red-800"
                      : "border-emerald-200 bg-emerald-50 text-emerald-800"
                    }`}
                  role="alert"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="leading-5">
                      {flashError ? flashError : flashSuccess}
                    </div>
                    <button
                      type="button"
                      className="shrink-0 rounded-md px-2 py-1 text-xs hover:bg-black/5"
                      onClick={() => {
                        const next = new URLSearchParams(searchParams.toString());
                        next.delete("errorMessage");
                        next.delete("error");
                        next.delete("message");
                        next.delete("successMessage");
                        next.delete("success");
                        setSearchParams(next, { replace: true });
                        setFlashError(null);
                        setFlashSuccess(null);
                      }}
                      aria-label="메시지 닫기"
                    >
                      닫기
                    </button>
                  </div>
                </div>
              )}
              <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as "login" | "signup")}>
                <TabsList className="grid w-full grid-cols-2 mb-8 bg-slate-100 p-1">
                  <TabsTrigger
                    value="login"
                    className="text-sm font-medium data-[state=active]:bg-gradient-to-r data-[state=active]:from-sky-500 data-[state=active]:to-teal-400 data-[state=active]:text-white data-[state=active]:shadow-sm text-slate-500"
                  >
                    로그인
                  </TabsTrigger>
                  <TabsTrigger
                    value="signup"
                    className="text-sm font-medium data-[state=active]:bg-gradient-to-r data-[state=active]:from-sky-500 data-[state=active]:to-teal-400 data-[state=active]:text-white data-[state=active]:shadow-sm text-slate-500"
                  >
                    회원가입
                  </TabsTrigger>
                </TabsList>

                <TabsContent value="login" className="mt-0 focus-visible:ring-0 outline-none animate-in fade-in slide-in-from-right-4 duration-300">
                  <LoginForm />
                </TabsContent>

                <TabsContent value="signup" className="mt-0 focus-visible:ring-0 outline-none">
                  <SignUpForm />
                </TabsContent>
              </Tabs>
            </div>

            {/* Footer Links */}
            <div className="mt-8 text-center space-y-2">
              <p className="text-xs text-slate-500 font-medium">
                계정 생성 시 FitMe의 <a href="#" className="underline hover:text-sky-500 transition-colors">이용약관</a> 및 <a href="#" className="underline hover:text-sky-500 transition-colors">개인정보처리방침</a>에 동의하게 됩니다.
              </p>
            </div>
          </div>
        </div>
      </div>
      <div className="absolute bottom-8 left-8 z-50">
        <button
          onClick={() => navigate("/")}
          className="flex items-center gap-2 text-slate-500 hover:text-slate-900 transition-colors font-medium text-sm bg-white/50 backdrop-blur-sm px-4 py-2 rounded-full border border-slate-200/50 hover:bg-white/80"
        >
          <ArrowLeft className="w-4 h-4" />
          홈으로 돌아가기
        </button>
      </div>
    </div>
  );
}
