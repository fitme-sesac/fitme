import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { useToast } from "@/hooks/use-toast";
import { Mail, Lock, User, Loader2, Briefcase, UserCircle } from "lucide-react";
import { SocialLoginButtons } from "./SocialLoginButtons";
import { z } from "zod";

const signUpSchema = z.object({
  displayName: z.string().min(2, "이름은 최소 2자 이상이어야 합니다").max(50, "이름은 50자 이하여야 합니다"),
  email: z.string().email("유효한 이메일 주소를 입력해주세요"),
  password: z.string().min(6, "비밀번호는 최소 6자 이상이어야 합니다"),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "비밀번호가 일치하지 않습니다",
  path: ["confirmPassword"],
});

export function SignUpForm() {
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [userType, setUserType] = useState("job_seeker");
  const [isLoading, setIsLoading] = useState(false);
  const { signUp } = useAuth();
  const { toast } = useToast();

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate input
    const result = signUpSchema.safeParse({ displayName, email, password, confirmPassword });
    if (!result.success) {
      toast({
        variant: "destructive",
        title: "입력 오류",
        description: result.error.errors[0].message,
      });
      return;
    }

    setIsLoading(true);
    
    const { error } = await signUp(email, password, displayName, userType);
    
    if (error) {
      let errorMessage = error.message;
      if (error.message.includes("already registered")) {
        errorMessage = "이미 등록된 이메일입니다";
      }
      toast({
        variant: "destructive",
        title: "회원가입 실패",
        description: errorMessage,
      });
    } else {
      toast({
        title: "회원가입 성공",
        description: "환영합니다! 로그인되었습니다.",
      });
    }
    
    setIsLoading(false);
  };

  return (
    <div className="space-y-6">
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* User Type Selection */}
        <div className="space-y-3">
          <Label>회원 유형</Label>
          <RadioGroup
            value={userType}
            onValueChange={(value) => setUserType(value)}
            className="grid grid-cols-2 gap-4"
          >
            <Label
              htmlFor="job_seeker"
              className={`flex flex-col items-center justify-center rounded-xl border-2 p-4 cursor-pointer transition-all ${
                userType === "job_seeker"
                  ? "border-primary bg-primary/5"
                  : "border-border hover:border-primary/50"
              }`}
            >
              <RadioGroupItem value="job_seeker" id="job_seeker" className="sr-only" />
              <UserCircle className={`h-8 w-8 mb-2 ${userType === "job_seeker" ? "text-primary" : "text-muted-foreground"}`} />
              <span className={`font-medium ${userType === "job_seeker" ? "text-primary" : "text-foreground"}`}>
                구직자
              </span>
              <span className="text-xs text-muted-foreground mt-1">개인 회원</span>
            </Label>
            
            <Label
              htmlFor="company"
              className={`flex flex-col items-center justify-center rounded-xl border-2 p-4 cursor-pointer transition-all ${
                userType === "company"
                  ? "border-primary bg-primary/5"
                  : "border-border hover:border-primary/50"
              }`}
            >
              <RadioGroupItem value="company" id="company" className="sr-only" />
              <Briefcase className={`h-8 w-8 mb-2 ${userType === "company" ? "text-primary" : "text-muted-foreground"}`} />
              <span className={`font-medium ${userType === "company" ? "text-primary" : "text-foreground"}`}>
                기업
              </span>
              <span className="text-xs text-muted-foreground mt-1">기업 회원</span>
            </Label>
          </RadioGroup>
        </div>

        <div className="space-y-2">
          <Label htmlFor="displayName">이름</Label>
          <div className="relative">
            <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="displayName"
              type="text"
              placeholder={userType === "company" ? "기업명을 입력하세요" : "이름을 입력하세요"}
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="pl-10"
              required
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="signupEmail">이메일</Label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="signupEmail"
              type="email"
              placeholder="example@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="pl-10"
              required
            />
          </div>
        </div>
        
        <div className="space-y-2">
          <Label htmlFor="signupPassword">비밀번호</Label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="signupPassword"
              type="password"
              placeholder="6자 이상 입력하세요"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="pl-10"
              required
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="confirmPassword">비밀번호 확인</Label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="confirmPassword"
              type="password"
              placeholder="비밀번호를 다시 입력하세요"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="pl-10"
              required
            />
          </div>
        </div>

        <Button type="submit" className="w-full btn-gradient-primary" disabled={isLoading}>
          {isLoading ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              가입 중...
            </>
          ) : (
            "회원가입"
          )}
        </Button>
      </form>

      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t border-border" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-card px-2 text-muted-foreground">또는</span>
        </div>
      </div>

      <SocialLoginButtons />
    </div>
  );
}
