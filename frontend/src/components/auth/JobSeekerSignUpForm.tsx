import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { Loader2, CheckCircle2, User, Eye, EyeOff } from "lucide-react";
import { z } from "zod";
import { TermsAgreement } from "./TermsAgreement";

// Validation Schema
const formSchema = z
    .object({
        userid: z
            .string()
            .min(4, "아이디는 4자 이상이어야 합니다")
            .regex(/^[a-z0-9]+$/, "영문 소문자와 숫자만 가능합니다"),
        password: z
            .string()
            .min(8, "비밀번호는 8자 이상이어야 합니다")
            .regex(/^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])/, "영문, 숫자, 특수문자를 포함해야 합니다"),
        confirmPassword: z.string(),
        username: z.string().min(1, "이름을 입력해주세요"),
        email: z.string().email("유효한 이메일 주소를 입력해주세요"),
        phone: z.string().regex(/^01[0-9]{8,9}$/, "유효한 휴대폰 번호를 입력해주세요."),
        verificationCode: z.string().optional(),
        gender: z.enum(["MALE", "FEMALE"], { required_error: "성별을 선택해주세요" }),
        birthday: z.string().regex(/^\d{8}$/, "생년월일 8자리를 입력해주세요 (예: 19900101)"),
    })
    .refine((data) => data.password === data.confirmPassword, {
        message: "비밀번호가 일치하지 않습니다",
        path: ["confirmPassword"],
    });

// ✅ named export 유지 (SignUpForm.tsx의 import { JobSeekerSignUpForm } 와 호환)
export function JobSeekerSignUpForm() {
    const [formData, setFormData] = useState({
        userid: "",
        password: "",
        confirmPassword: "",
        username: "",
        email: "",
        phone: "",
        verificationCode: "",
        gender: "",
        birthday: "",
    });

    // Steps State
    const [termsAgreed, setTermsAgreed] = useState(false);
    const [agreements, setAgreements] = useState<Record<string, boolean>>({});
    const [verificationSent, setVerificationSent] = useState(false);
    const [isVerifying, setIsVerifying] = useState(false);
    const [isVerified, setIsVerified] = useState(false);
    const [timer, setTimer] = useState(0);
    const [isLoading, setIsLoading] = useState(false);
    const [serverError, setServerError] = useState<string | null>(null);
    const [showPassword, setShowPassword] = useState(false);
    const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);

    const { signUp, requestPhoneVerification, verifyPhone } = useAuth();
    const { toast } = useToast();

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { id, value } = e.target;
        setFormData((prev) => ({ ...prev, [id]: value }));
        if (serverError) setServerError(null);
    };

    useEffect(() => {
        let interval: ReturnType<typeof setInterval> | undefined;

        if (verificationSent && timer > 0 && !isVerified) {
            interval = setInterval(() => {
                setTimer((prev) => prev - 1);
            }, 1000);
        } else if (timer === 0) {
            if (interval) clearInterval(interval);
        }
        return () => {
            if (interval) clearInterval(interval);
        };
    }, [verificationSent, timer, isVerified]);

    // Phone Verification
    const handlePhoneVerification = async () => {
        const phone = formData.phone;
        if (!phone || !/^01[0-9]{8,9}$/.test(phone)) {
            toast({ variant: "destructive", title: "입력 오류", description: "올바른 휴대폰 번호를 입력해주세요." });
            return;
        }

        // Magic Number for Testing
        if (phone === "01000000000") {
            toast({ title: "[Test Mode]", description: "테스트 번호입니다. 가짜 인증번호를 발송합니다." });
            setVerificationSent(true);
            setIsVerified(false);
            setFormData((p) => ({ ...p, verificationCode: "" }));
            setTimer(180);
            return;
        }

        try {
            const { data, error } = await requestPhoneVerification(phone);

            if (error) {
                toast({
                    title: "발송 실패",
                    description: String(error),
                    variant: "destructive",
                });
                return;
            }

            if (!data?.ok) {
                toast({
                    title: "발송 실패",
                    description: data?.message || "인증번호 발송에 실패했습니다.",
                    variant: "destructive",
                });
                return;
            }

            setVerificationSent(true);
            setIsVerified(false);
            setFormData((p) => ({ ...p, verificationCode: "" }));
            setTimer(300);
            toast({ title: "인증번호 발송", description: "인증번호가 발송되었습니다. 5분 이내에 입력해주세요." });
        } catch (error: any) {
            toast({
                title: "오류 발생",
                description: error?.message || "인증번호 발송 중 오류가 발생했습니다.",
                variant: "destructive",
            });
        }
    };

    const handleVerifyCode = async () => {
        const phone = formData.phone;
        const code = formData.verificationCode;

        if (!code) {
            toast({ variant: "destructive", title: "입력 오류", description: "인증번호를 입력하세요" });
            return;
        }

        // Magic Number Bypass
        if (phone === "01000000000" && code === "123456") {
            setIsVerified(true);
            setIsVerifying(false);
            setTimer(0);
            toast({ title: "[Test Mode]", description: "테스트 인증 성공!" });
            return;
        }

        setIsVerifying(true);
        try {
            const { data, error } = await verifyPhone(phone, code);

            if (error) {
                toast({
                    title: "인증 오류",
                    description: String(error),
                    variant: "destructive",
                });
                return;
            }

            const ok = Boolean(data?.verified ?? data?.ok);

            if (ok) {
                setIsVerified(true);
                setTimer(0);
                toast({ title: "인증 성공", description: "휴대폰 인증이 완료되었습니다." });
            } else {
                toast({
                    title: "인증 실패",
                    description: data?.message || "인증번호가 일치하지 않습니다.",
                    variant: "destructive",
                });
            }
        } catch (e: any) {
            toast({
                title: "인증 오류",
                description: e?.message || "인증 확인 중 오류가 발생했습니다.",
                variant: "destructive",
            });
        } finally {
            setIsVerifying(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!termsAgreed) {
            toast({ variant: "destructive", title: "약관 동의 필요", description: "필수 약관에 동의해주세요." });
            document.getElementById("section-terms")?.scrollIntoView({ behavior: "smooth" });
            return;
        }
        if (!isVerified) {
            toast({ variant: "destructive", title: "본인 인증 필요", description: "휴대폰 인증을 완료해주세요." });
            document.getElementById("section-verify")?.scrollIntoView({ behavior: "smooth" });
            return;
        }

        const result = formSchema.safeParse(formData);
        if (!result.success) {
            toast({
                variant: "destructive",
                title: "입력 오류",
                description: result.error.errors[0].message,
            });
            return;
        }

        setIsLoading(true);

        const userData = {
            // 백엔드(UserController.registerProc/UserService.register)가 기대하는 필드명에 최대한 맞춘다.
            userid: formData.userid,
            password: formData.password,
            passwordConfirm: formData.confirmPassword,
            username: formData.username,
            email: formData.email,
            phone: formData.phone,
            gender: formData.gender,
            // UI는 YYYYMMDD를 받지만, API에서 YYYY-MM-DD로 변환한다.
            birthday: formData.birthday,
            agreeTerms: Boolean(agreements?.service),
            agreePrivacy: Boolean(agreements?.privacy),
            agreePolicy: Boolean(agreements?.policy),
            marketingOptIn: Boolean(agreements?.marketing),
        };

        try {
            const { data, error } = await signUp(userData);
            if (error) {
                setServerError(String(error));
                toast({ variant: "destructive", title: "회원가입 실패", description: error });
            } else {
                setServerError(null);
                toast({ title: "회원가입 성공!", description: "구직자 회원으로 가입되었습니다." });
                window.location.href = "/auth?tab=login";
            }
        } catch (err: any) {
            setServerError(err?.message || "알 수 없는 오류가 발생했습니다.");
            toast({ variant: "destructive", title: "오류", description: err.message || "알 수 없는 오류가 발생했습니다." });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-8">
            {serverError && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {serverError}
                </div>
            )}
            {/* 1. 약관 동의 */}
            <section id="section-terms" className="space-y-4">
                <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">1</span>
                    약관 동의
                    {termsAgreed && <CheckCircle2 className="h-5 w-5 text-green-500 ml-auto" />}
                </h3>
                <div className="p-4 bg-card rounded-xl border border-border">
                    <TermsAgreement onComplete={setTermsAgreed} onChange={setAgreements} type="jobSeeker" />
                </div>
            </section>

            {/* 2. 본인 인증 및 계정 */}
            <section id="section-verify" className="space-y-4">
                <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">2</span>
                    본인 인증 및 계정 정보
                    {isVerified && <CheckCircle2 className="h-5 w-5 text-green-500 ml-auto" />}
                </h3>
                <div className="space-y-4 p-4 bg-card rounded-xl border border-border">
                    {/* Phone Verification */}
                    <div className="space-y-2">
                        <Label htmlFor="phone">
                            휴대폰 번호 <span className="text-destructive">*</span>
                        </Label>
                        <div className="flex gap-2">
                            <Input
                                id="phone"
                                placeholder="01012345678"
                                value={formData.phone}
                                onChange={handleChange}
                                disabled={isVerified}
                                className="flex-1"
                            />
                            <Button
                                type="button"
                                variant="secondary"
                                onClick={handlePhoneVerification}
                                disabled={isVerified || (verificationSent && timer > 0)}
                                className="w-28 whitespace-nowrap"
                            >
                                {verificationSent ? "재전송" : "인증번호 받기"}
                            </Button>
                        </div>

                        {verificationSent && !isVerified && (
                            <div className="flex gap-2 relative">
                                <div className="flex-1 relative">
                                    <Input
                                        id="verificationCode"
                                        placeholder="인증번호 6자리"
                                        value={formData.verificationCode}
                                        onChange={handleChange}
                                        className="w-full"
                                    />
                                    <span className="absolute right-3 top-2.5 text-sm text-destructive font-medium">
                    {Math.floor(timer / 60)}:{String(timer % 60).padStart(2, "0")}
                  </span>
                                </div>
                                <Button type="button" onClick={handleVerifyCode} disabled={isVerifying || timer === 0} className="w-28">
                                    {isVerifying ? <Loader2 className="h-4 w-4 animate-spin" /> : "확인"}
                                </Button>
                                {timer === 0 && (
                                    <div className="absolute -bottom-6 left-0 text-destructive text-xs">
                                        인증 시간이 만료되었습니다. 재전송 버튼을 눌러주세요.
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="userid">
                            아이디 <span className="text-destructive">*</span>
                        </Label>
                        <Input id="userid" placeholder="아이디를 입력해주세요" value={formData.userid} onChange={handleChange} required />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="password">
                                비밀번호 <span className="text-destructive">*</span>
                            </Label>
                            <div className="relative">
                                <Input
                                    id="password"
                                    type={showPassword ? "text" : "password"}
                                    placeholder="8자 이상 (영문, 숫자, 특수문자)"
                                    value={formData.password}
                                    onChange={handleChange}
                                    required
                                />
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                                    onClick={() => setShowPassword(!showPassword)}
                                >
                                    {showPassword ? <EyeOff className="h-4 w-4 text-muted-foreground" /> : <Eye className="h-4 w-4 text-muted-foreground" />}
                                </Button>
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="confirmPassword">
                                비밀번호 확인 <span className="text-destructive">*</span>
                            </Label>
                            <div className="relative">
                                <Input
                                    id="confirmPassword"
                                    type={showPasswordConfirm ? "text" : "password"}
                                    placeholder="비밀번호 재입력"
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    required
                                />
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                                    onClick={() => setShowPasswordConfirm(!showPasswordConfirm)}
                                >
                                    {showPasswordConfirm ? <EyeOff className="h-4 w-4 text-muted-foreground" /> : <Eye className="h-4 w-4 text-muted-foreground" />}
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* 3. 개인 정보 */}
            <section className="space-y-4">
                <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">3</span>
                    개인 정보
                </h3>
                <div className="space-y-4 p-4 bg-card rounded-xl border border-border">
                    <div className="space-y-2">
                        <Label htmlFor="username" className="flex items-center gap-2">
                            <User className="h-4 w-4" /> 이름 <span className="text-destructive">*</span>
                        </Label>
                        <Input id="username" placeholder="이름" value={formData.username} onChange={handleChange} required />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="email">
                            이메일 <span className="text-destructive">*</span>
                        </Label>
                        <Input id="email" type="email" placeholder="email@example.com" value={formData.email} onChange={handleChange} required />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="gender">
                                성별 <span className="text-destructive">*</span>
                            </Label>
                            <select
                                id="gender"
                                value={formData.gender}
                                onChange={(e) => setFormData((p) => ({ ...p, gender: e.target.value }))}
                                className="w-full h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
                                required
                            >
                                <option value="">선택</option>
                                <option value="MALE">남성</option>
                                <option value="FEMALE">여성</option>
                            </select>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="birthday">
                                생년월일 <span className="text-destructive">*</span>
                            </Label>
                            <Input id="birthday" placeholder="YYYYMMDD" value={formData.birthday} onChange={handleChange} maxLength={8} required />
                        </div>
                    </div>
                </div>
            </section>

            <Button type="submit" className="w-full h-14 text-lg font-bold btn-gradient-primary shadow-xl mt-8" disabled={isLoading}>
                {isLoading ? (
                    <>
                        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                        가입 처리 중...
                    </>
                ) : (
                    "구직자 회원가입 완료"
                )}
            </Button>
        </form>
    );
}

export default JobSeekerSignUpForm;
