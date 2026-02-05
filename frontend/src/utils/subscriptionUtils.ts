
export interface PlanDetails {
    name: string;
    description: string;
    benefits: string[];
    highlighted: string[];
    isPopular: boolean;
}

export const getPlanDetails = (productCode: string, creditAmount: number): PlanDetails => {
    let name = "";
    let description = "";
    let benefits: string[] = [];
    let highlighted: string[] = [];
    let isPopular = false;

    if (productCode.includes("BASIC")) {
        name = "Basic Plan";
        description = "소규모 팀을 위한 기본 플랜";
        benefits = [
            `월 ${creditAmount.toLocaleString()} 크레딧 제공`,
            "기본 검색 필터",
            "이메일 지원"
        ];
    } else if (productCode.includes("STANDARD")) {
        name = "Standard Plan";
        description = "성장하는 기업을 위한 표준 플랜";
        isPopular = true;
        benefits = [
            `월 ${creditAmount.toLocaleString()} 크레딧 제공`,
            "고급 검색 필터",
            "인재 연락처 열람",
            "우선 이메일 지원"
        ];
        highlighted = ["인재 연락처 열람"];
    } else if (productCode.includes("PRO")) {
        name = "Pro Plan";
        description = "대규모 채용을 위한 프로 플랜";
        benefits = [
            `월 ${creditAmount.toLocaleString()} 크레딧 제공`,
            "모든 검색 필터 사용",
            "인재 연락처 무제한 열람",
            "전담 매니저 배정",
            "24시간 전화 지원"
        ];
        highlighted = ["전담 매니저 배정", "24시간 전화 지원"];
    } else if (productCode.startsWith("OT_CREDIT")) {
        // Legacy or One-Time product display support
        name = "Monthly Credit";
        description = "월간 크레딧 자동 충전";
        const bonusMatch = productCode.match(/(\d+) 충전/); // Extract if possible, but creditAmount is reliable
        benefits = [`월 ${creditAmount.toLocaleString()} 크레딧 제공`];
    } else {
        // Fallback for unknown plans
        name = productCode;
        description = "기업용 구독 플랜";
        benefits = [`월 ${creditAmount.toLocaleString()} 크레딧 제공`];
    }

    return { name, description, benefits, highlighted, isPopular };
};
