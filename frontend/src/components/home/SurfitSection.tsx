import { Button } from "@/components/ui/button";
import { PremiumAdBanner } from "./PremiumAdBanner";
import { AIJobCard, AIJobProps } from "./AIJobCard";
import { Link } from "react-router-dom";
import { Job } from "@/pages/common/Jobs";

function formatActiveTime(createdAt: string | number): string {
    const timeValue = typeof createdAt === 'string' ? new Date(createdAt).getTime() : createdAt;
    const diff = Math.floor((Date.now() - timeValue) / (1000 * 60 * 60 * 24));
    if (diff === 0) return "방금 전";
    if (diff === 1) return "1일 전";
    if (diff < 7) return `${diff}일 전`;
    return new Date(timeValue).toLocaleDateString().slice(2); // YY.MM.DD
}

interface SurfitSectionProps {
    displayName?: string;
    jobs?: Job[];
    resumeStatus?: "NO_PRIMARY" | "PROCESSING" | "COMPLETED";
}

export function SurfitSection({ displayName, jobs = [], resumeStatus }: SurfitSectionProps) {
    // Map API Job or JobRecommendationDTO to AIJobProps
    const displayJobs: AIJobProps[] = (jobs || []).slice(0, 4).map((job: any) => {
        const skills = Array.isArray(job.stack)
            ? job.stack
            : job.stack != null && typeof job.stack === "string"
                ? String(job.stack).split(",").map(s => s.trim())
                : [];

        // Match Score: similarity (0~1) -> percentage (0~100)
        let matchScore = job.matchInfo?.overallMatchRate ?? job.matchInfo?.matchRate;
        if (matchScore === undefined && job.similarity !== undefined) {
            matchScore = Math.round(job.similarity * 100);
        }

        const createdAt = job.createdAt || job.created_at;

        return {
            id: String(job.jobId || job.id || "0"),
            company: job.companyName || job.company_name || job.company || "회사명",
            logo: job.companyLogoUrl || job.company_logo_url || (job.companyName || "C").substring(0, 1),
            position: job.title || "포지션",
            experience: job.requiredExperience !== undefined
                ? (job.requiredExperience > 0 ? `경력 ${job.requiredExperience}년+` : "신입/무관")
                : "신입/경력",
            location: (job.location?.split(" ").slice(0, 2).join(" ") || job.location) || "지역 정보 없음",
            matchScore: matchScore ?? 80,
            skills: skills,
            competitionRate: job.applicationCount !== undefined ? `${job.applicationCount}명 지원` : undefined,
            isNew: createdAt ? (Date.now() - new Date(createdAt).getTime()) < 1000 * 60 * 60 * 24 * 3 : false,
            activeTime: createdAt ? formatActiveTime(createdAt) : "방금 전",
            jobCount: undefined
        };
    });

    if (displayJobs.length === 0) {
        // Fallback to empty or null if no data, or keep mocks if desired?
        // User asked to "connect dummy", creating the implication of using the source.
        // If empty, we can just return null or empty grid to avoid showing nothing if that's expected.
        // But for now, let's render what we have. If 0, it will just be empty grid.
        // To be safe against empty initial load, maybe we check loading in parent?
    }

    return (
        <div className="container px-4 md:px-8 py-8 bg-slate-50/50 dark:bg-background/50">
            {/* Main Content: Full Width */}
            <main className="w-full">
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-2">
                        <span className="text-xl">✨</span>
                        <div>
                            <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
                                <span className="text-emerald-600">AI 추천</span> 채용 정보
                            </h2>
                            <p className="text-xs text-slate-500 mt-1">귀하의 이력서와 가장 잘 맞는 기업입니다.</p>
                        </div>
                    </div>
                </div>

                <div className="mb-4">
                    <PremiumAdBanner />
                </div>

                {/* Resume Status Banner */}
                {resumeStatus === "NO_PRIMARY" && (
                    <div className="mb-8 p-6 bg-white dark:bg-slate-900 border border-emerald-100 dark:border-emerald-900/30 rounded-2xl shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-full bg-emerald-50 dark:bg-emerald-900/20 flex items-center justify-center text-2xl">
                                📝
                            </div>
                            <div>
                                <h3 className="font-bold text-slate-900 dark:text-slate-100">대표 이력서를 등록해보세요!</h3>
                                <p className="text-sm text-slate-500">대표 이력서를 등록하면 나에게 딱 맞는 AI 추천 공고를 확인할 수 있습니다.</p>
                            </div>
                        </div>
                        <Link to="/resume">
                            <Button className="bg-emerald-600 hover:bg-emerald-700 text-white rounded-full px-6">
                                이력서 등록하러 가기
                            </Button>
                        </Link>
                    </div>
                )}

                {resumeStatus === "PROCESSING" && (
                    <div className="mb-8 p-6 bg-white dark:bg-slate-900 border border-blue-100 dark:border-blue-900/30 rounded-2xl shadow-sm flex items-center gap-4">
                        <div className="w-12 h-12 rounded-full bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-2xl">
                            🪄
                        </div>
                        <div>
                            <h3 className="font-bold text-slate-900 dark:text-slate-100">맞춤 추천을 준비 중입니다!</h3>
                            <p className="text-sm text-slate-500">AI가 이력서를 분석하고 있습니다. 잠시 후 나에게 최적화된 공고를 추천해 드릴게요.</p>
                        </div>
                    </div>
                )}

                {/* 4-Column Grid as requested */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                    {displayJobs.map((job) => (
                        <AIJobCard key={job.id} job={job} />
                    ))}
                </div>

                <div className="mt-12 text-center">
                    <Link to="/jobs">
                        <Button variant="outline" className="w-full md:w-auto min-w-[200px] rounded-full border-slate-200 hover:border-slate-300 hover:bg-slate-50 transition-colors">
                            AI 추천 더 보기
                        </Button>
                    </Link>
                </div>
            </main>
        </div>
    );
}
