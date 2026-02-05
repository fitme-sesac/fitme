import { MapPin, Building2, Globe, Users, Bookmark, Heart, Sparkles, Coins } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";

interface JobCardProps {
    id: number;
    company: string;
    logo: string;
    title: string;
    location: string;
    salary: string;
    skills?: string[] | string | null;
    postedAt: string;
    isAd?: boolean;
    matchScore?: number;
    requiredExperience?: number;
    companySummary?: string;
    employmentType?: string;
    createdAt?: string;
    applyCount?: number;
    recruitmentCapacity?: number;
}

// Brand color mapping (reused)
const BRAND_COLORS: Record<string, string> = {
    "Toss": "#0064FF",
    "Danggeun": "#FF6F0F",
    "Line": "#06C755",
    "Woowa Bros": "#2AC1BC",
    "Naver": "#03C75A",
    "Kakao": "#FEE500",
    "Samsung": "#1428A0",
    "Hyundai": "#002C5F",
    "LG": "#A50034",
    "SK": "#EA002C",
    "Coupang": "#E41B23",
};

const DEFAULT_COLOR = "#000000";

function getBrandColor(company: string): string {
    const key = Object.keys(BRAND_COLORS).find(k => company.toLowerCase().includes(k.toLowerCase()));
    return key ? BRAND_COLORS[key] : DEFAULT_COLOR;
}

function formatPostedAt(createdAt: string | undefined, postedAt: string): string {
    if (createdAt) {
        const diff = Math.floor((Date.now() - new Date(createdAt).getTime()) / (1000 * 60 * 60 * 24));
        if (diff === 0) return "방금 전";
        if (diff === 1) return "1일 전";
        if (diff < 7) return `${diff}일 전`;
    }
    return postedAt;
}

export function WideJobCard({
    id,
    company,
    logo,
    title,
    location,
    salary,
    skills: skillsProp,
    postedAt,
    isAd = false,
    requiredExperience,
    companySummary,
    employmentType = "정규직",
    createdAt,
    matchScore: matchScoreProp,
    applyCount = 0,
    recruitmentCapacity = 0,
}: JobCardProps) {
    const navigate = useNavigate();
    const { isAuthenticated } = useAuth();
    const [interested, setInterested] = useState(false);
    const rawSkills = Array.isArray(skillsProp)
        ? skillsProp
        : skillsProp != null && typeof skillsProp === "string"
            ? [skillsProp]
            : [];

    // Ensure all comma-separated values are split into individual tags
    const skills = rawSkills.flatMap(s => typeof s === "string" ? s.split(",") : String(s).split(",")).map(s => s.trim()).filter(s => s.length > 0);

    const postedLabel = formatPostedAt(createdAt, postedAt);
    const brandColor = getBrandColor(company);
    const mainGradient = "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)";

    // For guests or when data is missing, show nothing instead of mock scores
    const displayMatchScore = matchScoreProp;

    const displayLocation = location.split(" ").slice(0, 2).join(" ");
    const experienceLabel = requiredExperience ? `경력 ${requiredExperience}년+` : "신입/경력";

    return (
        <div
            role="button"
            tabIndex={0}
            className={cn(
                "group relative w-full rounded-2xl bg-white border border-gray-100 p-6 shadow-sm transition-all duration-300 hover:shadow-lg hover:border-primary/20 cursor-pointer",
                isAd && "ring-1 ring-primary/20"
            )}
            onClick={() => navigate(`/jobs/${id}`)}
            onKeyDown={(e) => e.key === "Enter" && navigate(`/jobs/${id}`)}
        >
            {/* Gradient Accent Line Top */}
            {isAd && <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-[#5AB2FA] to-[#3DCEC9]" />}

            <div className="flex flex-col gap-6">
                {/* Top Section: Company Info */}
                <div className="flex items-start gap-4">
                    <div
                        className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg text-white text-lg font-bold shadow-sm"
                        style={{ backgroundColor: brandColor }}
                    >
                        {logo.length > 5 ? (
                            <img src={logo} alt={company} className="w-full h-full object-cover rounded-lg" />
                        ) : (
                            <span>{company.charAt(0)}</span>
                        )}
                    </div>

                    <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                            <h4 className="font-bold text-gray-900 text-base">{company}</h4>
                        </div>
                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500">
                            <span className="flex items-center gap-1">
                                <MapPin className="w-3.5 h-3.5" />
                                {displayLocation}
                            </span>
                        </div>
                    </div>
                </div>

                {/* Quote & Description - Only show if available */}
                {companySummary && (
                    <div className="space-y-1">
                        <p className="font-bold text-gray-800 text-sm">
                            "{companySummary.split(/[\.\n]/)[0]}"
                        </p>
                        <p className="text-sm text-gray-500 line-clamp-2 leading-relaxed">
                            {companySummary}
                        </p>
                    </div>
                )}

                <div className="h-px bg-gray-100 w-full" />

                {/* Bottom Section: Job Details */}
                <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
                    <div className="space-y-3 flex-1">
                        {/* Title: Always full width for consistency */}
                        <Link to={`/jobs/${id}`} className="group-hover:text-primary transition-colors block w-full">
                            <h3 className="text-xl font-bold text-gray-900">{title}</h3>
                        </Link>

                        {/* Metadata Row: AI Match, Experience, Status Badges */}
                        <div className="flex items-center flex-wrap gap-2 pt-1">
                            {/* New Prominent AI Match Rate Badge - Only when real data exists */}
                            {displayMatchScore !== undefined && displayMatchScore !== null && (
                                <div
                                    className="flex items-center gap-1.5 px-3 py-1 rounded-full border border-primary/10 shadow-sm"
                                    style={{ background: "rgba(255, 255, 255, 0.8)" }}
                                >
                                    <Sparkles className="w-3.5 h-3.5 text-amber-400 fill-amber-400" />
                                    <span
                                        className="text-sm font-black text-transparent bg-clip-text"
                                        style={{ backgroundImage: mainGradient }}
                                    >
                                        AI 매칭률 {displayMatchScore}%
                                    </span>
                                </div>
                            )}

                            <span className="text-gray-400 text-sm hidden sm:inline">·</span>
                            <span className="text-gray-800 font-medium text-sm">{experienceLabel}</span>

                            {/* Status Badges */}
                            <Badge variant="outline" className="text-xs font-bold text-purple-600 border-purple-200 bg-purple-50">
                                적극 채용 중
                            </Badge>
                            {/* New Badge */}
                            {postedLabel === "방금 전" && (
                                <Badge variant="outline" className="text-xs font-bold text-orange-600 border-orange-200 bg-orange-50">
                                    NEW
                                </Badge>
                            )}
                        </div>

                        {/* Skills & Salary - Distinct Boxes */}
                        <div className="space-y-3 mb-6">
                            {/* Skills Box */}
                            <div className="flex flex-wrap gap-1.5 p-3 bg-slate-50 rounded-xl border border-slate-100/50">
                                {skills.slice(0, 6).map((skill, i) => (
                                    <Badge key={i} variant="secondary" className="bg-white border-slate-200 text-slate-600 hover:bg-slate-50 font-medium whitespace-nowrap">
                                        {skill}
                                    </Badge>
                                ))}
                                {skills.length > 6 && (
                                    <Badge variant="secondary" className="bg-white border-slate-200 text-slate-400 font-medium">+{skills.length - 6}</Badge>
                                )}
                            </div>

                            {/* Salary/Experience Box */}
                            <div className="p-3 bg-slate-50 rounded-xl border border-slate-100/50 flex items-center gap-2">
                                <Coins className="h-4 w-4 text-emerald-500" />
                                <span className="text-sm font-semibold text-slate-700">
                                    희망 연봉: {salary} ({experienceLabel})
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex items-center gap-2 shrink-0">
                        <Button
                            variant="outline"
                            size="lg"
                            className={cn(
                                "h-11 px-4 border-gray-200 text-gray-500 hover:text-red-500 hover:border-red-200 hover:bg-red-50 gap-2 font-medium transition-all",
                                interested && "text-red-500 border-red-200 bg-red-50"
                            )}
                            onClick={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                setInterested((v) => !v);
                            }}
                        >
                            <Heart className={cn("w-5 h-5", interested && "fill-current")} />
                            <span className="hidden sm:inline">관심</span>
                        </Button>
                        <Button
                            asChild
                            size="lg"
                            className="h-11 px-8 text-base font-bold text-white shadow-md hover:shadow-lg transition-all hover:scale-[1.02] border-0"
                            style={{ background: mainGradient }}
                        >
                            <Link to={`/jobs/${id}`} onClick={(e) => e.stopPropagation()}>
                                지원하기
                            </Link>
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
}
