import { Megaphone, X, ExternalLink } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";

// Premium Recruitment Ads (Ads subscribed by companies)
const PREMIUM_ADS = [
    {
        id: 1,
        company: "네이버클라우드",
        title: "대규모 경력직 공개 채용",
        description: "최고의 동료와 함께 성장할 기회! 전 직군 대규모 채용을 시작합니다.",
        link: "/jobs/1",
        bgColor: "from-green-500/10 via-emerald-500/10 to-teal-500/10",
        borderColor: "border-green-500/20",
        iconColor: "text-green-600",
    },
    {
        id: 2,
        company: "테크이노베이트",
        title: "NEXT 개발자를 찾습니다",
        description: "기술의 미래를 바꿀 당신을 기다립니다. 최고 대우 보장!",
        link: "/jobs/2",
        bgColor: "from-blue-500/10 via-indigo-500/10 to-violet-500/10",
        borderColor: "border-blue-500/20",
        iconColor: "text-blue-600",
    },
];

export function PremiumAdBanner() {
    const [isVisible, setIsVisible] = useState(true);
    // Pick a random ad for demo purposes
    const [ad] = useState(PREMIUM_ADS[Math.floor(Math.random() * PREMIUM_ADS.length)]);

    if (!isVisible) return null;

    return (
        <div className={`relative mb-8 rounded-xl bg-gradient-to-r ${ad.bgColor} border ${ad.borderColor} p-4 md:p-5 shadow-sm`}>
            <button
                onClick={() => setIsVisible(false)}
                className="absolute right-2 top-2 p-1.5 rounded-full hover:bg-black/5 transition-colors"
                aria-label="광고 닫기"
            >
                <X className="h-4 w-4 text-muted-foreground" />
            </button>

            <div className="flex flex-col md:flex-row items-start md:items-center gap-4">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-white shadow-sm border border-border/50">
                    <Megaphone className={`h-6 w-6 ${ad.iconColor}`} />
                </div>

                <div className="flex-1 pr-8">
                    <div className="flex items-center gap-2 mb-1">
                        <span className="text-xs font-bold px-2 py-0.5 rounded bg-white border border-border/50 text-foreground">AD</span>
                        <span className="text-sm font-semibold text-foreground">{ad.company}</span>
                    </div>
                    <h3 className="text-lg font-bold text-foreground mb-1">
                        {ad.title}
                    </h3>
                    <p className="text-sm text-foreground/80">
                        {ad.description}
                    </p>
                </div>

                <Button className={`shrink-0 mt-3 md:mt-0 w-full md:w-auto bg-white hover:bg-white/90 text-foreground border border-border shadow-sm`} asChild>
                    <a href={ad.link} target="_blank" rel="noopener noreferrer">
                        자세히 보기 <ExternalLink className="ml-2 h-4 w-4 opacity-50" />
                    </a>
                </Button>
            </div>
        </div>
    );
}
