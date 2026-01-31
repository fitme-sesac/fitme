import { useState, useEffect } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

interface TermsItem {
    id: string;
    label: string;
    required: boolean;
    subtext?: string | null;
}

interface TermsAgreementProps {
    type?: "jobSeeker" | "company";
    onComplete?: (agreed: boolean) => void;
    onChange?: (agreements: Record<string, boolean>) => void;
}

export function TermsAgreement({ type = "jobSeeker", onComplete, onChange }: TermsAgreementProps) {
    const TERMS_CONFIG = {
        jobSeeker: [
            { id: "age", label: "만 15세 이상입니다", required: true, subtext: null },
            { id: "service", label: "이용약관 동의", required: true, subtext: null },
            { id: "privacy", label: "개인정보 수집 및 이용 동의", required: true, subtext: null },
            { id: "policy", label: "개인정보 처리방침/정책 동의", required: true, subtext: null },
            { id: "marketing", label: "마케팅 정보 수신 동의", required: false, subtext: "(선택)" },
        ],
        company: [
            { id: "service", label: "기업 서비스 이용약관 동의", required: true, subtext: null },
            { id: "bizInfo", label: "기업 정보 수집 및 이용 동의", required: true, subtext: null },
            { id: "policy", label: "개인정보 처리방침/정책 동의", required: true, subtext: null },
            { id: "sms", label: "SMS/알림 수신 동의", required: true, subtext: null },
            { id: "marketing", label: "마케팅 정보 수신 동의", required: false, subtext: "(선택)" },
        ],
    };

    const termsList: TermsItem[] = TERMS_CONFIG[type];

    const [agreements, setAgreements] = useState<Record<string, boolean>>(() => {
        const init: Record<string, boolean> = {};
        termsList.forEach((t) => (init[t.id] = false));
        return init;
    });

    const allRequiredAgreed = () => {
        return termsList.filter((t) => t.required).every((t) => agreements[t.id]);
    };

    const toggleAll = (checked: boolean) => {
        const next: Record<string, boolean> = {};
        termsList.forEach((t) => (next[t.id] = checked));
        setAgreements(next);
    };

    const toggleOne = (id: string, checked: boolean) => {
        setAgreements((prev) => ({ ...prev, [id]: checked }));
    };

    useEffect(() => {
        onComplete?.(allRequiredAgreed());
        onChange?.(agreements);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [agreements]);

    const allChecked = termsList.every((t) => agreements[t.id]);

    return (
        <div className="space-y-4">
            <div className="flex items-center gap-2 rounded-lg border border-border p-3 bg-muted/30">
                <Checkbox id="all" checked={allChecked} onCheckedChange={(v) => toggleAll(Boolean(v))} />
                <Label htmlFor="all" className="font-semibold cursor-pointer">
                    전체 동의
                </Label>
            </div>

            <div className="space-y-3">
                {termsList.map((t) => (
                    <div key={t.id} className={cn("flex items-start gap-2 rounded-lg p-2", t.required ? "" : "opacity-90")}>
                        <Checkbox id={t.id} checked={agreements[t.id]} onCheckedChange={(v) => toggleOne(t.id, Boolean(v))} />
                        <Label htmlFor={t.id} className="cursor-pointer leading-5">
              <span className={cn(t.required ? "font-medium" : "")}>
                {t.label}{" "}
                  {t.required ? <span className="text-destructive">(필수)</span> : <span className="text-muted-foreground">(선택)</span>}
              </span>
                            {t.subtext && <div className="text-xs text-muted-foreground mt-1">{t.subtext}</div>}
                        </Label>
                    </div>
                ))}
            </div>
        </div>
    );
}
