import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
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
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { Eye, EyeOff, Loader2, Check, CheckCircle2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { TermsAgreement } from "./TermsAgreement";

// 1. Define Schema
const formSchema = z.object({
    userid: z.string().min(4, "아이디는 4자 이상이어야 합니다."),
    password: z.string()
        .min(8, "비밀번호는 8자 이상이어야 합니다.")
        .regex(/^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])/, "영문, 숫자, 특수문자를 포함해야 합니다."),
    passwordConfirm: z.string(),
    username: z.string().min(2, "이름은 2자 이상이어야 합니다."),
    email: z.string().email("유효한 이메일 주소를 입력해주세요."),
    phone: z.string().regex(/^01[0-9]{8,9}$/, "유효한 휴대폰 번호를 입력해주세요."),
    verificationCode: z.string().min(1, "인증번호를 입력해주세요."),
    gender: z.enum(["male", "female"], { required_error: "성별을 선택해주세요." }),
    birthday: z.string().regex(/^\d{8}$/, "생년월일 8자리를 입력해주세요 (예: 19900101)"),
    referralSource: z.string().optional(),
    terms: z.object({
        age: z.boolean().refine((val) => val === true, "필수 항목입니다."),
        service: z.boolean().refine((val) => val === true, "필수 항목입니다."),
        privacy: z.boolean().optional(), // Based on TermsAgreement, privacy is optional? Or required? Photo says (선택). Keeping it flexible.
        marketing: z.boolean().optional(),
    }),
}).refine((data) => data.password === data.passwordConfirm, {
    message: "비밀번호가 일치하지 않습니다.",
    path: ["passwordConfirm"],
});

export function JobSeekerSignUpForm() {
    const { signUp, requestPhoneVerification, verifyPhone } = useAuth();
    const { toast } = useToast();
    const navigate = useNavigate();

    const [showPassword, setShowPassword] = useState(false);
    const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
    const [verificationSent, setVerificationSent] = useState(false);
    const [isVerifying, setIsVerifying] = useState(false);
    const [isVerified, setIsVerified] = useState(false);
    const [timer, setTimer] = useState(0);

    // 2. Define Form
    const form = useForm({
        resolver: zodResolver(formSchema),
        defaultValues: {
            userid: "",
            password: "",
            passwordConfirm: "",
            username: "",
            email: "",
            phone: "",
            verificationCode: "",
            gender: undefined,
            birthday: "", // YYYYMMDD string
            referralSource: "",
            terms: {
                age: false,
                service: false,
                privacy: false,
                marketing: false,
            },
        },
    });

    // 3. Handlers
    const handlePhoneVerification = async () => {
        const phone = form.getValues("phone");
        if (!phone || !/^01[0-9]{8,9}$/.test(phone)) {
            form.setError("phone", { message: "올바른 휴대폰 번호를 입력해주세요." });
            return;
        }

        // Magic Number for Testing
        if (phone === "01000000000") {
            toast({ title: "[Test Mode]", description: "테스트 번호입니다. 가짜 인증번호를 발송합니다." });
            setVerificationSent(true);
            setTimer(180);
            return;
        }

        try {
            const res = await requestPhoneVerification(phone);
            // API auth.js throws on error, catch block handles it
            // If successful, returns data object
            if (res && res.ok !== false) {
                setVerificationSent(true);
                setTimer(180);
                toast({ title: "인증번호가 발송되었습니다.", description: "3분 이내에 입력해주세요." });
            } else {
                // Should not happen if auth.js throws, but just in case
                toast({ title: "인증번호 발송 실패", description: res?.message || "오류가 발생했습니다.", variant: "destructive" });
            }
        } catch (error) {
            toast({ title: "오류 발생", description: error.message || "인증번호 발송 중 오류가 발생했습니다.", variant: "destructive" });
        }
    };

    const handleVerifyCode = async () => {
        const phone = form.getValues("phone");
        const code = form.getValues("verificationCode");

        if (!code) return form.setError("verificationCode", { message: "인증번호를 입력하세요" });

        // Magic Number Bypass
        if (phone === "01000000000" && code === "123456") {
            setIsVerified(true);
            setIsVerifying(false);
            toast({ title: "[Test Mode]", description: "테스트 인증 성공!" });
            form.clearErrors("verificationCode");
            return;
        }

        setIsVerifying(true);
        try {
            const res = await verifyPhone(phone, code);
            // API auth.js throws on error
            if (res && (res.verified || res.ok)) {
                setIsVerified(true);
                toast({ title: "인증 성공", description: "휴대폰 인증이 완료되었습니다." });
                form.clearErrors("verificationCode");
            } else {
                toast({ title: "인증 실패", description: res?.message || "인증번호가 일치하지 않습니다.", variant: "destructive" });
            }
        } catch (e) {
            toast({ title: "인증 오류", description: e.message || "인증 확인 중 오류가 발생했습니다.", variant: "destructive" });
        } finally {
            setIsVerifying(false);
        }
    };

    const onSubmit = async (data: z.infer<typeof formSchema>) => {
        if (!data.terms.age || !data.terms.service) { // Ensure terms
            toast({ title: "약관 동의 필요", description: "필수 약관에 동의해주세요.", variant: "destructive" });
            document.getElementById("section-terms")?.scrollIntoView({ behavior: "smooth" });
            return;
        }
        if (!isVerified) {
            toast({ title: "인증 필요", description: "휴대폰 인증을 완료해주세요.", variant: "destructive" });
            document.getElementById("section-verify")?.scrollIntoView({ behavior: "smooth" });
            return;
        }

        // Prepare payload for API
        const payload = {
            loginId: data.userid,
            password: data.password,
            name: data.username,
            email: data.email,
            phone: data.phone,
            role: "CANDIDATE",
            gender: data.gender,
            birthday: data.birthday,
            referralSource: data.referralSource,
            marketingAgree: data.terms.marketing,
            terms: data.terms // Pass full terms object for strict compliance check if needed by auth.js
        };

        const res = await signUp(payload);
        if (res.error) {
            toast({ title: "회원가입 실패", description: res.error, variant: "destructive" });
        } else {
            toast({ title: "회원가입 성공", description: "로그인 페이지로 이동합니다." });
        }
    };

    // Sync TermsAgreement with React Hook Form
    const handleTermsChange = (agreements) => {
        form.setValue("terms.age", agreements.age);
        form.setValue("terms.service", agreements.service);
        form.setValue("terms.privacy", agreements.privacy);
        form.setValue("terms.marketing", agreements.marketing);
    };

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">

                {/* 1. 약관 동의 */}
                <section id="section-terms" className="space-y-4">
                    <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">1</span>
                        약관 동의
                    </h3>
                    <TermsAgreement onComplete={() => { }} onChange={handleTermsChange} />
                </section>

                {/* 2. 본인 및 계정 */}
                <section id="section-verify" className="space-y-4">
                    <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">2</span>
                        본인 인증 및 계정 정보
                        {isVerified && <CheckCircle2 className="h-5 w-5 text-green-500 ml-auto" />}
                    </h3>
                    <div className="space-y-4 p-4 bg-card rounded-xl border border-border">
                        {/* Phone Verification */}
                        <div className="space-y-2">
                            <Label>휴대폰 번호 <span className="text-destructive">*</span></Label>
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
                                <Button type="button" variant="secondary" onClick={handlePhoneVerification} disabled={isVerified || verificationSent}>
                                    {verificationSent ? "재전송" : "인증번호 받기"}
                                </Button>
                            </div>
                            {verificationSent && !isVerified && (
                                <div className="flex gap-2">
                                    <FormField
                                        control={form.control}
                                        name="verificationCode"
                                        render={({ field }) => (
                                            <FormItem className="flex-1 space-y-0">
                                                <FormControl>
                                                    <Input placeholder="인증번호 6자리" {...field} />
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                    <Button type="button" onClick={handleVerifyCode} disabled={isVerifying}>
                                        {isVerifying ? <Loader2 className="h-4 w-4 animate-spin" /> : "확인"}
                                    </Button>
                                </div>
                            )}
                        </div>

                        <FormField
                            control={form.control}
                            name="userid"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>아이디 <span className="text-destructive">*</span></FormLabel>
                                    <FormControl>
                                        <Input placeholder="4~12자 이내 영문, 숫자" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="password"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>비밀번호 <span className="text-destructive">*</span></FormLabel>
                                        <div className="relative">
                                            <FormControl>
                                                <Input type={showPassword ? "text" : "password"} placeholder="8자 이상 (영문, 숫자, 특수문자)" {...field} />
                                            </FormControl>
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
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="passwordConfirm"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>비밀번호 확인 <span className="text-destructive">*</span></FormLabel>
                                        <div className="relative">
                                            <FormControl>
                                                <Input type={showPasswordConfirm ? "text" : "password"} placeholder="재입력" {...field} />
                                            </FormControl>
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
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>
                    </div>
                </section>

                {/* 3. 개인 정보 & 나머지 */}
                <section className="space-y-4">
                    <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">3</span>
                        개인 정보
                    </h3>
                    <div className="space-y-4 p-4 bg-card rounded-xl border border-border">
                        <FormField
                            control={form.control}
                            name="username"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>이름 <span className="text-destructive">*</span></FormLabel>
                                    <FormControl>
                                        <Input placeholder="실명 입력" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="email"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>이메일 <span className="text-destructive">*</span></FormLabel>
                                    <FormControl>
                                        <Input placeholder="example@email.com" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="gender"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>성별</FormLabel>
                                        <div className="flex gap-2">
                                            <div
                                                onClick={() => field.onChange("male")}
                                                className={`flex-1 p-2 border rounded-md text-center cursor-pointer transition-colors ${field.value === "male" ? "bg-primary text-primary-foreground border-primary" : "hover:bg-accent"}`}
                                            >
                                                남자
                                            </div>
                                            <div
                                                onClick={() => field.onChange("female")}
                                                className={`flex-1 p-2 border rounded-md text-center cursor-pointer transition-colors ${field.value === "female" ? "bg-primary text-primary-foreground border-primary" : "hover:bg-accent"}`}
                                            >
                                                여자
                                            </div>
                                        </div>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="birthday"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>생년월일</FormLabel>
                                        <FormControl>
                                            <Input placeholder="YYYYMMDD (8자리)" maxLength={8} {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>
                    </div>
                </section>

                <FormField
                    control={form.control}
                    name="referralSource"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>가입 경로 (선택)</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                                <FormControl>
                                    <SelectTrigger>
                                        <SelectValue placeholder="선택해주세요" />
                                    </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                    <SelectItem value="search">검색</SelectItem>
                                    <SelectItem value="sns">SNS</SelectItem>
                                    <SelectItem value="recommendation">추천</SelectItem>
                                    <SelectItem value="advertisement">광고</SelectItem>
                                    <SelectItem value="other">기타</SelectItem>
                                </SelectContent>
                            </Select>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <Button type="submit" className="w-full h-14 text-lg font-bold btn-gradient-primary shadow-xl mt-8">
                    구직자 회원가입 완료
                </Button>
            </form>
        </Form>
    );
}
