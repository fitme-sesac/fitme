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
import { useAuth } from "@/contexts/AuthContext"; // Keeping for consistency if needed, but using direct API actions
import { useToast } from "@/hooks/use-toast";
import { Loader2, ArrowLeft, CheckCircle2, Copy } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { findUserId, verifyUserIdCode } from "@/api/auth";

const formSchema = z.object({
    name: z.string().min(2, "이름을 입력해주세요."),
    phone: z.string().regex(/^01[0-9]{8,9}$/, "유효한 휴대폰 번호를 입력해주세요."),
    verificationCode: z.string().min(1, "인증번호를 입력해주세요."),
});

export default function FindUserIdPage() {
    const { toast } = useToast();
    const navigate = useNavigate();

    const [verificationSent, setVerificationSent] = useState(false);
    const [isVerifying, setIsVerifying] = useState(false);
    const [timer, setTimer] = useState(0);
    const [foundUserId, setFoundUserId] = useState(null);
    const [serverError, setServerError] = useState("");

    const form = useForm({
        resolver: zodResolver(formSchema),
        defaultValues: {
            name: "",
            phone: "",
            verificationCode: "",
        },
    });

    useEffect(() => {
        let interval;
        if (verificationSent && timer > 0 && !foundUserId) {
            interval = setInterval(() => {
                setTimer((prev) => prev - 1);
            }, 1000);
        } else if (timer === 0) {
            clearInterval(interval);
        }
        return () => clearInterval(interval);
    }, [verificationSent, timer, foundUserId]);

    const handleSendAuth = async () => {
        const name = form.getValues("name");
        const phone = form.getValues("phone");

        if (!name) { form.setError("name", { message: "이름을 입력해주세요" }); return; }
        if (!phone || !/^01[0-9]{8,9}$/.test(phone)) { form.setError("phone", { message: "올바른 휴대폰 번호를 입력해주세요" }); return; }

        try {
            setServerError("");
            const res = await findUserId(name, phone);
            setVerificationSent(true);
            setTimer(Number(res?.expiresIn ?? 300));
            toast({ title: "인증번호 발송", description: "입력하신 번호로 인증번호를 보냈습니다." });
        } catch (e) {
            setVerificationSent(false);
            setTimer(0);
            const msg = e?.message || "이름과 휴대폰 번호가 일치하는 회원이 없습니다.";
            setServerError(msg);
            toast({ title: "발송 실패", description: msg, variant: "destructive" });
        }
    };

    const handleConfirmAuth = async () => {
        const phone = form.getValues("phone");
        const code = form.getValues("verificationCode");
        if (!code) {
            form.setError("verificationCode", { message: "인증번호를 입력해주세요" });
            return;
        }

        setIsVerifying(true);
        try {
            setServerError("");
            const res = await verifyUserIdCode(phone, code);
            const userId = res?.userId;
            if (userId) {
                setFoundUserId(userId);
                toast({ title: "인증 성공", description: "아이디를 찾았습니다." });
                navigate(`/auth/find-id/result?userid=${encodeURIComponent(userId)}`);
            } else {
                const msg = "아이디를 찾지 못했습니다. 입력 정보를 다시 확인해주세요.";
                setServerError(msg);
                toast({ title: "실패", description: msg, variant: "destructive" });
            }
        } catch (e) {
            const msg = e?.message || "인증 확인 중 오류가 발생했습니다.";
            setServerError(msg);
            toast({ title: "오류", description: msg, variant: "destructive" });
        } finally {
            setIsVerifying(false);
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
                            아이디를 잊으셨나요?<br />간편하게 찾아보세요.
                        </p>
                    </div>
                </div>
            </div>

            {/* 2. Right Panel: Form Area */}
            <div className="w-full lg:w-1/2 flex flex-col h-screen bg-transparent relative z-20">
                <div className="flex-1 overflow-y-auto overflow-x-hidden p-6 md:p-12 flex items-center">
                    <div className="w-full mx-auto max-w-md transition-all duration-500 ease-in-out">

                        {/* Mobile Heading */}
                        <div className="flex lg:hidden flex-col items-center justify-center gap-4 mb-8 text-center">
                            <h1 className="text-2xl font-bold text-slate-900 drop-shadow-sm">
                                아이디 찾기
                            </h1>
                        </div>

                        <Card className="bg-white/95 backdrop-blur-md rounded-2xl shadow-xl border border-slate-200">
                            <CardHeader className="space-y-1 pb-6">
                                <div className="flex items-center gap-2 mb-2">
                                    <Button variant="ghost" size="icon" onClick={() => navigate("/auth?tab=login")} className="-ml-3 h-8 w-8 rounded-full">
                                        <ArrowLeft className="h-4 w-4" />
                                    </Button>
                                    <CardTitle className="text-xl font-bold">아이디 찾기</CardTitle>
                                </div>
                                <CardDescription>
                                    가입시 등록한 이름과 휴대폰 번호를 입력해주세요.
                                </CardDescription>
                            </CardHeader>
                            <CardContent className="pt-0">
                                {!foundUserId ? (
                                    <Form {...form}>
                                        <form className="space-y-4" onSubmit={(e) => e.preventDefault()}>
                                            {serverError && (
                                                <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                                                    {serverError}
                                                </div>
                                            )}

                                            <FormField
                                                control={form.control}
                                                name="name"
                                                render={({ field }) => (
                                                    <FormItem>
                                                        <FormLabel>이름</FormLabel>
                                                        <FormControl>
                                                            <Input placeholder="이름 입력" {...field} disabled={verificationSent} className="bg-white" />
                                                        </FormControl>
                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />

                                            <div className="space-y-2">
                                                <Label>휴대폰 번호</Label>
                                                <div className="flex gap-2">
                                                    <FormField
                                                        control={form.control}
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
                                                        disabled={verificationSent && timer > 0}
                                                        className="w-24 shrink-0 bg-white"
                                                    >
                                                        {verificationSent ? (timer > 0 ? "발송됨" : "재발송") : "인증 요청"}
                                                    </Button>
                                                </div>
                                            </div>

                                            {verificationSent && (
                                                <div className="space-y-2 animate-in slide-in-from-top-2">
                                                    <div className="flex gap-2 relative">
                                                        <FormField
                                                            control={form.control}
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
                                    <div className="text-center space-y-6 animate-in zoom-in-95 duration-300 py-6">
                                        <div className="flex justify-center">
                                            <div className="rounded-full bg-green-100 p-3">
                                                <CheckCircle2 className="h-12 w-12 text-green-600" />
                                            </div>
                                        </div>
                                        <div className="space-y-2">
                                            <h3 className="text-lg font-medium">회원님의 아이디는 아래와 같습니다.</h3>
                                            <div className="p-4 bg-slate-50 rounded-lg border flex items-center justify-center gap-2">
                                                <span className="text-xl font-bold tracking-wide text-slate-900">{foundUserId}</span>
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => {
                                                        navigator.clipboard.writeText(foundUserId);
                                                        toast({ title: "복사됨", description: "아이디가 클립보드에 복사되었습니다." });
                                                    }}
                                                >
                                                    <Copy className="h-4 w-4" />
                                                </Button>
                                            </div>
                                        </div>
                                        <Button className="w-full btn-gradient-primary h-12 text-lg font-bold shadow-md" onClick={() => navigate("/auth?tab=login")}>
                                            로그인하러 가기
                                        </Button>
                                    </div>
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
