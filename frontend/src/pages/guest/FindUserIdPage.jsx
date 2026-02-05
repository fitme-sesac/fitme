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
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
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
            setTimer(Number(res?.expiresInSec ?? 300));
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
            <div className="absolute inset-0 bg-[url('/noise.svg')] opacity-[0.03] mix-blend-multiply pointer-events-none"></div>

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

            <div className="w-full lg:w-1/2 flex flex-col h-screen bg-transparent relative z-20">
                <div className="flex-1 overflow-y-auto overflow-x-hidden p-6 md:p-12 flex items-center">
                    <div className="w-full mx-auto max-w-md transition-all duration-500 ease-in-out">
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
                                                        <Input placeholder="이름 입력" {...field} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={form.control}
                                            name="phone"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>휴대폰 번호</FormLabel>
                                                    <FormControl>
                                                        <Input placeholder="01012345678" {...field} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        {!verificationSent ? (
                                            <Button type="button" className="w-full" onClick={handleSendAuth}>
                                                인증번호 발송
                                            </Button>
                                        ) : (
                                            <>
                                                <FormField
                                                    control={form.control}
                                                    name="verificationCode"
                                                    render={({ field }) => (
                                                        <FormItem>
                                                            <FormLabel>인증번호</FormLabel>
                                                            <FormControl>
                                                                <Input placeholder="인증번호 입력" {...field} />
                                                            </FormControl>
                                                            <FormMessage />
                                                        </FormItem>
                                                    )}
                                                />

                                                <Button type="button" className="w-full" onClick={handleConfirmAuth} disabled={isVerifying}>
                                                    {isVerifying ? <Loader2 className="h-4 w-4 animate-spin" /> : "인증 확인"}
                                                </Button>

                                                <div className="text-xs text-slate-500 text-center">
                                                    남은 시간: {Math.floor(timer / 60)}:{String(timer % 60).padStart(2, "0")}
                                                </div>
                                            </>
                                        )}
                                    </form>
                                </Form>
                            </CardContent>
                        </Card>
                    </div>
                </div>
            </div>
        </div>
    );
}
