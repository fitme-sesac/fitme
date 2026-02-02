import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { useToast } from "@/hooks/use-toast";
import { User, Lock, Loader2, UserCircle, Briefcase } from "lucide-react";
import { SocialLoginButtons } from "./SocialLoginButtons";
import { z } from "zod";

const loginSchema = z.object({
  loginId: z.string().min(3, "아이디는 최소 3자 이상이어야 합니다"),
  password: z.string().min(6, "비밀번호는 최소 6자 이상이어야 합니다"),
});

export function LoginForm() {
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const { signIn } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate input
    const result = loginSchema.safeParse({ loginId, password });
    if (!result.success) {
      toast({
        variant: "destructive",
        title: "입력 오류",
        description: result.error.errors[0].message,
      });
      return;
    }

    setIsLoading(true);

    const { error } = await signIn(loginId, password);

    if (error) {
      toast({
        variant: "destructive",
        title: "로그인 실패",
        description: typeof error === 'string' ? error : "아이디 또는 비밀번호가 올바르지 않습니다",
      });
    } else {
      toast({
        title: "로그인 성공",
        description: "환영합니다!",
      });
      navigate("/");
    }

    setIsLoading(false);
  };

  const [userType, setUserType] = useState<"job_seeker" | "company">("job_seeker");

  return (
    <div className="space-y-6">
      {/* User Type Selection - Visual Separation */}
      <div className="space-y-3">
        <Label className="text-base font-semibold">회원 유형 선택</Label>
        <RadioGroup
          value={userType}
          onValueChange={(value: "job_seeker" | "company") => setUserType(value)}
          className="grid grid-cols-2 gap-4"
        >
          <Label
            htmlFor="login_job_seeker"
            className={`flex flex-col items-center justify-center rounded-xl border-2 p-4 cursor-pointer transition-all ${userType === "job_seeker"
              ? "border-primary bg-primary/5 shadow-sm"
              : "border-border hover:border-primary/50"
              }`}
          >
            <RadioGroupItem value="job_seeker" id="login_job_seeker" className="sr-only" />
            <UserCircle className={`h-8 w-8 mb-2 ${userType === "job_seeker" ? "text-primary" : "text-muted-foreground"}`} />
            <span className={`font-bold ${userType === "job_seeker" ? "text-primary" : "text-foreground"}`}>
              구직자
            </span>
            <span className="text-xs text-muted-foreground mt-1 text-center">개인 회원 로그인</span>
          </Label>

          <Label
            htmlFor="login_company"
            className={`flex flex-col items-center justify-center rounded-xl border-2 p-4 cursor-pointer transition-all ${userType === "company"
              ? "border-primary bg-primary/5 shadow-sm"
              : "border-border hover:border-primary/50"
              }`}
          >
            <RadioGroupItem value="company" id="login_company" className="sr-only" />
            <Briefcase className={`h-8 w-8 mb-2 ${userType === "company" ? "text-primary" : "text-muted-foreground"}`} />
            <span className={`font-bold ${userType === "company" ? "text-primary" : "text-foreground"}`}>
              기업
            </span>
            <span className="text-xs text-muted-foreground mt-1 text-center">기업 회원 로그인</span>
          </Label>
        </RadioGroup>
      </div>

      <div className="relative py-2">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t border-border" />
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="loginId">아이디</Label>
          <div className="relative">
            <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="loginId"
              type="text"
              placeholder="아이디를 입력하세요"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              className="pl-10"
              required
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="password">비밀번호</Label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="password"
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="pl-10"
              required
            />
          </div>
        </div>

        <Button type="submit" className="w-full btn-gradient-primary" disabled={isLoading}>
          {isLoading ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              로그인 중...
            </>
          ) : (
            "로그인"
          )}
        </Button>
      </form>

      <div className="flex justify-center items-center gap-4 text-sm text-muted-foreground my-4">
        <span
          onClick={() => navigate("/auth/find-id")}
          className="cursor-pointer hover:text-primary hover:underline transition-all"
        >
          아이디 찾기
        </span>
        <span className="h-3 w-[1px] bg-border" />
        <span
          onClick={() => navigate("/auth/find-password")}
          className="cursor-pointer hover:text-primary hover:underline transition-all"
        >
          비밀번호 찾기
        </span>
        <span className="h-3 w-[1px] bg-border" />
        <span
          onClick={() => navigate("/auth?tab=signup")}
          className="cursor-pointer hover:text-primary hover:underline transition-all"
        >
          회원가입
        </span>
      </div>

      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t border-border" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-card px-2 text-muted-foreground">또는</span>
        </div>
      </div>

      <SocialLoginButtons />

      {/* Development Helper: Create Dummy User */}
      <div className="mt-8 pt-4 border-t border-dashed">
        <p className="text-xs text-center text-muted-foreground mb-2">개발용 더미 계정 도구</p>
        <Button
          variant="outline"
          className="w-full text-xs h-8 bg-slate-50"
          onClick={async () => {
            try {
              const dummyUser = {
                loginId: "candidate100",
                password: "Password123!",
                name: "김더미",
                email: "candidate100@test.com",
                phone: "01012345678",
                role: "CANDIDATE",
                marketingAgree: true,
                terms: { age: true, service: true, privacy: true }
              };
              const { register } = await import("@/api/auth");
              const res = await register(dummyUser);
              console.log("Dummy creation result:", res);

              if (res.error) {
                toast({ title: "생성 실패", description: res.error, variant: "destructive" });
              } else {
                toast({ title: "생성 성공", description: "ID: candidate100 / PW: Password123!" });
                setLoginId("candidate100");
                setPassword("Password123!");
              }
            } catch (e) {
              console.error(e);
              toast({ title: "오류 발생", description: "콘솔을 확인하세요", variant: "destructive" });
            }
          }}
        >
          candidate100 계정 생성/채우기
        </Button>
      </div>
    </div>
  );
}
