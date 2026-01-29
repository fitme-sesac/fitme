import { Button } from "@/components/ui/button";
import { PremiumAdBanner } from "./PremiumAdBanner";
import { AIJobCard, AIJobProps } from "./AIJobCard";

const MOCK_AI_JOBS: AIJobProps[] = [
    {
        id: "1",
        company: "Toss",
        logo: "T",
        position: "Frontend Developer (Platform)",
        experience: "경력 3~5년",
        location: "서울 강남구",
        matchScore: 98,
        skills: ["React", "TypeScript", "Next.js", "Design System"],
        salary: "6,000만 ~ 8,000만",
        isNew: true,
        activeTime: "방금 전",
        jobCount: 3
    },
    {
        id: "2",
        company: "Danggeun",
        logo: "D",
        position: "Backend Engineer (Server)",
        experience: "경력 5년+",
        location: "서울 서초구",
        matchScore: 95,
        skills: ["Java", "Spring Boot", "Kotlin", "AWS", "JPA"],
        salary: "7,000만 ~ 1억",
        isNew: true,
        activeTime: "10분 전",
        jobCount: 2
    },
    {
        id: "3",
        company: "Line",
        logo: "L",
        position: "Global Messenger Client Dev",
        experience: "경력 3년+",
        location: "경기 성남시",
        matchScore: 92,
        skills: ["Swift", "iOS", "Objective-C", "RxSwift"],
        salary: "5,500만 ~ 7,500만",
        activeTime: "1시간 전",
        jobCount: 5
    },
    {
        id: "4",
        company: "Woowa Bros",
        logo: "W",
        position: "Fullstack Engineer",
        experience: "경력 4년+",
        location: "서울 송파구",
        matchScore: 89,
        skills: ["Node.js", "React", "AWS", "Docker"],
        salary: "6,500만 ~ 8,500만",
        activeTime: "3시간 전",
        jobCount: 1
    }
];

export function SurfitSection({ displayName }: { displayName: string }) {
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
                    {MOCK_AI_JOBS.map((job) => (
                        <AIJobCard key={job.id} job={job} />
                    ))}
                </div>

                <div className="mt-12 text-center">
                    <Button variant="outline" className="w-full md:w-auto min-w-[200px] rounded-full border-slate-200">
                        AI 추천 더 보기
                    </Button>
                </div>
            </main>
        </div>
    );
}
