import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { ChevronRight } from "lucide-react";

interface TermsAgreementProps {
    onComplete: (isCompleted: boolean) => void;
}

export function TermsAgreement({ onComplete }: TermsAgreementProps) {
    const [agreements, setAgreements] = useState({
        all: false,
        terms: false, // 이용약관 (필수)
        privacy: false, // 개인정보 (필수)
        marketing: false, // 마케팅 (선택)
    });

    const handleAllCheck = (checked: boolean) => {
        setAgreements({
            all: checked,
            terms: checked,
            privacy: checked,
            marketing: checked,
        });
    };

    const handleSingleCheck = (key: keyof typeof agreements) => (checked: boolean) => {
        const newAgreements = { ...agreements, [key]: checked };
        const allChecked =
            newAgreements.terms && newAgreements.privacy && newAgreements.marketing;
        setAgreements({ ...newAgreements, all: allChecked });
    };

    useEffect(() => {
        // 필수 약관(terms, privacy)이 모두 체크되었는지 부모에게 알림
        onComplete(agreements.terms && agreements.privacy);
    }, [agreements, onComplete]);

    return (
        <div className="space-y-6">
            <div className="rounded-xl border border-primary/20 bg-primary/5 p-4">
                <div className="flex items-center space-x-2">
                    <Checkbox
                        id="all"
                        checked={agreements.all}
                        onCheckedChange={handleAllCheck}
                        className="h-5 w-5 border-primary data-[state=checked]:bg-primary"
                    />
                    <Label htmlFor="all" className="text-base font-bold cursor-pointer">
                        약관 전체 동의
                    </Label>
                </div>
            </div>

            <div className="space-y-4 px-2">
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                        <Checkbox
                            id="terms"
                            checked={agreements.terms}
                            onCheckedChange={handleSingleCheck("terms")}
                        />
                        <Label htmlFor="terms" className="text-sm cursor-pointer">
                            <span className="text-primary font-bold">(필수)</span> 서비스 이용약관 동의
                        </Label>
                    </div>
                    <Button variant="ghost" size="sm" className="h-6 px-2 text-xs text-muted-foreground">
                        내용보기 <ChevronRight className="h-3 w-3" />
                    </Button>
                </div>

                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                        <Checkbox
                            id="privacy"
                            checked={agreements.privacy}
                            onCheckedChange={handleSingleCheck("privacy")}
                        />
                        <Label htmlFor="privacy" className="text-sm cursor-pointer">
                            <span className="text-primary font-bold">(필수)</span> 개인정보 수집 및 이용 동의
                        </Label>
                    </div>
                    <Button variant="ghost" size="sm" className="h-6 px-2 text-xs text-muted-foreground">
                        내용보기 <ChevronRight className="h-3 w-3" />
                    </Button>
                </div>

                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                        <Checkbox
                            id="marketing"
                            checked={agreements.marketing}
                            onCheckedChange={handleSingleCheck("marketing")}
                        />
                        <Label htmlFor="marketing" className="text-sm cursor-pointer">
                            (선택) 마케팅 정보 수신 동의
                        </Label>
                    </div>
                    <Button variant="ghost" size="sm" className="h-6 px-2 text-xs text-muted-foreground">
                        내용보기 <ChevronRight className="h-3 w-3" />
                    </Button>
                </div>
            </div>
        </div>
    );
}
