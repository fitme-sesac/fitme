import { useState, useRef } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { Loader2, RotateCcw, ChevronDown, Check, ChevronRight, ChevronLeft, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { JobCard } from "@/components/home/JobCard";
import { RecommendedSection } from "@/components/home/RecommendedSection";
import { SideJobList } from "@/components/home/SideJobList";
import { usePublicJobs } from "@/hooks/useJobs";

// --- Types ---
interface Job {
    jobId: number;
    companyName: string;
    companyLogoUrl?: string; // Optional if sometimes missing
    title: string;
    location: string;
    salaryDisplay?: string;
    stack: string[];
    createdAt: string; // ISO date string
    adBidCredit?: number;
    matchInfo?: {
        score: number;
    };
}

interface JobsResponse {
    jobs: Job[];
    totalPages: number;
    totalElements: number;
}

const FILTER_OPTIONS: Record<string, string[]> = {
    "포지션": ["전체", "서버/백엔드", "프론트엔드", "웹 풀스택", "안드로이드", "iOS", "머신러닝/AI", "데이터 엔지니어", "DevOps", "게임 클라이언트", "보안 엔진"],
    "경력": ["전체", "신입", "1년", "2년", "3년", "4년", "5년", "6년", "7년", "8년", "9년", "10년 이상"],
    "스킬": ["전체", "Java", "Python", "JavaScript", "TypeScript", "React", "Vue", "Spring", "Node.js", "Django", "AWS", "Docker", "Kubernetes"],
    "연봉": ["전체", "3,000만원 이상", "4,000만원 이상", "5,000만원 이상", "6,000만원 이상", "7,000만원 이상", "8,000만원 이상", "1억원 이상"],
    "근무지": ["전체", "서울 강남구", "서울 서초구", "서울 송파구", "서울 마포구", "서울 영등포/구로", "경기 판교/분당", "인천", "대전", "부산", "제주"],
    "서비스 분야": ["전체", "커머스", "금융/핀테크", "O2O", "소셜/커뮤니티", "게임", "SaaS", "인공지능", "블록체인", "모빌리티", "여행"]
};

export default function Jobs() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [keyword, setKeyword] = useState(searchParams.get("keyword") || "");
    const [page, setPage] = useState(0);
    const [activePopup, setActivePopup] = useState<string | null>(null);
    const filterScrollRef = useRef<HTMLDivElement>(null);

    const scrollFilter = (direction: "left" | "right") => {
        if (filterScrollRef.current) {
            const amount = direction === "left" ? -200 : 200;
            filterScrollRef.current.scrollBy({ left: amount, behavior: "smooth" });
        }
    };

    // Cast the data to our interface or use generic if usePublicJobs supports it.
    // Assuming usePublicJobs returns { data: JobsResponse, ... } structure roughly.
    const { data: rawData, isLoading, error } = usePublicJobs({
        page,
        size: 10,
        keyword: searchParams.get("keyword") || "",
    });

    const data = rawData as JobsResponse | undefined;

    const jobs = data?.jobs || [];
    const totalPages = data?.totalPages || 0;
    const totalElements = data?.totalElements || 0;

    return (
        <div className="min-h-screen bg-background text-foreground font-sans selection:bg-primary/20">
            <Sidebar />

            <div className="lg:pl-64 transition-all duration-300">
                <Header />

                <main className="container max-w-7xl mx-auto py-6 px-4 md:px-8">
                    {/* 상단 추천/광고 섹션 (탭 형태) */}
                    <RecommendedSection />

                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-8">
                        {/* 중앙 피드 (8 cols) */}
                        <div className="lg:col-span-8 space-y-6">

                            {/* Advanced Filter Section (Reference Style) */}
                            <div className="sticky top-16 z-30 bg-background/95 backdrop-blur-sm -mx-4 px-4 md:mx-0 md:px-0 mb-6 border-b border-border/40">
                                <div className="py-4 relative group">
                                    {/* Scroll Buttons */}
                                    <button
                                        onClick={() => scrollFilter("left")}
                                        className="absolute left-0 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-white/90 backdrop-blur shadow-md rounded-full flex items-center justify-center text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:scale-110 disabled:opacity-0 border border-gray-100"
                                        aria-label="Scroll left"
                                    >
                                        <ChevronLeft className="w-5 h-5" />
                                    </button>
                                    <button
                                        onClick={() => scrollFilter("right")}
                                        className="absolute right-0 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-white/90 backdrop-blur shadow-md rounded-full flex items-center justify-center text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:scale-110 border border-gray-100"
                                        aria-label="Scroll right"
                                    >
                                        <ChevronRight className="w-5 h-5" />
                                    </button>

                                    {/* Filter Bar */}
                                    <div
                                        ref={filterScrollRef}
                                        className="flex items-center gap-2 overflow-x-auto pb-1 px-8 scrollbar-hide [&::-webkit-scrollbar]:hidden scroll-smooth"
                                        style={{ scrollbarWidth: 'none' }}
                                    >
                                        <Button
                                            variant="ghost"
                                            className="rounded-full shrink-0 h-10 px-4 text-muted-foreground hover:text-primary hover:bg-primary/10 font-bold flex items-center gap-1.5 mr-2"
                                            onClick={() => { setKeyword(""); setPage(0); setActivePopup(null); }}
                                        >
                                            <RotateCcw className="w-4 h-4" /> 초기화
                                        </Button>

                                        {Object.keys(FILTER_OPTIONS).map((key) => (
                                            <Button
                                                key={key}
                                                variant={activePopup === key ? "default" : "outline"}
                                                className={`rounded-full border-gray-300 font-normal px-4 h-10 shrink-0 transition-colors ${activePopup === key
                                                        ? "bg-gray-900 text-white border-transparent"
                                                        : "bg-white text-gray-600 hover:bg-gray-50 hover:text-primary hover:border-primary"
                                                    }`}
                                                onClick={() => setActivePopup(activePopup === key ? null : key)}
                                            >
                                                {key} <ChevronDown className={`ml-2 h-3 w-3 transition-transform ${activePopup === key ? "rotate-180" : ""} `} />
                                            </Button>
                                        ))}
                                    </div>
                                </div>

                                {/* Expanded Filter Detail Area */}
                                {activePopup && FILTER_OPTIONS[activePopup] && (
                                    <div className="pb-4 animate-in slide-in-from-top-2 fade-in duration-200">
                                        <div className="flex flex-wrap gap-2 p-4 bg-gray-50/50 rounded-xl border border-gray-100">
                                            {FILTER_OPTIONS[activePopup].map((option) => (
                                                <button
                                                    key={option}
                                                    onClick={() => {
                                                        const val = option === "전체" ? "" : option;
                                                        setKeyword(val);
                                                        setPage(0);
                                                    }}
                                                    className={`px-3 py-1.5 rounded-lg text-sm transition-colors border ${(keyword === option || (option === "전체" && keyword === ""))
                                                            ? "bg-white border-primary text-primary font-bold shadow-sm"
                                                            : "bg-white border-transparent text-gray-600 hover:bg-gray-100 hover:border-gray-200"
                                                        }`}
                                                >
                                                    {option}
                                                </button>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* Count & Sort */}
                                <div className="flex items-center justify-between py-3">
                                    <div className="text-sm font-bold text-gray-900">
                                        전체 <span className="text-[#10B981]">{totalElements.toLocaleString()}</span>개
                                    </div>
                                    <div className="flex items-center gap-4 text-xs sm:text-sm">
                                        <button className="text-gray-400 hover:text-gray-600 transition-colors">응답 빠른 순</button>
                                        <button className="text-gray-900 font-bold flex items-center gap-1">
                                            <Check className="h-3 w-3 text-[#10B981]" /> 최근 업데이트 순
                                        </button>
                                    </div>
                                </div>
                            </div>

                            {/* Scout Banner */}
                            {!isLoading && (
                                <div className="bg-[#F8F9FA] rounded-md py-4 px-6 mb-8 flex flex-col sm:flex-row items-center justify-between gap-2 border border-[#E1E4E8] sm:h-[60px]">
                                    <p className="text-[#4E5968] text-sm tracking-tight">지금 프로필을 등록하면, 스카우트 제안을 받을 수 있어요</p>
                                    <Link to="/resume" className="text-[#3B82F6] text-sm font-bold flex items-center hover:underline whitespace-nowrap">
                                        내가 받을 스카우트 제안 보기 <ChevronRight className="ml-0.5 h-4 w-4" />
                                    </Link>
                                </div>
                            )}

                            {/* 로딩 상태 */}
                            {isLoading && (
                                <div className="flex flex-col items-center justify-center py-20 space-y-4">
                                    <Loader2 className="h-10 w-10 animate-spin text-primary" />
                                    <p className="text-muted-foreground animate-pulse">AI가 딱 맞는 공고를 찾고 있어요...</p>
                                </div>
                            )}

                            {/* 에러 상태 */}
                            {error && !isLoading && (
                                <div className="rounded-xl bg-destructive/10 p-6 text-center text-destructive border border-destructive/20">
                                    <p>채용공고를 불러오는데 실패했습니다.</p>
                                    <Button variant="outline" className="mt-4" onClick={() => window.location.reload()}>
                                        다시 시도
                                    </Button>
                                </div>
                            )}

                            {/* 채용공고 리스트 (피드) */}
                            {!isLoading && jobs.length > 0 && (
                                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
                                    {jobs.map((job) => (
                                        <JobCard
                                            key={job.jobId}
                                            id={job.jobId}
                                            company={job.companyName}
                                            logo={job.companyLogoUrl || job.companyName.charAt(0)}
                                            title={job.title}
                                            location={job.location}
                                            salary={job.salaryDisplay || "회사내규"}
                                            skills={job.stack || []}
                                            postedAt={new Date(job.createdAt).toLocaleDateString()}
                                            isAd={(job.adBidCredit ?? 0) > 0}
                                            matchScore={job.matchInfo?.score}
                                        />
                                    ))}
                                </div>
                            )}

                            {/* 데이터 없음 */}
                            {!isLoading && !error && jobs.length === 0 && (
                                <div className="text-center py-20 bg-card rounded-3xl border border-dashed border-border">
                                    <p className="text-lg font-medium text-muted-foreground">검색 결과가 없습니다</p>
                                    <Button variant="link" onClick={() => { setKeyword(""); setSearchParams({}); }} className="mt-2 text-primary">
                                        전체 보기
                                    </Button>
                                </div>
                            )}

                            {/* 페이지네이션 */}
                            {!isLoading && totalPages > 1 && (
                                <div className="flex justify-center pt-8 pb-4">
                                    <div className="flex gap-2">
                                        <Button
                                            variant="outline"
                                            disabled={page === 0}
                                            onClick={() => setPage(page - 1)}
                                            className="rounded-xl"
                                        >
                                            이전
                                        </Button>
                                        <Button
                                            variant="outline"
                                            disabled={page >= totalPages - 1}
                                            onClick={() => setPage(page + 1)}
                                            className="rounded-xl"
                                        >
                                            다음
                                        </Button>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* 우측 사이드바 (4 cols) */}
                        <div className="hidden lg:block lg:col-span-4 space-y-6">
                            <SideJobList />

                            {/* 추가 위젯 (예: 인기 태그) */}
                            <div className="bg-card rounded-2xl border border-border p-5 sticky top-[500px]">
                                <h3 className="font-bold mb-4 text-sm text-muted-foreground">인기 검색 키워드</h3>
                                <div className="flex flex-wrap gap-2">
                                    {["Python", "Java", "React", "Spring Boot", "AI", "Data Engineer", "Frontend"].map(tag => (
                                        <span key={tag} className="px-3 py-1.5 rounded-lg bg-secondary/50 text-xs font-medium cursor-pointer hover:bg-primary/20 hover:text-primary transition-colors">
                                            #{tag}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </main>
                <Footer />
            </div>
        </div>
    );
}
