import { MapPin, Coins, Building2, Bookmark, Sparkles, Loader2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Link } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useEffect, useState } from "react";
import { Card } from "@/components/ui/card";
import { useQueryClient } from "@tanstack/react-query";
import { scrapJob, checkScrapStatus } from "@/api/jobs";

interface JobCardProps {
    id: number;
    company: string;
    logo: string;
    logoUrl?: string; // Optional URL for image logos
    title: string;
    location: string;
    salary: string;
    skills: string[] | string | null | undefined;
    postedAt: string;
    isAd?: boolean;
    matchScore?: number;
    createdAt?: string;
    applyCount?: number;
    recruitmentCapacity?: number;
}

const BRAND_COLORS: Record<string, string> = {
    Toss: "#0064FF",
    Danggeun: "#FF6F0F",
    Line: "#06C755",
    "Woowa Bros": "#2AC1BC",
    Naver: "#03C75A",
    Kakao: "#FEE500",
    Samsung: "#1428A0",
    Hyundai: "#002C5F",
    LG: "#A50034",
    SK: "#EA002C",
    Coupang: "#E41B23",
};

const DEFAULT_COLOR = "#000000";

function getBrandColor(company: string): string {
    const key = Object.keys(BRAND_COLORS).find((k) =>
        company.toLowerCase().includes(k.toLowerCase())
    );
    return key ? BRAND_COLORS[key] : DEFAULT_COLOR;
}

export function JobCard({
                            id,
                            company,
                            logo,
                            logoUrl,
                            title,
                            location,
                            salary,
                            skills: skillsProp,
                            postedAt,
                            isAd = false,
                            matchScore: matchScoreProp,
                            createdAt,
                            applyCount = 0,
                            recruitmentCapacity = 0,
                        }: JobCardProps) {
    const auth: any = useAuth();
    const user = auth?.user ?? null;
    const isAuthenticated =
        typeof auth?.isAuthenticated === "boolean" ? auth.isAuthenticated : Boolean(user);

    const queryClient = useQueryClient();

    const skills = Array.isArray(skillsProp)
        ? skillsProp
        : typeof skillsProp === "string"
            ? skillsProp.split(",").map((s) => s.trim()).filter(Boolean)
            : [];

    const [interested, setInterested] = useState(false);
    const [loading, setLoading] = useState(false);

    // 스크랩 상태 조회
    useEffect(() => {
        const fetchScrapStatus = async () => {
            if (!user || !id) return;
            try {
                const response = await checkScrapStatus(id);
                setInterested(response?.scraped ?? false);
            } catch (error) {
                console.error("스크랩 상태 조회 실패:", error);
            }
        };
        fetchScrapStatus();
    }, [id, user]);

    // 스크랩 토글 (API 연동 + 캐시 무효화)
    const handleScrapToggle = async (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();

        if (!user) {
            alert("로그인이 필요합니다.");
            return;
        }
        if (loading) return;

        setLoading(true);
        try {
            const response = await scrapJob(id);
            setInterested(response?.scraped ?? !interested);

            queryClient.invalidateQueries({ queryKey: ["myScrapedJobs"] });
            queryClient.invalidateQueries({ queryKey: ["scrapedJobs"] });
            queryClient.invalidateQueries({ queryKey: ["profileSummary"] });
        } catch (error) {
            console.error("스크랩 토글 실패:", error);
            alert("스크랩 처리에 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const displayLocation = (location || "").split(" ").slice(0, 2).join(" ");
    const experienceLabel = "신입/경력";
    const mainGradient = "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)";
    const brandColor = getBrandColor(company);
    const postedLabel = postedAt || "방금 전";

    const hasRealMatchScore =
        matchScoreProp !== undefined && matchScoreProp !== null && Number.isFinite(matchScoreProp);

    // 기존 기능 유지: 로그인 사용자는 matchScore가 없으면 랜덤 fallback(70~95)
    const displayMatchScore = isAuthenticated
        ? (hasRealMatchScore ? matchScoreProp : Math.floor(Math.random() * 26) + 70)
        : undefined;

    return (
        <Card
            className={cn(
                "group relative flex flex-col h-full bg-white transition-all duration-300 hover:shadow-xl hover:-translate-y-1 overflow-hidden border border-gray-100",
                isAd && "ring-2 ring-primary/20"
            )}
        >
            {isAd && (
                <div className="absolute top-0 right-0 bg-primary/10 text-primary text-[10px] font-bold px-2 py-0.5 rounded-bl-lg">
                    AD
                </div>
            )}

            <div className="p-5 flex flex-col h-full">
                {/* Header */}
                <div className="flex items-start gap-4 mb-4 min-h-[90px]">
                    <div
                        className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full text-white text-xl font-bold shadow-sm mt-1"
                        style={{ backgroundColor: brandColor }}
                    >
                        {logoUrl ? (
                            <img
                                src={logoUrl}
                                alt={company}
                                className="w-full h-full object-cover rounded-full"
                            />
                        ) : (
                            <span>{(company?.charAt?.(0) || "C").toUpperCase()}</span>
                        )}
                    </div>

                    <div className="flex flex-col w-full">
                        <div className="flex items-center gap-2 mb-0.5">
                            <span className="font-bold text-lg text-gray-900 line-clamp-1">{company}</span>
                            {postedLabel === "방금 전" && (
                                <span className="shrink-0 px-1.5 py-0.5 rounded text-[10px] font-bold bg-orange-100 text-orange-600">
                  NEW
                </span>
                            )}
                        </div>

                        <h3 className="text-base font-bold text-gray-800 leading-snug line-clamp-2 min-h-[44px] group-hover:text-primary transition-colors">
                            {title}
                        </h3>

                        <p className="text-xs text-gray-500 mt-1.5 flex items-center gap-2">
                            <Building2 className="w-3 h-3 shrink-0" />
                            <span className="shrink-0">{experienceLabel}</span>
                            <span className="w-px h-2.5 bg-gray-300 shrink-0" />
                            <MapPin className="w-3 h-3 shrink-0" />
                            <span className="truncate">{displayLocation}</span>
                        </p>
                    </div>
                </div>

                {/* 가져오는 코드 기능: matchScore가 "실제 값"일 때만 배지 표시 */}
                {hasRealMatchScore && (
                    <div className="mb-4">
                        <div
                            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full border border-primary/10 shadow-sm"
                            style={{ background: "rgba(255, 255, 255, 0.8)" }}
                        >
                            <Sparkles className="w-3.5 h-3.5 text-amber-400 fill-amber-400" />
                            <span
                                className="text-sm font-black text-transparent bg-clip-text"
                                style={{ backgroundImage: mainGradient }}
                            >
                AI 매칭률 {matchScoreProp}%
              </span>
                        </div>
                    </div>
                )}

                {/* 기존 코드 기능: 로그인 사용자는 매칭률 바 표시(실제값 우선, 없으면 랜덤 fallback) */}
                {isAuthenticated && displayMatchScore !== undefined && (
                    <div className="mb-5">
                        <div className="flex justify-between items-end mb-2">
                            <span className="text-sm font-bold text-gray-700">AI 매칭률</span>
                            <span
                                className="text-lg font-black text-transparent bg-clip-text"
                                style={{ backgroundImage: mainGradient }}
                            >
                {displayMatchScore}%
              </span>
                        </div>
                        <div className="w-full h-2.5 bg-gray-100 rounded-full overflow-hidden">
                            <div
                                className="h-full rounded-full transition-all duration-1000 ease-out"
                                style={{
                                    width: `${displayMatchScore}%`,
                                    background: mainGradient,
                                }}
                            />
                        </div>
                    </div>
                )}

                {/* Skills + Salary/Competition */}
                <div className="space-y-3 mb-6 flex-1">
                    <div className="flex flex-wrap gap-1.5 p-3 bg-slate-50 rounded-xl border border-slate-100/50">
                        {skills.slice(0, 4).map((skill, i) => (
                            <Badge
                                key={i}
                                variant="secondary"
                                className="bg-white border-slate-200 text-slate-600 hover:bg-slate-50 font-medium whitespace-nowrap"
                            >
                                {skill}
                            </Badge>
                        ))}
                        {skills.length > 4 && (
                            <Badge
                                variant="secondary"
                                className="bg-white border-slate-200 text-slate-400 font-medium"
                            >
                                +{skills.length - 4}
                            </Badge>
                        )}
                    </div>

                    <div className="p-3 bg-slate-50 rounded-xl border border-slate-100/50 flex items-center gap-2">
                        <Coins className="h-4 w-4 text-emerald-500" />
                        <span className="text-sm font-semibold text-slate-700">
              {isAuthenticated ? (
                  <>
                      {salary} ({postedLabel === "방금 전" ? "D-Day" : postedLabel})
                  </>
              ) : (
                  <>
                      경쟁률 {applyCount}:{recruitmentCapacity}
                  </>
              )}
            </span>
                    </div>
                </div>

                {/* Actions */}
                <div className="mt-auto">
                    <div className="flex items-center gap-3">
                        <Button
                            asChild
                            className="flex-1 h-12 rounded-xl text-base font-bold text-white shadow-md hover:shadow-lg transition-all hover:scale-[1.02] border-0"
                            style={{ background: mainGradient }}
                        >
                            <Link to={`/jobs/${id}`}>지원하기</Link>
                        </Button>

                        {/* 기존 기능(스크랩 API) + 가져오는 코드(Bookmark 아이콘) 병합 */}
                        <button
                            type="button"
                            className={cn(
                                "h-12 w-12 flex items-center justify-center rounded-xl border border-gray-200 text-gray-400 hover:border-gray-300 hover:text-gray-600 transition-colors bg-white",
                                interested && "text-red-500 border-red-200 bg-red-50"
                            )}
                            onClick={handleScrapToggle}
                            disabled={loading}
                        >
                            {loading ? (
                                <Loader2 className="w-6 h-6 animate-spin" />
                            ) : (
                                <Bookmark className={cn("w-6 h-6", interested && "fill-current")} />
                            )}
                        </button>
                    </div>

                    <div className="flex items-center justify-between mt-4 text-[11px] text-gray-400 font-medium">
                        <span>마지막 활동: {postedLabel}</span>
                        <span>채용공고 1개</span>
                    </div>
                </div>
            </div>
        </Card>
    );
}
