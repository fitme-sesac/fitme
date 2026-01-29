import { useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { ChevronRight } from "lucide-react";

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
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/components/ui/use-toast";

export default function JobSeekerSignup() {
    const navigate = useNavigate();
    const { signUp, requestPhoneVerification, verifyPhone } = useAuth();
    const { toast } = useToast();

    const { control, register, handleSubmit, watch, setValue, formState: { errors, isSubmitting } } = useForm({
        defaultValues: {
            userid: "",
            password: "",
            username: "",
            email: "",
            phone: "",
            otp: "",
            gender: "",
            birthday: "",
            referralSource: "search"
        }
    });

    const [agreements, setAgreements] = useState({
        all: false,
        age: false,
        service: false,
        privacy: false,
        marketing: false,
        thirdParty: false,
    });

    const [otpSent, setOtpSent] = useState(false);
    const [phoneVerified, setPhoneVerified] = useState(false);
    const [verifying, setVerifying] = useState(false);

    // Handlers for checkboxes
    const handleAllCheck = (checked: boolean) => {
        setAgreements({
            all: checked,
            age: checked,
            service: checked,
            privacy: checked,
            marketing: checked,
            thirdParty: checked,
        });
    };

    const handleSingleCheck = (key: keyof typeof agreements, checked: boolean) => {
        const newAgreements = { ...agreements, [key]: checked };
        const allChecked =
            newAgreements.age &&
            newAgreements.service &&
            newAgreements.privacy && // Assuming these are required as per RegisterPage.jsx
            newAgreements.marketing &&
            newAgreements.thirdParty;

        setAgreements({ ...newAgreements, [key]: checked, all: newAgreements.age && newAgreements.service && newAgreements.privacy && newAgreements.marketing && newAgreements.thirdParty });
        // Note: 'all' logic can be adjusted based on strict requirements (e.g. only essential ones)
    };

    // Phone Verification
    const onSendOtp = async () => {
        const phone = watch("phone");
        if (!phone || phone.length < 10) {
            toast({ title: "휴대폰 번호를 올바르게 입력해주세요.", variant: "destructive" });
            return;
        }

        try {
            const res = await requestPhoneVerification(phone);
            if (res.error) {
                toast({ title: res.error, variant: "destructive" });
            } else {
                toast({ title: "인증번호가 발송되었습니다.", description: "문자를 확인해주세요." });
                setOtpSent(true);
            }
        } catch (e) {
            toast({ title: "인증번호 발송 실패", variant: "destructive" });
        }
    };

    const onVerifyOtp = async () => {
        const phone = watch("phone");
        const code = watch("otp");
        if (!code) return;

        setVerifying(true);
        try {
            const res = await verifyPhone(phone, code);
            if (res.error) {
                toast({ title: res.error, variant: "destructive" });
            } else {
                // Check if verification was successful based on legacy response structure
                if (res.data && res.data.verified) {
                    toast({ title: "휴대폰 인증이 완료되었습니다.", className: "bg-green-500 text-white" });
                    setPhoneVerified(true);
                } else {
                    toast({ title: "인증번호가 일치하지 않습니다.", variant: "destructive" });
                }
            }
        } catch (e) {
            toast({ title: "인증 오류", variant: "destructive" });
        } finally {
            setVerifying(false);
        }
    };

    const onSubmit = async (data: any) => {
        if (!agreements.age || !agreements.service || !agreements.privacy) { // Minimum required from legacy
            toast({ title: "필수 약관에 동의해주세요.", variant: "destructive" });
            return;
        }

        if (!phoneVerified) {
            toast({ title: "휴대폰 인증을 완료해주세요.", variant: "destructive" });
            return;
        }

        const payload = {
            loginId: data.userid,
            password: data.password,
            name: data.username,
            email: data.email,
            phone: data.phone,
            gender: data.gender,
            birthday: data.birthday,
            role: "CANDIDATE" // Job Seeker
        };

        const res = await signUp(payload);

        if (res.error) {
            toast({ title: "회원가입 실패", description: res.error, variant: "destructive" });
        } else {
            toast({ title: "회원가입 성공!", description: "로그인 페이지로 이동합니다." });
            navigate("/login");
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Header />

            <main className="flex-1 container max-w-2xl mx-auto py-12 px-4">
                <div className="text-center mb-10">
                    <h1 className="text-3xl font-bold text-[#111] mb-2">개인 회원가입</h1>
                    <p className="text-gray-500">FitMe와 함께 커리어를 성장시켜보세요.</p>
                </div>

                <form onSubmit={handleSubmit(onSubmit)} className="space-y-8 bg-white p-8 rounded-2xl shadow-sm border border-gray-100">

                    {/* Terms Section */}
                    <section className="space-y-4">
                        <h2 className="text-lg font-bold text-[#111] flex items-center gap-1">
                            약관 동의 <span className="text-red-500">*</span>
                        </h2>

                        <div className="border rounded-xl p-5 space-y-4 bg-gray-50/50">
                            <div className="flex items-center space-x-3 pb-4 border-b">
                                <Checkbox
                                    id="all"
                                    checked={agreements.all}
                                    onCheckedChange={(c) => handleAllCheck(!!c)}
                                    className="w-5 h-5 border-gray-300 data-[state=checked]:bg-[#5A639C] data-[state=checked]:border-[#5A639C]"
                                />
                                <Label htmlFor="all" className="text-base font-bold cursor-pointer">
                                    전체동의
                                </Label>
                                <span className="text-xs text-gray-400 font-normal ml-auto hidden sm:inline-block">선택항목 포함 모든 약관에 동의합니다.</span>
                            </div>

                            <div className="space-y-3 pl-1">
                                {[
                                    { key: "age", label: "(필수) 만 15세 이상입니다.", required: true },
                                    { key: "service", label: "(필수) 서비스 이용약관 동의", required: true, hasMore: true },
                                    { key: "privacy", label: "(필수) 개인정보 수집 및 이용 동의", required: true, hasMore: true },
                                    { key: "marketing", label: "(선택) 마케팅 정보 수신 동의", required: false, hasMore: true },
                                    { key: "thirdParty", label: "(선택) 위치기반 서비스 이용약관 동의", required: false, hasMore: true },
                                ].map((item) => (
                                    <div key={item.key} className="flex items-center justify-between">
                                        <div className="flex items-center space-x-3">
                                            <Checkbox
                                                id={item.key}
                                                checked={agreements[item.key as keyof typeof agreements]}
                                                onCheckedChange={(c) => handleSingleCheck(item.key as keyof typeof agreements, !!c)}
                                                className="w-5 h-5 border-gray-300 text-white data-[state=checked]:bg-[#5A639C] data-[state=checked]:border-[#5A639C]"
                                            />
                                            <Label htmlFor={item.key} className={`cursor-pointer ${item.key === 'age' || item.key === 'service' || item.key === 'privacy' ? 'text-gray-900' : 'text-gray-600'}`}>
                                                {item.label}
                                            </Label>
                                        </div>
                                        {item.hasMore && <ChevronRight className="h-4 w-4 text-gray-300 cursor-pointer hover:text-gray-500" />}
                                    </div>
                                ))}
                            </div>
                        </div>
                    </section>

                    {/* Phone Verification */}
                    <section className="space-y-4 pt-4">
                        <h2 className="text-lg font-bold text-[#111] flex items-center gap-1">
                            휴대폰번호 <span className="text-red-500">*</span>
                        </h2>
                        <div className="space-y-3">
                            <div className="flex gap-2">
                                <Input
                                    {...register("phone", { required: true })}
                                    placeholder="휴대폰 번호 '-'제외하고 입력"
                                    className="h-12 bg-white"
                                    readOnly={phoneVerified}
                                />
                                <Button
                                    type="button"
                                    variant="secondary"
                                    className="h-12 w-24 bg-gray-200 text-gray-600 hover:bg-gray-300 font-bold"
                                    onClick={onSendOtp}
                                    disabled={phoneVerified}
                                >
                                    인증번호
                                </Button>
                            </div>
                            <div className="flex gap-2">
                                <Input
                                    {...register("otp")}
                                    placeholder="인증번호 입력"
                                    className="h-12 bg-white"
                                    readOnly={phoneVerified || !otpSent}
                                />
                                <Button
                                    type="button"
                                    variant="secondary"
                                    className="h-12 w-24 bg-gray-200 text-gray-600 hover:bg-gray-300 font-bold"
                                    onClick={onVerifyOtp}
                                    disabled={phoneVerified || !otpSent || verifying}
                                >
                                    {verifying ? "확인중" : "확인"}
                                </Button>
                            </div>
                        </div>
                    </section>

                    {/* User Info Fields */}
                    <section className="space-y-6 pt-4">
                        <div className="space-y-2">
                            <Label className="text-base font-bold flex gap-1">아이디 <span className="text-red-500">*</span></Label>
                            <Input
                                {...register("userid", { required: true, minLength: 4 })}
                                className="h-12 bg-gray-50 border-gray-200"
                                placeholder="아이디 입력"
                            />
                        </div>

                        <div className="space-y-2">
                            <Label className="text-base font-bold flex gap-1 items-center">
                                비밀번호 <span className="text-red-500">*</span>
                            </Label>
                            <Input
                                type="password"
                                {...register("password", { required: true, minLength: 8 })}
                                className="h-12 bg-gray-50 border-gray-200"
                                placeholder="영문, 숫자, 특수문자 포함 8자 이상"
                            />
                        </div>

                        <div className="space-y-2">
                            <Label className="text-base font-bold flex gap-1">이름 <span className="text-red-500">*</span></Label>
                            <Input
                                {...register("username", { required: true })}
                                className="h-12 bg-gray-50 border-gray-200"
                                placeholder="실명 입력"
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label className="text-base font-bold flex gap-1">생년월일 <span className="text-red-500">*</span></Label>
                                <Input
                                    type="date"
                                    {...register("birthday", { required: true })}
                                    className="h-12 bg-gray-50 border-gray-200"
                                />
                            </div>
                            <div className="space-y-2">
                                <Label className="text-base font-bold flex gap-1">성별 <span className="text-red-500">*</span></Label>
                                <Controller
                                    control={control}
                                    name="gender"
                                    rules={{ required: true }}
                                    render={({ field }) => (
                                        <Select onValueChange={field.onChange} value={field.value}>
                                            <SelectTrigger className="h-12 bg-gray-50 border-gray-200">
                                                <SelectValue placeholder="선택" />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="MALE">남성</SelectItem>
                                                <SelectItem value="FEMALE">여성</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    )}
                                />
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label className="text-base font-bold flex gap-1">이메일 <span className="text-red-500">*</span></Label>
                            <Input
                                type="email"
                                {...register("email", { required: true })}
                                className="h-12 bg-gray-50 border-gray-200"
                                placeholder="example@email.com"
                            />
                        </div>

                        <div className="space-y-2">
                            <Label className="text-base font-bold">가입경로</Label>
                            <Controller
                                control={control}
                                name="referralSource"
                                render={({ field }) => (
                                    <Select onValueChange={field.onChange} value={field.value}>
                                        <SelectTrigger className="h-12 bg-gray-50 border-gray-200 text-gray-500">
                                            <SelectValue placeholder="가입경로 선택" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="search">검색</SelectItem>
                                            <SelectItem value="ad">광고</SelectItem>
                                            <SelectItem value="friend">지인 추천</SelectItem>
                                            <SelectItem value="etc">기타</SelectItem>
                                        </SelectContent>
                                    </Select>
                                )}
                            />
                        </div>
                    </section>

                    <Button type="submit" disabled={isSubmitting} className="w-full h-14 text-lg font-bold bg-[#cdcdcd] hover:bg-[#5A639C] text-white rounded-xl mt-8 transition-colors">
                        {isSubmitting ? "가입 처리중..." : "가입하기"}
                    </Button>

                </form>
            </main>

            <Footer />
        </div>
    );
}
