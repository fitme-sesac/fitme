import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { Loader2, CheckCircle2, User, Mail, Smartphone } from "lucide-react";
import { z } from "zod";
import { TermsAgreement } from "./TermsAgreement";
import { IdentityVerification } from "./IdentityVerification";

const formSchema = z.object({
    userid: z.string().min(4, "아이디는 4자 이상이어야 합니다").regex(/^[a-z0-9]+$/, "영문 소문자와 숫자만 가능합니다"),
    password: z.string().min(6, "비밀번호는 6자 이상이어야 합니다"),
    confirmPassword: z.string(),
    name: z.string().min(2, "이름을 입력해주세요"),
    phone: z.string().regex(/^01([0|1|6|7|8|9])-?([0-9]{3,4})-?([0-9]{4})$/, "올바른 휴대폰 번호를 입력해주세요"),
    email: z.string().email("유효한 이메일 주소를 입력해주세요"),
}).refine((data) => data.password === data.confirmPassword, {
    message: "비밀번호가 일치하지 않습니다",
    path: ["confirmPassword"],
});

export function JobSeekerSignUpForm() {
    const [formData, setFormData] = useState({
        userid: "",
        password: "",
        confirmPassword: "",
        name: "",
        phone: "",
        email: "",
    });

    // Steps State
    const [termsAgreed, setTermsAgreed] = useState(false);
    const [identityVerified, setIdentityVerified] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const { signUp } = useAuth();
    const { toast } = useToast();

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { id, value } = e.target;
        setFormData(prev => ({ ...prev, [id]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!termsAgreed) {
            toast({ variant: "destructive", title: "약관 동의 필요", description: "필수 약관에 동의해주세요." });
            document.getElementById("section-terms-seeker")?.scrollIntoView({ behavior: "smooth" });
            return;
        }
        if (!identityVerified) {
            toast({ variant: "destructive", title: "본인 인증 필요", description: "본인 인증을 완료해주세요." });
            document.getElementById("section-verify-seeker")?.scrollIntoView({ behavior: "smooth" });
            return;
        }

        // Validate
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
            loginId: formData.userid,
            userid: formData.userid,
            password: formData.password,
            name: formData.name,
            phone: formData.phone,
            email: formData.email,
            role: 'CANDIDATE'
        };

        const { error } = await signUp(userData);

        if (error) {
            toast({
                variant: "destructive",
                title: "회원가입 실패",
                description: typeof error === 'string' ? error : "회원가입 중 오류가 발생했습니다",
            });
        } else {
            toast({
                title: "회원가입 성공",
                description: "환영합니다! 구직자 회원으로 가입되었습니다.",
            });
        }

        setIsLoading(false);
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">

            {/* 1. 약관 동의 */}
            <section id="section-terms-seeker" className="space-y-4">
                <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">1</span>
                    약관 동의
                </h3>
                <TermsAgreement onComplete={setTermsAgreed} />
            </section>

            {/* 2. 본인 인증 */}
            <section id="section-verify-seeker" className="space-y-4">
                <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">2</span>
                    본인 인증
                    {identityVerified && <CheckCircle2 className="h-5 w-5 text-green-500 ml-auto" />}
                </h3>
                <IdentityVerification onComplete={setIdentityVerified} />
            </section>

            {/* 3. 계정 정보 */}
            <section className="space-y-4">
                <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">3</span>
                    계정 정보
                </h3>
                <div className="space-y-4 p-4 bg-card rounded-xl border border-border">
                    <div className="space-y-2">
                        <Label htmlFor="userid">아이디</Label>
                        <Input id="userid" placeholder="아이디 (4자 이상)" value={formData.userid} onChange={handleChange} required />
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="password">비밀번호</Label>
                            <Input id="password" type="password" placeholder="비밀번호 (6자 이상)" value={formData.password} onChange={handleChange} required />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="confirmPassword">비밀번호 확인</Label>
                            <Input id="confirmPassword" type="password" placeholder="비밀번호 재입력" value={formData.confirmPassword} onChange={handleChange} required />
                        </div>
                    </div>
                </div>
            </section>

            {/* 4. 개인 정보 */}
            <section className="space-y-4">
                <h3 className="text-lg font-bold flex items-center gap-2 border-b pb-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs">4</span>
                    개인 정보
                </h3>
                <div className="space-y-4 p-4 bg-card rounded-xl border border-border">
                    <div className="space-y-2">
                        <Label htmlFor="name" className="flex items-center gap-2">
                            <User className="h-4 w-4" /> 이름
                        </Label>
                        <Input id="name" placeholder="실명을 입력해주세요" value={formData.name} onChange={handleChange} required />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="phone" className="flex items-center gap-2">
                            <Smartphone className="h-4 w-4" /> 휴대폰 번호
                        </Label>
                        <Input id="phone" placeholder="010-0000-0000" value={formData.phone} onChange={handleChange} required />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="email" className="flex items-center gap-2">
                            <Mail className="h-4 w-4" /> 이메일
                        </Label>
                        <Input id="email" type="email" placeholder="example@email.com" value={formData.email} onChange={handleChange} required />
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
