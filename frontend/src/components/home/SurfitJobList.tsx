import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Link } from "react-router-dom";

interface JobItem {
    id: string;
    company: string;
    title: string;
    tags: string[];
    logo: string;
    dDay: string;
}

const MOCK_JOBS: JobItem[] = [
    {
        id: "1",
        company: "패리닷",
        title: "AI Agent Engineer",
        tags: ["채용중", "경력 3년+"],
        logo: "",
        dDay: "D-14",
    },
    {
        id: "2",
        company: "마카롱팩토리",
        title: "서버 개발 (5년 이상)",
        tags: ["차량 관리 앱 1위", "경력 5년+"],
        logo: "",
        dDay: "상시",
    },
    {
        id: "3",
        company: "스포트라이트글로벌",
        title: "Fullstack Engineer",
        tags: ["글로벌 플랫폼", "경력 5년+"],
        logo: "",
        dDay: "D-7",
    },
    {
        id: "4",
        company: "테크스타트",
        title: "Frontend Developer",
        tags: ["핀테크", "경력 3년+"],
        logo: "",
        dDay: "채용시",
    },
    {
        id: "5",
        company: "로컬히어로",
        title: "Software Engineer (Platform)",
        tags: ["커뮤니티 플랫폼", "전직군"],
        logo: "",
        dDay: "상시",
    }
];

export function SurfitJobList() {
    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between mb-2">
                <h3 className="font-bold text-lg flex items-center gap-2">
                    🖥️ 개발 채용 공고
                </h3>
                <Link to="/jobs" className="text-xs text-slate-500 hover:text-sky-600">
                    더보기
                </Link>
            </div>

            <div className="space-y-3">
                {MOCK_JOBS.map((job) => (
                    <Link key={job.id} to={`/jobs/${job.id}`} className="block group">
                        <div className="flex gap-3 p-3 rounded-xl bg-slate-50 dark:bg-slate-900/50 hover:bg-white hover:shadow-md border border-transparent hover:border-slate-100 transition-all duration-200">
                            {/* Logo */}
                            <div className="h-10 w-10 shrink-0 rounded-lg bg-white border border-slate-100 flex items-center justify-center overflow-hidden">
                                {job.logo ? (
                                    <img src={job.logo} alt={job.company} className="w-full h-full object-cover" />
                                ) : (
                                    <div className="w-full h-full bg-slate-100 flex items-center justify-center text-xs font-bold text-slate-400">
                                        {job.company[0]}
                                    </div>
                                )}
                            </div>

                            {/* Info */}
                            <div className="flex-1 min-w-0">
                                <h4 className="font-bold text-sm text-slate-900 dark:text-slate-100 truncate group-hover:text-sky-600 transition-colors">
                                    {job.title}
                                </h4>
                                <p className="text-xs text-slate-500 truncate mt-0.5">
                                    {job.company}
                                </p>
                                <div className="flex gap-1 mt-2">
                                    {job.tags.map((tag, idx) => (
                                        <span key={idx} className="inline-block px-1.5 py-0.5 rounded text-[10px] bg-slate-200/50 text-slate-500">
                                            {tag}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        </div>

                    </Link>
                ))}
            </div>

            {/* Mini Ad Card */}
            <div className="mt-6 p-4 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 text-white relative overflow-hidden group cursor-pointer">
                <div className="relative z-10">
                    <h4 className="font-bold text-base mb-1">이직 생각이 없어도<br />한번 대화는 시작해보세요.</h4>
                    <div className="flex items-center gap-1 text-xs opacity-90 mt-2">
                        ☕ 스타트업 대표·담당자의 커피챗 제안
                    </div>
                    <Button size="sm" variant="secondary" className="mt-3 h-8 text-xs bg-white text-indigo-600 hover:bg-white/90 border-0 rounded-full">
                        커피챗 2개 받기
                    </Button>
                </div>
                <div className="absolute right-[-10px] bottom-[-10px] opacity-20 group-hover:opacity-30 transition-opacity">
                    <div className="w-24 h-24 rounded-full bg-white blur-xl" />
                </div>
            </div>
        </div>
    );
}
