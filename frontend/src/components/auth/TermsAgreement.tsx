import { useState, useEffect } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { ChevronRight, ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface TermsAgreementProps {
    onComplete: (isCompleted: boolean) => void;
    onChange?: (agreements: Record<string, boolean>) => void;
    type?: "jobSeeker" | "company";
}

export function TermsAgreement({ onComplete, onChange, type = "jobSeeker" }: TermsAgreementProps) {
    // Defines terms for each type
    const TERMS_CONFIG = {
        jobSeeker: [
            { id: "age", label: "만 15세 이상입니다", required: true, subtext: null },
            { id: "service", label: "이용약관 동의", required: true, subtext: null },
            // Divider handled by rendering logic
            { id: "privacy", label: "개인정보 수집 및 이용 동의", required: false, subtext: null },
            { id: "marketing", label: "광고성 정보 수신 동의", required: false, subtext: null },
        ],
        company: [
            { id: "service", label: "서비스 이용약관 동의", required: true, subtext: null },
            { id: "sms", label: "문자서비스 이용약관 동의", required: true, subtext: null },
            { id: "bizInfo", label: "사업자등록정보 수집 및 이용 동의", required: true, subtext: null },
            // Divider
            { id: "privacy_recommend", label: "개인정보수집 및 이용 동의-인재추천·혜택", required: false, subtext: "인재추천 및 다양한 혜택 알림을 받으려면 동의가 필요해요" },
            { id: "marketing", label: "광고성 정보 수신 동의", required: false, subtext: null },
            { id: "payroll_service", label: "근태·급여관리 서비스 이용약관 동의", required: false, subtext: "직원의 근로계약서, 급여명세서를 작성 하려면 서비스 동의가 필요해요" },
            { id: "payroll_privacy", label: "근태·급여관리 서비스 개인정보수집 및 이용 동의", required: false, subtext: null },
        ]
    };

    const currentTerms = TERMS_CONFIG[type];

    // Header Text Logic
    // Unified Header Text for both Job Seeker and Company
    const headerText = "필수동의 항목 및 개인정보 수집 및 이용 동의(선택), 광고성 정보 수신(선택)에 모두 동의합니다.";
    const headerSubText = null;

    // Initialize state dynamically
    const [agreements, setAgreements] = useState<Record<string, boolean>>(() => {
        const initialStates: Record<string, boolean> = {};
        currentTerms.forEach(term => {
            initialStates[term.id] = false;
        });
        return initialStates;
    });

    useEffect(() => {
        setAgreements(prev => {
            const next: Record<string, boolean> = {};
            currentTerms.forEach(term => {
                next[term.id] = prev[term.id] || false;
            });
            return next;
        });
    }, [type]);

    const allChecked = currentTerms.every(term => agreements[term.id]);

    const handleAllCheck = (checked: boolean) => {
        const newAgreements = { ...agreements };
        currentTerms.forEach(term => {
            newAgreements[term.id] = checked;
        });
        setAgreements(newAgreements);
    };

    const handleSingleCheck = (id: string) => (checked: boolean) => {
        setAgreements(prev => ({ ...prev, [id]: checked }));
    };

    useEffect(() => {
        const requiredIds = currentTerms.filter(t => t.required).map(t => t.id);
        const isComplete = requiredIds.every(id => agreements[id]);

        onComplete(isComplete);
        if (onChange) onChange(agreements);
    }, [agreements, onComplete, onChange, type]);

    const TermItem = ({
        id,
        label,
        required = false,
        subtext = null
    }: { id: string, label: string, required?: boolean, subtext?: string | null }) => {

        // Dynamic Label Styling based on Type
        // JobSeeker: [필수] (Blue), [선택] (Gray)
        // Company: (필수) (Red/Primary or Black?), (선택) (Gray) -> Photo shows standard text style, maybe colored req?
        // Let's look at Company Photo (Step 625/684):
        // (필수) is Black/Bold? Actually looks like (필수) is same color as text, maybe slightly bolder.
        // Wait, standard UI pattern: Required is usually colored.
        // But requested specifically "Like photo". 
        // JobSeeker photo explicitly highlights [필수] in Blue.
        // Company photo seems plain.
        // I will implement based on "Like photo".

        // Unified Styling: Parentheses for all, Primary (Lavender) for Required
        const prefix = required ? "(필수)" : "(선택)";

        // Updated for Lavender Palette
        const prefixClass = required
            ? "text-primary font-bold mr-1"
            : "text-gray-400 font-bold mr-1";

        const ArrowIcon = type === "jobSeeker" ? ChevronDown : ChevronRight;
        const arrowText = type === "jobSeeker" ? "내용보기" : "";

        return (
            <div className="py-2 flex flex-col justify-center group">
                <div className="flex items-center justify-between w-full">
                    <div className="flex items-center space-x-2 flex-1">
                        <Checkbox
                            id={id}
                            checked={agreements[id] || false}
                            onCheckedChange={handleSingleCheck(id)}
                            className="h-5 w-5 border-gray-300 data-[state=checked]:bg-primary data-[state=checked]:border-primary mt-0.5"
                        />
                        <Label htmlFor={id} className="text-sm cursor-pointer font-normal text-gray-700 select-none flex-1 flex items-center">
                            <span className={prefixClass}>
                                {prefix}
                            </span>
                            {label}
                        </Label>
                    </div>

                    {/* Arrow/Link Section */}
                    <div className="flex items-center text-xs text-gray-400 cursor-pointer whitespace-nowrap ml-2 hover:text-gray-600">
                        {arrowText} <ArrowIcon className="h-3 w-3 ml-1" />
                    </div>
                </div>

                {/* Subtext */}
                {subtext && (
                    <p className="text-xs text-slate-400 pl-7 mt-0.5 whitespace-pre-wrap">{subtext}</p>
                )}
            </div>
        );
    };

    return (
        <div className="border border-gray-200 rounded-lg p-6 bg-white">
            <div className="flex items-start space-x-3 mb-4 pb-4 border-b border-gray-100">
                <Checkbox
                    id="all"
                    checked={allChecked}
                    onCheckedChange={handleAllCheck}
                    className="h-6 w-6 border-gray-300 data-[state=checked]:bg-primary mt-1"
                />
                <div className="flex flex-col">
                    <Label htmlFor="all" className="text-base font-bold cursor-pointer text-gray-900 select-none leading-snug">
                        {headerText}
                    </Label>
                    {headerSubText && (
                        <span className="text-xs text-gray-500 mt-1">
                            {headerSubText}
                        </span>
                    )}
                </div>
            </div>

            <div className="space-y-1">
                {currentTerms.map((term, index) => {
                    // Spacer Logic: Add explicit divider if switching from Required to Optional
                    const isFirstOptional = !term.required && (index > 0 && currentTerms[index - 1].required);

                    return (
                        <div key={term.id}>
                            {isFirstOptional && (
                                <div className="my-3 border-t border-dashed border-gray-200" />
                            )}
                            <TermItem
                                id={term.id}
                                label={term.label}
                                required={term.required}
                                subtext={term.subtext}
                            />
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
