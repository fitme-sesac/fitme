import { Button } from "@/components/ui/button";
import { PremiumAdBanner } from "./PremiumAdBanner";
import { AIJobCard, AIJobProps } from "./AIJobCard";
import { Link } from "react-router-dom";
import { Job } from "@/pages/Jobs";

function formatActiveTime(createdAt: string): string {
    const diff = Math.floor((Date.now() - new Date(createdAt).getTime()) / (1000 * 60 * 60 * 24));
    if (diff === 0) return "방금 전";
    if (diff === 1) return "1일 전";
    if (diff < 7) return `${diff}일 전`;
    return new Date(createdAt).toLocaleDateString().slice(2); // YY.MM.DD
}

interface SurfitSectionProps {
    displayName?: string;
    jobs?: Job[];
}

export function SurfitSection({ displayName, jobs = [] }: SurfitSectionProps) {
    // Map API Job to AIJobProps
    const displayJobs: AIJobProps[] = jobs.slice(0, 4).map(job => {
        const skills = Array.isArray(job.stack)
            ? job.stack
            : job.stack != null && typeof job.stack === "string"
                ? String(job.stack).split(",").map(s => s.trim())
                : [];

        return {
            id: String(job.jobId),
            company: job.companyName,
            logo: job.companyLogoUrl || job.companyName.substring(0, 1),
            position: job.title,
            experience: job.requiredExperience ? `경력 ${job.requiredExperience}년+` : "신입/경력",
            location: job.location?.split(" ").slice(0, 2).join(" ") || job.location, // Shorten location
            matchScore: job.matchInfo?.overallMatchRate ?? job.matchInfo?.matchRate ?? 80, // Fallback
            skills: skills,
            salary: job.salaryDisplay || "회사내규",
            isNew: (Date.now() - new Date(job.createdAt).getTime()) < 1000 * 60 * 60 * 24 * 3, // New if < 3 days
            activeTime: formatActiveTime(job.createdAt),
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

                <div className="mb-8 p-1">
                    <PremiumAdBanner />
                </div>

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
