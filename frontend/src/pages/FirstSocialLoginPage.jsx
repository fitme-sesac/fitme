import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { useSearchParams, useNavigate } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Loader2, CheckCircle2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";

import { useToast } from "@/hooks/use-toast";

import { useAuth } from "@/contexts/AuthContext";
import { http } from "@/api/http";

// Schema Definition
const formSchema = z.object({
  email: z.string().email(),
  socialType: z.string(),
  username: z.string().min(2, "이름은 2자 이상이어야 합니다."),
  gender: z.enum(["MALE", "FEMALE"], { required_error: "성별을 선택해주세요." }),
  birthday: z.string().regex(/^\d{8}$/, "생년월일 8자리를 입력해주세요 (예: 19900101)"),
  phone: z.string().regex(/^01[0-9]{8,9}$/, "유효한 휴대폰 번호를 입력해주세요."),
  verificationCode: z.string().min(6, "인증번호 6자리를 입력해주세요."),
  terms: z.object({
    terms: z.boolean().refine(val => val === true, "필수 약관입니다."),
    privacy: z.boolean().refine(val => val === true, "필수 약관입니다."),
    policy: z.boolean().refine(val => val === true, "필수 약관입니다."),
    marketing: z.boolean().optional(),
  }),
});

export default function FirstSocialLoginPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { requestPhoneVerification, verifyPhone } = useAuth(); // AuthContext methods

  // State
  const [isVerifying, setIsVerifying] = useState(false); // Phone verify loading
  const [isVerified, setIsVerified] = useState(false);   // Phone verified status
  const [verificationSent, setVerificationSent] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [timer, setTimer] = useState(0);

  useEffect(() => {
    let interval;
    if (verificationSent && timer > 0 && !isVerified) {
      interval = setInterval(() => {
        setTimer((prev) => prev - 1);
      }, 1000);
    } else if (timer === 0) {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [verificationSent, timer, isVerified]);

  // Initial Values from URL
  const emailParam = searchParams.get("email") || "";
  // socialType default: OTHER if missing
  const socialTypeParam = (searchParams.get("socialType") || searchParams.get("provider") || "OTHER").toUpperCase();
  const usernameParam = searchParams.get("username") || searchParams.get("name") || "";
  const genderParam = (searchParams.get("gender") || "").toUpperCase();
  const birthdayParam = searchParams.get("birthday") || "";
  const phoneParam = (searchParams.get("phone") || "").replace(/[^0-9]/g, "");

  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      email: emailParam,
      socialType: socialTypeParam,
      username: usernameParam,
      gender: (genderParam === "MALE" || genderParam === "FEMALE") ? genderParam : undefined,
      birthday: birthdayParam,
      phone: phoneParam,
      verificationCode: "",
      terms: {
        terms: false,
        privacy: false,
        policy: false,
        marketing: false,
      },
    },
  });

  // Populate form if params change (safeguard)
  useEffect(() => {
    if (emailParam) form.setValue("email", emailParam);
    if (socialTypeParam) form.setValue("socialType", socialTypeParam);
    if (usernameParam) form.setValue("username", usernameParam);
  }, [emailParam, socialTypeParam, usernameParam, form]);

  // Handlers
  const handlePhoneVerification = async () => {
    const phone = form.getValues("phone");
    if (!phone || !/^01[0-9]{8,9}$/.test(phone)) {
      form.setError("phone", { message: "올바른 휴대폰 번호를 입력해주세요." });
      return;
    }

    try {
      const res = await requestPhoneVerification(phone);
      if (res && res.ok !== false) {
        setVerificationSent(true);
        setTimer(300); // 5 minutes
        toast({ title: "인증번호 발송", description: "문자로 인증번호가 발송되었습니다." });
      } else {
        toast({ title: "발송 실패", description: res?.message || "오류가 발생했습니다.", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "오류", description: "인증번호 발송 중 오류가 발생했습니다.", variant: "destructive" });
    }
  };

  const handleVerifyCode = async () => {
    const phone = form.getValues("phone");
    const code = form.getValues("verificationCode");

    if (!code) return form.setError("verificationCode", { message: "인증번호를 입력하세요" });

    setIsVerifying(true);
    try {
      const res = await verifyPhone(phone, code);
      if (res && (res.verified || res.ok)) {
        setIsVerified(true);
        toast({ title: "인증 성공", description: "휴대폰 인증이 완료되었습니다.", className: "bg-green-600 text-white" });
        form.clearErrors("verificationCode");
      } else {
        toast({ title: "인증 실패", description: res?.message || "인증번호가 일치하지 않습니다.", variant: "destructive" });
      }
    } catch (e) {
      toast({ title: "오류", description: "인증 확인 중 오류가 발생했습니다.", variant: "destructive" });
    } finally {
      setIsVerifying(false);
    }
  };

  const onSubmit = async (data) => {
    if (!isVerified) {
      toast({ title: "인증 필요", description: "휴대폰 인증을 완료해주세요.", variant: "destructive" });
      return;
    }

    setIsSubmitting(true);
    try {
      // Build FormData for Legacy Backend Endpoint
      const formData = new FormData();
      formData.append("email", data.email);
      formData.append("socialType", data.socialType);
      formData.append("username", data.username);
      formData.append("gender", data.gender);
      formData.append("birthday", data.birthday);
      formData.append("phone", data.phone);

      // Terms (converting boolean to string "true"/"false" if backend expects that, usually "true" is checked)
      formData.append("agreeTerms", "true");
      formData.append("agreePrivacy", "true");
      formData.append("agreePolicy", "true");
      formData.append("marketingOptIn", data.terms.marketing ? "true" : "false");

      // Assuming explicit endpoint /User/First_Social_Login as per legacy form action
      const response = await http.post("/User/First_Social_Login", formData, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      });

      // Legacy backend usually redirects or returns HTML. 
      // If it returns JSON { ok: true } or similar, we navigate.
      // If it behaves like a form submit redirect, we might need to handle it.
      // Assuming standard API-like behavior or successful 200 OK means we can go to main/login.

      if (response.status === 200) {
        toast({ title: "가입 완료", description: "환영합니다! 다시 로그인해주세요." });
        navigate("/auth?tab=login"); // Or directly to home if backend sets session cookie
      } else {
        throw new Error("처리 실패");
      }

    } catch (error) {
      console.error(error);
      toast({ title: "가입 실패", description: "서버 오류가 발생했습니다. 관리자에게 문의하세요.", variant: "destructive" });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <Card className="w-full max-w-lg shadow-xl border-slate-200">
        <CardHeader className="space-y-1 text-center pb-8 border-b bg-white rounded-t-xl">
          <CardTitle className="text-2xl font-bold tracking-tight">추가 정보 입력</CardTitle>
          <CardDescription>
            원활한 서비스 이용을 위해 추가 정보를 입력해주세요.<br />
            <span className="text-xs text-muted-foreground mt-1 inline-block">
              * 최초 소셜 로그인 시 1회만 진행됩니다.
            </span>
          </CardDescription>
        </CardHeader>

        <CardContent className="pt-6 bg-white/50">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">

              {/* Read Only Info */}
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-100 rounded-lg text-sm mb-6">
                <div>
                  <span className="text-muted-foreground block text-xs mb-1">이메일</span>
                  <span className="font-medium text-slate-800 break-all">{emailParam}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-xs mb-1">연동 계정</span>
                  <span className="font-medium text-slate-800">{socialTypeParam}</span>
                </div>
              </div>

              <FormField
                control={form.control}
                name="username"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>이름 <span className="text-red-500">*</span></FormLabel>
                    <FormControl>
                      <Input placeholder="실명 입력" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="birthday"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>생년월일 <span className="text-red-500">*</span></FormLabel>
                      <FormControl>
                        <Input placeholder="YYYYMMDD (8자리)" {...field} maxLength={8} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="gender"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>성별 <span className="text-red-500">*</span></FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="선택" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="MALE">남성</SelectItem>
                          <SelectItem value="FEMALE">여성</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Phone Verification */}
              <div className="space-y-2">
                <Label>휴대폰 <span className="text-red-500">*</span></Label>
                <div className="flex gap-2">
                  <FormField
                    control={form.control}
                    name="phone"
                    render={({ field }) => (
                      <FormItem className="flex-1 space-y-0">
                        <FormControl>
                          <Input placeholder="01012345678" {...field} disabled={isVerified} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handlePhoneVerification}
                    disabled={isVerified || (verificationSent && timer > 0)}
                    className="w-28 whitespace-nowrap"
                  >
                    {verificationSent ? "재전송" : "인증번호 받기"}
                  </Button>
                </div>

                {verificationSent && !isVerified && (
                  <div className="flex gap-2 animate-in slide-in-from-top-2">
                    <FormField
                      control={form.control}
                      name="verificationCode"
                      render={({ field }) => (
                        <FormItem className="flex-1 space-y-0 relative">
                          <FormControl>
                            <Input placeholder="인증번호 6자리" {...field} />
                          </FormControl>
                          <span className="absolute right-3 top-2.5 text-sm text-red-500 font-medium">
                            {Math.floor(timer / 60)}:{String(timer % 60).padStart(2, '0')}
                          </span>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <Button
                      type="button"
                      onClick={handleVerifyCode}
                      disabled={isVerifying || timer === 0}
                      className="w-28"
                    >
                      {isVerifying ? <Loader2 className="h-4 w-4 animate-spin" /> : "확인"}
                    </Button>
                  </div>
                )}
                {isVerified && <div className="text-green-600 text-sm flex items-center gap-1 font-medium"><CheckCircle2 className="w-4 h-4" /> 인증 완료되었습니다.</div>}
                {verificationSent && timer === 0 && !isVerified && <div className="text-red-500 text-sm">인증 시간이 만료되었습니다. 재전송 버튼을 눌러주세요.</div>}
              </div>

              {/* Terms */}
              <div className="space-y-3 pt-4 border-t">
                <Label className="text-base font-semibold">약관 동의</Label>
                <div className="space-y-4 bg-slate-50 p-4 rounded-lg border">
                  {/* Select All */}
                  <div className="flex items-center space-x-3 pb-4 border-b border-slate-200">
                    <Checkbox
                      id="all"
                      checked={
                        form.watch("terms.terms") &&
                        form.watch("terms.privacy") &&
                        form.watch("terms.policy") &&
                        form.watch("terms.marketing")
                      }
                      onCheckedChange={(checked) => {
                        const val = !!checked;
                        form.setValue("terms.terms", val);
                        form.setValue("terms.privacy", val);
                        form.setValue("terms.policy", val);
                        form.setValue("terms.marketing", val);
                      }}
                    />
                    <Label htmlFor="all" className="font-bold cursor-pointer">
                      약관 전체 동의
                    </Label>
                  </div>

                  <div className="space-y-3 pl-1">
                    <FormField
                      control={form.control}
                      name="terms.terms"
                      render={({ field }) => (
                        <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                          <FormControl>
                            <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                          </FormControl>
                          <div className="space-y-1 leading-none">
                            <FormLabel className="font-normal cursor-pointer">
                              (필수) 이용약관 동의
                            </FormLabel>
                          </div>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="terms.privacy"
                      render={({ field }) => (
                        <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                          <FormControl>
                            <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                          </FormControl>
                          <div className="space-y-1 leading-none">
                            <FormLabel className="font-normal cursor-pointer">
                              (필수) 개인정보 처리방침 동의
                            </FormLabel>
                          </div>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="terms.policy"
                      render={({ field }) => (
                        <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                          <FormControl>
                            <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                          </FormControl>
                          <div className="space-y-1 leading-none">
                            <FormLabel className="font-normal cursor-pointer">
                              (필수) 운영정책 동의
                            </FormLabel>
                          </div>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="terms.marketing"
                      render={({ field }) => (
                        <FormItem className="flex flex-row items-start space-x-3 space-y-0 text-muted-foreground">
                          <FormControl>
                            <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                          </FormControl>
                          <div className="space-y-1 leading-none">
                            <FormLabel className="font-normal cursor-pointer">
                              (선택) 마케팅 정보 수신 동의
                            </FormLabel>
                          </div>
                        </FormItem>
                      )}
                    />
                  </div>
                </div>
              </div>

              <Button
                type="submit"
                className="w-full h-12 text-lg font-bold bg-gradient-to-r from-sky-500 to-teal-400 hover:opacity-90 transition-opacity"
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    처리 중...
                  </>
                ) : (
                  "가입 완료"
                )}
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}
