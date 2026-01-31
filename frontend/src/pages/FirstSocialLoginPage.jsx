import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { useSearchParams } from "react-router-dom";
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
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/contexts/AuthContext";

const DRAFT_KEY = "firstSocialLoginDraft";

const isYyyyMmDd = (v) => /^\d{4}-\d{2}-\d{2}$/.test(v);
const isYyyyMmDd8 = (v) => /^\d{8}$/.test(v);

const normalizeBirthdayToYyyyMmDd = (v) => {
    const s = (v || "").trim();
    if (isYyyyMmDd(s)) return s;
    if (isYyyyMmDd8(s)) return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
    return s;
};

const postByHiddenForm = (action, fields) => {
    const form = document.createElement("form");
    form.method = "POST";
    form.action = action;

    Object.entries(fields).forEach(([name, value]) => {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = name;
        input.value = value == null ? "" : String(value);
        form.appendChild(input);
    });

    document.body.appendChild(form);
    form.submit();
};

// Schema Definition
const formSchema = z.object({
    email: z.string().email(),
    socialType: z.string(),
    username: z.string().min(2, "이름은 2자 이상이어야 합니다."),
    gender: z.enum(["MALE", "FEMALE"], { required_error: "성별을 선택해주세요." }),
    // ✅ 네이버에서 기본 YYYY-MM-DD로 내려오므로 둘 다 허용
    birthday: z.string().refine((v) => isYyyyMmDd(v) || isYyyyMmDd8(v), {
        message: "생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD 또는 YYYYMMDD)",
    }),
    phone: z.string().regex(/^01[0-9]{8,9}$/, "유효한 휴대폰 번호를 입력해주세요."),
    verificationCode: z.string().min(6, "인증번호 6자리를 입력해주세요."),
    terms: z.object({
        terms: z.boolean().refine((val) => val === true, "필수 약관입니다."),
        privacy: z.boolean().refine((val) => val === true, "필수 약관입니다."),
        policy: z.boolean().refine((val) => val === true, "필수 약관입니다."),
        marketing: z.boolean().optional(),
    }),
});

export default function FirstSocialLoginPage() {
    const [searchParams] = useSearchParams();
    const { toast } = useToast();
    const { requestPhoneVerification, verifyPhone } = useAuth();

    const [isVerifying, setIsVerifying] = useState(false);
    const [isVerified, setIsVerified] = useState(false);
    const [verificationSent, setVerificationSent] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [timer, setTimer] = useState(0);

    const [pageError, setPageError] = useState("");

    // Initial Values from URL
    const emailParam = searchParams.get("email") || "";
    const socialTypeParam = (searchParams.get("socialType") || searchParams.get("provider") || "OTHER").toUpperCase();
    const usernameParam = searchParams.get("username") || searchParams.get("name") || "";
    const genderParam = (searchParams.get("gender") || "").toUpperCase();
    const birthdayParam = searchParams.get("birthday") || "";
    const phoneParam = (searchParams.get("phone") || "").replace(/[^0-9]/g, "");

    const errorMessageParam = searchParams.get("errorMessage") || "";

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

    // ✅ redirect(errorMessage만 있음)로 돌아올 때 입력값 보존
    useEffect(() => {
        const raw = sessionStorage.getItem(DRAFT_KEY);
        if (!raw) return;

        try {
            const d = JSON.parse(raw);

            if (!emailParam && d.email) form.setValue("email", d.email);
            if (!socialTypeParam && d.socialType) form.setValue("socialType", d.socialType);
            if (!usernameParam && d.username) form.setValue("username", d.username);
            if (!birthdayParam && d.birthday) form.setValue("birthday", d.birthday);
            if (!phoneParam && d.phone) form.setValue("phone", d.phone);

            if (d.gender) form.setValue("gender", d.gender);
            if (d.terms) form.setValue("terms", d.terms);
        } catch {
            // ignore
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // 기본 params 반영 + draft 저장
    useEffect(() => {
        if (emailParam) form.setValue("email", emailParam);
        if (socialTypeParam) form.setValue("socialType", socialTypeParam);
        if (usernameParam) form.setValue("username", usernameParam);
        if (birthdayParam) form.setValue("birthday", birthdayParam);
        if (phoneParam) form.setValue("phone", phoneParam);

        sessionStorage.setItem(DRAFT_KEY, JSON.stringify({
            email: emailParam,
            socialType: socialTypeParam,
            username: usernameParam,
            gender: (genderParam === "MALE" || genderParam === "FEMALE") ? genderParam : undefined,
            birthday: birthdayParam,
            phone: phoneParam,
            terms: form.getValues("terms"),
        }));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [emailParam, socialTypeParam, usernameParam, genderParam, birthdayParam, phoneParam]);

    // 타이머
    useEffect(() => {
        let interval;
        if (verificationSent && timer > 0 && !isVerified) {
            interval = setInterval(() => setTimer((prev) => prev - 1), 1000);
        } else if (timer === 0) {
            clearInterval(interval);
        }
        return () => clearInterval(interval);
    }, [verificationSent, timer, isVerified]);

    // ✅ 백엔드 redirect errorMessage 표시
    useEffect(() => {
        if (!errorMessageParam) return;

        setPageError(errorMessageParam);
        toast({
            title: "처리 실패",
            description: errorMessageParam,
            variant: "destructive",
        });

        // 휴대폰 관련 에러면 재입력 UX
        if (errorMessageParam.includes("휴대폰")) {
            setIsVerified(false);
            setVerificationSent(false);
            setTimer(0);
            form.setValue("verificationCode", "");

            if (errorMessageParam.includes("이미 다른 계정")) {
                form.setValue("phone", "");
            }

            form.setError("phone", { message: errorMessageParam });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [errorMessageParam]);

    const handlePhoneVerification = async () => {
        const phone = form.getValues("phone");
        if (!phone || !/^01[0-9]{8,9}$/.test(phone)) {
            form.setError("phone", { message: "올바른 휴대폰 번호를 입력해주세요." });
            return;
        }

        try {
            const { data, error } = await requestPhoneVerification(phone);

            if (error) {
                // ✅ 페이지 내 문구(폼 에러)로 노출
                form.setError("phone", { message: String(error) });
                setPageError(String(error));

                setVerificationSent(false);
                setIsVerified(false);
                setTimer(0);
                form.setValue("verificationCode", "");

                toast({ title: "발송 실패", description: String(error), variant: "destructive" });
                return;
            }

            if (!data?.ok) {
                const msg = data?.message || "오류가 발생했습니다.";
                form.setError("phone", { message: msg });
                setPageError(msg);

                setVerificationSent(false);
                setIsVerified(false);
                setTimer(0);
                form.setValue("verificationCode", "");

                toast({ title: "발송 실패", description: msg, variant: "destructive" });
                return;
            }

            setPageError("");
            setVerificationSent(true);
            setIsVerified(false);
            form.setValue("verificationCode", "");
            setTimer(300);

            toast({ title: "인증번호 발송", description: "문자로 인증번호가 발송되었습니다." });
        } catch {
            const msg = "인증번호 발송 중 오류가 발생했습니다.";
            form.setError("phone", { message: msg });
            setPageError(msg);
            toast({ title: "오류", description: msg, variant: "destructive" });
        }
    };

    const handleVerifyCode = async () => {
        const phone = form.getValues("phone");
        const code = form.getValues("verificationCode");

        if (!code) {
            form.setError("verificationCode", { message: "인증번호를 입력하세요" });
            return;
        }

        setIsVerifying(true);
        try {
            const { data, error } = await verifyPhone(phone, code);

            if (error) {
                toast({ title: "인증 오류", description: String(error), variant: "destructive" });
                return;
            }

            const ok = Boolean(data?.verified ?? data?.ok);

            if (ok) {
                setIsVerified(true);
                setTimer(0);
                setPageError("");
                toast({ title: "인증 성공", description: "휴대폰 인증이 완료되었습니다.", className: "bg-green-600 text-white" });
                form.clearErrors("verificationCode");
            } else {
                toast({ title: "인증 실패", description: data?.message || "인증번호가 일치하지 않습니다.", variant: "destructive" });
            }
        } catch {
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

        // redirect 에러 시 복구용 draft 저장
        sessionStorage.setItem(
            DRAFT_KEY,
            JSON.stringify({
                ...data,
                terms: data.terms,
            })
        );

        // ✅ 백엔드가 redirect(errorMessage)를 쓰는 구조라 axios로 보내면 실패/성공 구분이 깨짐
        //    반드시 브라우저 폼 제출로 보낸다.
        const birthday = normalizeBirthdayToYyyyMmDd(data.birthday);

        postByHiddenForm("/User/First_Social_Login", {
            email: data.email,
            socialType: data.socialType,
            username: data.username,
            gender: data.gender,
            birthday,
            phone: data.phone,

            agreeTerms: "true",
            agreePrivacy: "true",
            agreePolicy: "true",
            marketingOptIn: data.terms.marketing ? "true" : "false",
        });
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
                    {/* ✅ 페이지 내 에러 문구 */}
                    {pageError && (
                        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                            {pageError}
                        </div>
                    )}

                    <Form {...form}>
                        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">

                            <div className="grid grid-cols-2 gap-4 p-4 bg-slate-100 rounded-lg text-sm mb-6">
                                <div>
                                    <span className="text-muted-foreground block text-xs mb-1">이메일</span>
                                    <span className="font-medium text-slate-800 break-all">{form.getValues("email")}</span>
                                </div>
                                <div>
                                    <span className="text-muted-foreground block text-xs mb-1">연동 계정</span>
                                    <span className="font-medium text-slate-800">{form.getValues("socialType")}</span>
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
                                                <Input placeholder="YYYY-MM-DD 또는 YYYYMMDD" {...field} />
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

                                {isVerified && (
                                    <div className="text-green-600 text-sm flex items-center gap-1 font-medium">
                                        <CheckCircle2 className="w-4 h-4" /> 인증 완료되었습니다.
                                    </div>
                                )}
                                {verificationSent && timer === 0 && !isVerified && (
                                    <div className="text-red-500 text-sm">인증 시간이 만료되었습니다. 재전송 버튼을 눌러주세요.</div>
                                )}
                            </div>

                            {/* Terms */}
                            <div className="space-y-3 pt-4 border-t">
                                <Label className="text-base font-semibold">약관 동의</Label>
                                <div className="space-y-4 bg-slate-50 p-4 rounded-lg border">
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
