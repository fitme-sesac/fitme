export interface SubscriptionPlan {
    id: string;
    name: string;
    description: string;
    price: number;
    billingCycle: "monthly" | "yearly";
    features: string[];
    highlightedFeatures?: string[];
    limitations?: string[];
    isPopular?: boolean;
    ctaText: string;
}

export const subscriptionPlans: SubscriptionPlan[] = [
    {
        id: "free",
        name: "Free",
        description: "소규모 팀을 위한 기본 플랜",
        price: 0,
        billingCycle: "monthly",
        features: [
            "월 5명 인재 프로필 열람",
            "기본 검색 필터",
            "이메일 지원",
        ],
        limitations: [
            "연락처 열람 불가",
            "인재 스크랩 불가",
        ],
        ctaText: "무료로 시작하기",
    },
    {
        id: "pro",
        name: "Pro",
        description: "성장하는 기업을 위한 추천 플랜",
        price: 99000,
        billingCycle: "monthly",
        features: [
            "월 50명 인재 프로필 열람",
            "고급 검색 필터 (기술스택, 경력, 지역)",
            "인재 연락처 열람",
            "인재 스크랩 (최대 100명)",
            "면접 제안 발송",
            "우선 이메일 지원",
        ],
        highlightedFeatures: [
            "인재 연락처 열람",
            "면접 제안 발송",
        ],
        isPopular: true,
        ctaText: "Pro 시작하기",
    },
    {
        id: "enterprise",
        name: "Enterprise",
        description: "대규모 채용을 위한 맞춤 플랜",
        price: 299000,
        billingCycle: "monthly",
        features: [
            "무제한 인재 프로필 열람",
            "모든 검색 필터 사용",
            "인재 연락처 무제한 열람",
            "인재 스크랩 무제한",
            "면접 제안 무제한 발송",
            "전담 매니저 배정",
            "채용 분석 리포트",
            "API 연동 지원",
            "24시간 전화 지원",
        ],
        highlightedFeatures: [
            "전담 매니저 배정",
            "채용 분석 리포트",
        ],
        ctaText: "영업팀 문의",
    },
];

export const planComparison = [
    {
        feature: "인재 프로필 열람",
        free: "월 5명",
        pro: "월 50명",
        enterprise: "무제한",
    },
    {
        feature: "검색 필터",
        free: "기본",
        pro: "고급",
        enterprise: "전체",
    },
    {
        feature: "연락처 열람",
        free: false,
        pro: true,
        enterprise: true,
    },
    {
        feature: "인재 스크랩",
        free: false,
        pro: "100명",
        enterprise: "무제한",
    },
    {
        feature: "면접 제안",
        free: false,
        pro: true,
        enterprise: true,
    },
    {
        feature: "채용 분석 리포트",
        free: false,
        pro: false,
        enterprise: true,
    },
    {
        feature: "전담 매니저",
        free: false,
        pro: false,
        enterprise: true,
    },
    {
        feature: "API 연동",
        free: false,
        pro: false,
        enterprise: true,
    },
];
