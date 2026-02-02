import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { Loader2, ArrowLeft, CheckCircle2, Eye, EyeOff } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { findPassword, verifyPasswordCode, setNewPassword } from "@/api/auth";

// Schema for Step 1: Verify User
const verifySchema = z.object({
  loginId: z.string().min(3, "아이디를 입력해주세요."),
  phone: z.string().regex(/^01[0-9]{8,9}$/, "유효한 휴대폰 번호를 입력해주세요."),
  verificationCode: z.string().min(1, "인증번호를 입력해주세요."),
});

// Schema for Step 2: Reset Password
const resetSchema = z.object({
  password: z.string()
    .min(8, "비밀번호는 8자 이상이어야 합니다.")
    .regex(/^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])/, "영문, 숫자, 특수문자를 포함해야 합니다."),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "비밀번호가 일치하지 않습니다.",
  path: ["confirmPassword"],
});

export default function FindPasswordPage() {
  const { toast } = useToast();
  const navigate = useNavigate();

  const [step, setStep] = useState("verify");

  // Verify State
  const [verificationSent, setVerificationSent] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [timer, setTimer] = useState(0);
  const [resetToken, setResetToken] = useState(null);

  // Reset State
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Forms
  const verifyForm = useForm({
    resolver: zodResolver(verifySchema),
    defaultValues: {
      loginId: "",
      phone: "",
      verificationCode: "",
    },
  });

  const resetForm = useForm({
    resolver: zodResolver(resetSchema),
    defaultValues: {
      password: "",
      confirmPassword: "",
    },
  });

  useEffect(() => {
    let interval;
    if (verificationSent && timer > 0 && !resetToken) {
      interval = setInterval(() => {
        setTimer((prev) => prev - 1);
      }, 1000);
    } else if (timer === 0) {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [verificationSent, timer, resetToken]);

  const handleSendAuth = async () => {
    const loginId = verifyForm.getValues("loginId");
    const phone = verifyForm.getValues("phone");

    if (!loginId) { verifyForm.setError("loginId", { message: "아이디를 입력해주세요" }); return; }
    if (!phone || !/^01[0-9]{8,9}$/.test(phone)) { verifyForm.setError("phone", { message: "올바른 휴대폰 번호를 입력해주세요" }); return; }

    try {
      await findPassword(loginId, phone);
      setVerificationSent(true);
      setTimer(300);
      toast({ title: "인증번호 발송", description: "입력하신 번호로 인증번호를 보냈습니다." });
    } catch (e) {
      toast({ title: "발송 실패", description: e.message || "일치하는 회원 정보가 없습니다.", variant: "destructive" });
    }
  };

  const handleConfirmAuth = async () => {
    const phone = verifyForm.getValues("phone");
    const code = verifyForm.getValues("verificationCode");
    if (!code) {
      verifyForm.setError("verificationCode", { message: "인증번호를 입력해주세요" });
      return;
    }

    setIsVerifying(true);
    try {
      // verifyPasswordCode returns { token: string }
      const res = await verifyPasswordCode(phone, code);
      if (res && res.token) {
        setResetToken(res.token);
        setStep("reset");
        toast({ title: "인증 성공", description: "비밀번호를 재설정해주세요." });
      } else {
        toast({ title: "인증 실패", description: "인증번호가 일치하지 않습니다.", variant: "destructive" });
      }
    } catch (e) {
      toast({ title: "오류", description: e.message || "인증 확인 중 오류가 발생했습니다.", variant: "destructive" });
    } finally {
      setIsVerifying(false);
    }
  };

  const handleResetPassword = async (data) => {
    if (!resetToken) return;
    setIsSubmitting(true);
    try {
      await setNewPassword(resetToken, data.password);
      toast({ title: "변경 완료", description: "비밀번호가 변경되었습니다. 로그인해주세요." });
      // Optional: Show success UI before redirect? Or just redirect.
      // Let's redirect after a short delay or show a success state.
      navigate("/auth?tab=login");
    } catch (e) {
      toast({ title: "변경 실패", description: e.message || "비밀번호 변경 중 오류가 발생했습니다.", variant: "destructive" });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex w-full bg-slate-50 overflow-hidden relative">
      {/* Background Texture */}
      <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-[0.03] mix-blend-multiply pointer-events-none"></div>

      {/* 1. Left Panel: Brand Visual */}
      <div className="hidden lg:flex w-1/2 items-center justify-center relative overflow-hidden z-10">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[#0EA5E9] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob" />
        <div className="absolute top-1/3 right-1/4 w-96 h-96 bg-[#2DD4BF] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-2000" />
        <div className="absolute bottom-1/4 left-1/3 w-96 h-96 bg-[#38BDF8] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-4000" />

        <div className="relative z-10 flex flex-col items-center justify-center h-full p-12">
          <div className="mt-12 text-center">
            <h2 className="text-4xl font-extrabold text-slate-900 mb-4 tracking-tight drop-shadow-sm">FitMe</h2>
            <p className="text-slate-600 text-lg font-medium max-w-sm mx-auto leading-relaxed">
              비밀번호를 잊으셨나요?<br />새로운 비밀번호를 설정하세요.
            </p>
          </div>
        </div>
      </div>

      {/* 2. Right Panel: Form Area */}
      <div className="w-full lg:w-1/2 flex flex-col h-screen bg-transparent relative z-20">
        <div className="flex-1 overflow-y-auto overflow-x-hidden p-6 md:p-12 flex items-center">
          <div className="w-full mx-auto max-w-md transition-all duration-500 ease-in-out">

            <Card className="bg-white/95 backdrop-blur-md rounded-2xl shadow-xl border border-slate-200">
              <CardHeader className="space-y-1 pb-6">
                <div className="flex items-center gap-2 mb-2">
                  <Button variant="ghost" size="icon" onClick={() => navigate("/auth?tab=login")} className="-ml-3 h-8 w-8 rounded-full">
                    <ArrowLeft className="h-4 w-4" />
                  </Button>
                  <CardTitle className="text-xl font-bold">비밀번호 찾기</CardTitle>
                </div>
                <CardDescription>
                  {step === "verify" ? "가입된 아이디와 휴대폰 번호로 본인을 인증해주세요." : "새로운 비밀번호를 설정해주세요."}
                </CardDescription>
              </CardHeader>
              <CardContent className="pt-0">
                {step === "verify" ? (
                  <Form {...verifyForm}>
                    <form className="space-y-4" onSubmit={(e) => e.preventDefault()}>
                      <FormField
                        control={verifyForm.control}
                        name="loginId"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>아이디</FormLabel>
                            <FormControl>
                              <Input placeholder="아이디 입력" {...field} disabled={verificationSent} className="bg-white" />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <div className="space-y-2">
                        <Label>휴대폰 번호</Label>
                        <div className="flex gap-2">
                          <FormField
                            control={verifyForm.control}
                            name="phone"
                            render={({ field }) => (
                              <FormItem className="flex-1 space-y-0">
                                <FormControl>
                                  <Input placeholder="01012345678" {...field} disabled={verificationSent} className="bg-white" />
                                </FormControl>
                                <FormMessage />
                              </FormItem>
                            )}
                          />
                          <Button
                            type="button"
                            variant="outline"
                            onClick={handleSendAuth}
                            disabled={verificationSent}
                            className="w-24 shrink-0 bg-white"
                          >
                            {verificationSent ? "발송됨" : "인증 요청"}
                          </Button>
                        </div>
                      </div>

                      {verificationSent && (
                        <div className="space-y-2 animate-in slide-in-from-top-2">
                          <div className="flex gap-2 relative">
                            <FormField
                              control={verifyForm.control}
                              name="verificationCode"
                              render={({ field }) => (
                                <FormItem className="flex-1 space-y-0 relative">
                                  <FormControl>
                                    <Input placeholder="인증번호 6자리" {...field} className="bg-white" />
                                  </FormControl>
                                  <span className="absolute right-3 top-2.5 text-sm text-destructive font-medium">
                                    {Math.floor(timer / 60)}:{String(timer % 60).padStart(2, '0')}
                                  </span>
                                  <FormMessage />
                                </FormItem>
                              )}
                            />
                            <Button onClick={handleConfirmAuth} disabled={isVerifying} className="w-24 shrink-0 btn-gradient-primary">
                              {isVerifying ? <Loader2 className="h-4 w-4 animate-spin" /> : "확인"}
                            </Button>
                          </div>
                        </div>
                      )}

                    </form>
                  </Form>
                ) : (
                  <Form {...resetForm}>
                    <form onSubmit={resetForm.handleSubmit(handleResetPassword)} className="space-y-4 animate-in slide-in-from-right duration-500">
                      <FormField
                        control={resetForm.control}
                        name="password"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>새 비밀번호</FormLabel>
                            <div className="relative">
                              <FormControl>
                                <Input type={showPassword ? "text" : "password"} placeholder="8자 이상 (영문, 숫자, 특수문자)" {...field} className="bg-white" />
                              </FormControl>
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                                onClick={() => setShowPassword(!showPassword)}
                              >
                                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                              </Button>
                            </div>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={resetForm.control}
                        name="confirmPassword"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>비밀번호 확인</FormLabel>
                            <div className="relative">
                              <FormControl>
                                <Input type={showConfirmPassword ? "text" : "password"} placeholder="비밀번호 재입력" {...field} className="bg-white" />
                              </FormControl>
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                              >
                                {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                              </Button>
                            </div>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <Button type="submit" className="w-full btn-gradient-primary h-12 text-lg mt-4 shadow-md" disabled={isSubmitting}>
                        {isSubmitting ? (
                          <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            변경 중...
                          </>
                        ) : "비밀번호 변경 완료"}
                      </Button>
                    </form>
                  </Form>
                )}
              </CardContent>
            </Card>

            <div className="mt-8 text-center space-y-2">
              <p className="text-xs text-slate-500 font-medium">
                문제가 지속되면 <a href="#" className="underline hover:text-sky-500 transition-colors">고객센터</a>로 문의해주세요.
              </p>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}
