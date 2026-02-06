import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { Loader2, ArrowLeft, CheckCircle2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { findPassword, verifyPasswordCode } from "@/api/auth";

// Step 1: Verify User (이름 + 휴대폰 + OTP)
const verifySchema = z.object({
  username: z.string().min(1, "이름을 입력해주세요."),
  phone: z.string().regex(/^01[0-9]{8,9}$/, "유효한 휴대폰 번호를 입력해주세요."),
  verificationCode: z.string().min(1, "인증번호를 입력해주세요."),
});

export default function FindPasswordPage() {
  const { toast } = useToast();
  const navigate = useNavigate();

  const [step, setStep] = useState("verify"); // verify | done
  const [verificationSent, setVerificationSent] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [timer, setTimer] = useState(0);
  const [emailMasked, setEmailMasked] = useState("");
  const [verificationId, setVerificationId] = useState("");

  const verifyForm = useForm({
    resolver: zodResolver(verifySchema),
    defaultValues: {
      username: "",
      phone: "",
      verificationCode: "",
    },
  });

  useEffect(() => {
    let interval;
    if (verificationSent && timer > 0) {
      interval = setInterval(() => setTimer((prev) => prev - 1), 1000);
    } else if (timer === 0) {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [verificationSent, timer]);

  const resetAll = () => {
    setStep("verify");
    setVerificationSent(false);
    setIsSending(false);
    setIsVerifying(false);
    setTimer(0);
    setEmailMasked("");
    setVerificationId("");
    verifyForm.reset();
  };

  const handleSendAuth = async () => {
    const username = verifyForm.getValues("username");
    const phone = verifyForm.getValues("phone");

    if (!username) {
      verifyForm.setError("username", { message: "이름을 입력해주세요." });
      return;
    }
    if (!phone || !/^01[0-9]{8,9}$/.test(phone)) {
      verifyForm.setError("phone", { message: "올바른 휴대폰 번호를 입력해주세요." });
      return;
    }

    setIsSending(true);
    try {
      const res = await findPassword(username, phone);
      setVerificationId(String(res?.verificationId || ""));
      setVerificationSent(true);

      // backend가 expiresIn(초) 제공 시 사용, 없으면 300초
      const expires = Number(res?.expiresIn || 300);
      setTimer(expires);

      toast({ title: "인증번호 발송", description: "입력하신 번호로 인증번호를 보냈습니다." });
    } catch (e) {
      toast({
        title: "발송 실패",
        description: e?.message || "일치하는 회원 정보가 없습니다.",
        variant: "destructive",
      });
    } finally {
      setIsSending(false);
    }
  };

  const handleConfirmAuth = async () => {
    const username = verifyForm.getValues("username");
    const phone = verifyForm.getValues("phone");
    const code = verifyForm.getValues("verificationCode");

    if (!verificationSent) {
      toast({ title: "확인 필요", description: "먼저 인증번호를 발송해주세요.", variant: "destructive" });
      return;
    }
    if (!verificationId) {
      toast({ title: "확인 필요", description: "인증번호를 다시 발송해주세요.", variant: "destructive" });
      return;
    }
    if (!code) {
      verifyForm.setError("verificationCode", { message: "인증번호를 입력해주세요." });
      return;
    }

    setIsVerifying(true);
    try {
      const res = await verifyPasswordCode(username, phone, code, verificationId);
      setEmailMasked(res?.emailMasked || "");
      setStep("done");

      toast({ title: "인증 성공", description: "이메일로 비밀번호 변경 링크를 보냈습니다." });
    } catch (e) {
      toast({
        title: "인증 실패",
        description: e?.message || "인증번호가 일치하지 않습니다.",
        variant: "destructive",
      });
    } finally {
      setIsVerifying(false);
    }
  };

  const formatTime = (sec) => {
    const m = String(Math.floor(sec / 60)).padStart(2, "0");
    const s = String(sec % 60).padStart(2, "0");
    return `${m}:${s}`;
  };

  return (
    <div className="min-h-screen flex w-full bg-slate-50 overflow-hidden relative">
      {/* Background Texture */}
      <div className="absolute inset-0 bg-[url('/noise.svg')] opacity-[0.03] mix-blend-multiply pointer-events-none" />

      {/* Left Panel: Brand Visual */}
      <div className="hidden lg:flex w-1/2 items-center justify-center relative overflow-hidden z-10">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[#0EA5E9] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob" />
        <div className="absolute top-1/3 right-1/4 w-96 h-96 bg-[#2DD4BF] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-2000" />
        <div className="absolute bottom-1/4 left-1/3 w-96 h-96 bg-[#38BDF8] rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-4000" />

        <div className="relative z-10 flex flex-col items-center justify-center h-full p-12">
          <div className="mt-12 text-center">
            <h2 className="text-4xl font-extrabold text-slate-900 mb-4 tracking-tight">비밀번호 찾기</h2>
            <p className="text-slate-600 text-lg leading-relaxed max-w-md">
              휴대폰 인증 후, 계정 이메일로 비밀번호 변경 링크를 전송합니다.
            </p>
          </div>
        </div>
      </div>

      {/* Right Panel */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-4 sm:p-8 relative z-20">
        <div className="w-full max-w-md">
          <Button
            variant="ghost"
            className="mb-6 text-slate-600 hover:text-slate-900 hover:bg-slate-100"
            onClick={() => navigate("/auth?tab=login")}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            로그인으로
          </Button>

          <Card className="border-0 shadow-xl bg-white/80 backdrop-blur-sm">
            <CardHeader className="space-y-2 pb-6">
              <CardTitle className="text-2xl font-bold text-slate-900">
                {step === "verify" ? "휴대폰 인증" : "링크 발송 완료"}
              </CardTitle>
              <CardDescription className="text-slate-600">
                {step === "verify"
                  ? "이름과 휴대폰 번호를 입력하고 인증을 진행하세요."
                  : "이메일로 전송된 링크에서 새 비밀번호를 설정하세요."}
              </CardDescription>
            </CardHeader>

            <CardContent className="pt-0">
              {step === "verify" ? (
                <Form {...verifyForm}>
                  <form className="space-y-4" onSubmit={(e) => e.preventDefault()}>
                    <FormField
                      control={verifyForm.control}
                      name="username"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>이름</FormLabel>
                          <FormControl>
                            <Input
                              placeholder="이름 입력"
                              {...field}
                              disabled={verificationSent}
                              className="bg-white"
                            />
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
                            <FormItem className="flex-1">
                              <FormControl>
                                <Input
                                  placeholder="01012345678"
                                  {...field}
                                  disabled={verificationSent}
                                  className="bg-white"
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        <Button
                          type="button"
                          onClick={handleSendAuth}
                          disabled={verificationSent || isSending}
                          className="shrink-0"
                        >
                          {isSending ? (
                            <>
                              <Loader2 className="mr-2 h-4 w-4 animate-spin" /> 발송중
                            </>
                          ) : (
                            "인증번호 발송"
                          )}
                        </Button>
                      </div>
                    </div>

                    {verificationSent && (
                      <div className="space-y-2">
                        <Label>인증번호</Label>
                        <div className="flex gap-2 items-start">
                          <FormField
                            control={verifyForm.control}
                            name="verificationCode"
                            render={({ field }) => (
                              <FormItem className="flex-1">
                                <FormControl>
                                  <Input placeholder="인증번호 입력" {...field} className="bg-white" />
                                </FormControl>
                                <FormMessage />
                              </FormItem>
                            )}
                          />
                          <Button type="button" onClick={handleConfirmAuth} disabled={isVerifying}>
                            {isVerifying ? (
                              <>
                                <Loader2 className="mr-2 h-4 w-4 animate-spin" /> 확인중
                              </>
                            ) : (
                              "확인"
                            )}
                          </Button>
                        </div>

                        <p className="text-sm text-slate-500">
                          인증번호 유효시간: <span className="font-medium">{formatTime(timer)}</span>
                        </p>

                        {timer === 0 && (
                          <Button
                            type="button"
                            variant="outline"
                            className="w-full mt-2"
                            onClick={() => {
                              setVerificationSent(false);
                              verifyForm.setValue("verificationCode", "");
                              toast({ title: "재발송", description: "인증번호를 다시 발송해주세요." });
                            }}
                          >
                            인증번호 재발송
                          </Button>
                        )}
                      </div>
                    )}
                  </form>
                </Form>
              ) : (
                <div className="space-y-4">
                  <div className="flex items-start gap-3 p-4 rounded-xl bg-slate-50 border">
                    <CheckCircle2 className="h-5 w-5 text-emerald-600 mt-0.5" />
                    <div className="space-y-1">
                      <p className="text-slate-900 font-semibold">비밀번호 변경 링크를 보냈습니다.</p>
                      <p className="text-slate-600 text-sm">
                        {emailMasked ? (
                          <>
                            <span className="font-medium">{emailMasked}</span> 로 전송했습니다. 메일함(스팸함 포함)을 확인하세요.
                          </>
                        ) : (
                          <>등록된 이메일로 전송했습니다. 메일함(스팸함 포함)을 확인하세요.</>
                        )}
                      </p>
                    </div>
                  </div>

                  <Button className="w-full" onClick={() => navigate("/auth?tab=login")}>
                    로그인으로 이동
                  </Button>

                  <Button variant="outline" className="w-full" onClick={resetAll}>
                    다시 시도
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
