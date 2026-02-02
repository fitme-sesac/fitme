import { useState } from "react";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Briefcase, UserCircle } from "lucide-react";
import { SocialLoginButtons } from "./SocialLoginButtons";
import { JobSeekerSignUpForm } from "./JobSeekerSignUpForm";
import { CompanySignUpForm } from "./CompanySignUpForm";

export function SignUpForm() {
  const [userType, setUserType] = useState<"job_seeker" | "company">("job_seeker");

  return (
    <div className="space-y-6">
      {/* User Type Selection */}
      <div className="space-y-3">
        <Label className="text-base font-semibold">회원 유형 선택</Label>
        <RadioGroup
          value={userType}
          onValueChange={(value: "job_seeker" | "company") => setUserType(value)}
          className="grid grid-cols-2 gap-4"
        >
          <Label
            htmlFor="job_seeker"
            className={`flex flex-col items-center justify-center rounded-xl border-2 p-4 cursor-pointer transition-all ${userType === "job_seeker"
              ? "border-primary bg-primary/5 shadow-sm"
              : "border-border hover:border-primary/50"
              }`}
          >
            <RadioGroupItem value="job_seeker" id="job_seeker" className="sr-only" />
            <UserCircle className={`h-8 w-8 mb-2 ${userType === "job_seeker" ? "text-primary" : "text-muted-foreground"}`} />
            <span className={`font-bold ${userType === "job_seeker" ? "text-primary" : "text-foreground"}`}>
              구직자
            </span>
            <span className="text-xs text-muted-foreground mt-1 text-center">개인 회원가입</span>
          </Label>

          <Label
            htmlFor="company"
            className={`flex flex-col items-center justify-center rounded-xl border-2 p-4 cursor-pointer transition-all ${userType === "company"
              ? "border-primary bg-primary/5 shadow-sm"
              : "border-border hover:border-primary/50"
              }`}
          >
            <RadioGroupItem value="company" id="company" className="sr-only" />
            <Briefcase className={`h-8 w-8 mb-2 ${userType === "company" ? "text-primary" : "text-muted-foreground"}`} />
            <span className={`font-bold ${userType === "company" ? "text-primary" : "text-foreground"}`}>
              기업
            </span>
            <span className="text-xs text-muted-foreground mt-1 text-center">기업 회원가입</span>
          </Label>
        </RadioGroup>
      </div>

      <div className="relative py-2">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t border-border" />
        </div>
      </div>

      {/* Render Selected Form */}
      {userType === "job_seeker" ? (
        <JobSeekerSignUpForm />
      ) : (
        <CompanySignUpForm />
      )}

      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t border-border" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-card px-2 text-muted-foreground">간편 가입</span>
        </div>
      </div>

      <SocialLoginButtons />
    </div>
  );
}
