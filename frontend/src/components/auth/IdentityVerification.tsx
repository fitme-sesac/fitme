import { useState } from "react";
import { Button } from "@/components/ui/button";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { ShieldCheck, Smartphone, User } from "lucide-react";

interface IdentityVerificationProps {
    onComplete: (isVerified: boolean) => void;
}

export function IdentityVerification({ onComplete }: IdentityVerificationProps) {
    const [method, setMethod] = useState<"phone" | "ipin">("phone");
    const [isVerified, setIsVerified] = useState(false);

    const handleVerify = () => {
        // Mock Verification Logic
        // 실제로는 팝업 오픈 -> 본인인증 진행 -> 콜백 수신
        const width = 500;
        const height = 600;
        const left = window.screenX + (window.outerWidth - width) / 2;
        const top = window.screenY + (window.outerHeight - height) / 2;

        // 모의 팝업 (실제 구현 시 PG/인증사 연동)
        alert("본인인증 모의 테스트: 인증이 완료되었습니다.");
        setIsVerified(true);
        onComplete(true);
    };

    return (
        <div className="space-y-6">
            <RadioGroup value={method} onValueChange={(v: "phone" | "ipin") => setMethod(v)} className="grid grid-cols-2 gap-4">
                <div>
                    <RadioGroupItem value="phone" id="phone" className="peer sr-only" />
                    <Label
                        htmlFor="phone"
                        className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-popover p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary peer-data-[state=checked]:text-primary cursor-pointer transition-all"
                    >
                        <Smartphone className="mb-3 h-6 w-6" />
                        <span className="font-semibold text-sm">휴대폰 인증</span>
                    </Label>
                </div>
                <div>
                    <RadioGroupItem value="ipin" id="ipin" className="peer sr-only" />
                    <Label
                        htmlFor="ipin"
                        className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-popover p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary peer-data-[state=checked]:text-primary cursor-pointer transition-all"
                    >
                        <User className="mb-3 h-6 w-6" />
                        <span className="font-semibold text-sm">아이핀 인증</span>
                    </Label>
                </div>
            </RadioGroup>

            <div className="bg-secondary/20 p-4 rounded-lg text-xs text-muted-foreground space-y-1">
                <p>• 입력하신 정보는 본인확인 및 중복가입 방지를 위해 사용됩니다.</p>
                <p>• 타인의 개인정보를 도용하여 가입할 경우 관계 법령에 따라 처벌받을 수 있습니다.</p>
            </div>

            {!isVerified ? (
                <Button onClick={handleVerify} className="w-full h-12 text-base font-bold bg-muted text-muted-foreground hover:bg-muted/80">
                    <ShieldCheck className="mr-2 h-4 w-4" />
                    인증하기
                </Button>
            ) : (
                <div className="h-12 w-full rounded-md bg-green-500/10 text-green-500 flex items-center justify-center font-bold border border-green-500/20">
                    <ShieldCheck className="mr-2 h-5 w-5" />
                    본인인증 완료
                </div>
            )}
        </div>
    );
}
