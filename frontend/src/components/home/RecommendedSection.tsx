import { useRef, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ChevronRight, ChevronLeft, Building2, Sparkles, Loader2, MapPin } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useAds } from "@/hooks/useAds"; // Using Ad API
import { cn } from "@/lib/utils";

const TABS = [
    { id: "fit", label: "내 조건에 딱 맞는 채용 공고", icon: Sparkles },
    { id: "career", label: "커리어에 딱 맞는 채용 공고", icon: Building2 },
];

export function RecommendedSection() {
    const { user } = useAuth();
    const [activeTab, setActiveTab] = useState("fit");
    const scrollContainerRef = useRef<HTMLDivElement>(null);

    // Ad API Integration (V3 Hybrid for logged-in, V3 Public for guest)
    const { data: recommendations, isLoading } = useAds({
        memberId: user?.id, // Optional: undefined if not logged in
        limit: 10,
    });

    const scroll = (direction: "left" | "right") => {
        if (scrollContainerRef.current) {
            const scrollAmount = direction === "left" ? -400 : 400;
            scrollContainerRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
        }
    };

    return (
        <div className="mb-8 space-y-4">
            {/* AI Recommendation Header */}
            <div className="flex items-center gap-2 px-2">
                <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-primary/10">
                    <Sparkles className="w-5 h-5 text-primary animate-pulse" />
                </div>
                <div>
                    <h2 className="text-xl font-bold text-gray-900 tracking-tight">AI 맞춤형 채용 추천 🎯</h2>
                </div>
            </div>

            {/* Tabs */}
            <div className="flex flex-wrap items-center gap-2">
                {TABS.map((tab) => (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={cn(
                            "flex items-center gap-2 px-4 py-1.5 rounded-full text-sm transition-all",
                            activeTab === tab.id
                                ? "bg-gray-900 text-white font-bold shadow-md"
                                : "bg-white text-gray-400 font-medium hover:bg-gray-50 border border-gray-200"
                        )}
                    >
                        <tab.icon className="w-4 h-4" />
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* Horizontal Scroll List */}
            <div className="w-full relative group/scroll">
                <button
                    onClick={() => scroll("left")}
                    className="absolute left-2 top-1/2 -translate-y-1/2 z-20 w-10 h-10 bg-white/90 backdrop-blur shadow-lg rounded-full flex items-center justify-center text-gray-700 opacity-0 group-hover/scroll:opacity-100 transition-opacity duration-300 hover:scale-110 disabled:opacity-0"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <button
                    onClick={() => scroll("right")}
                    className="absolute right-2 top-1/2 -translate-y-1/2 z-20 w-10 h-10 bg-white/90 backdrop-blur shadow-lg rounded-full flex items-center justify-center text-gray-700 opacity-0 group-hover/scroll:opacity-100 transition-opacity duration-300 hover:scale-110"
                >
                    <ChevronRight className="w-6 h-6" />
                </button>

                <div
                    ref={scrollContainerRef}
                    className="flex gap-4 overflow-x-auto pb-6 pt-2 px-4 no-scrollbar overscroll-x-contain min-h-[210px]"
                >
                    {isLoading ? (
                        <div className="flex items-center justify-center w-full py-10 gap-3">
                            <Loader2 className="w-6 h-6 animate-spin text-primary" />
                            <span className="text-sm text-muted-foreground font-medium">나에게 꼭 맞는 공고를 분석 중입니다...</span>
                        </div>
                    ) : recommendations && recommendations.length > 0 ? (
                        recommendations.map((job: any) => (
                            <Link
                                key={job.jobId || job.campaignId}
                                to={`/jobs/${job.jobId}`}
                                className="flex-none w-[220px] group/card"
                            >
                                <div className="h-[200px] bg-white rounded-2xl border border-gray-100 p-4 shadow-sm transition-all duration-300 group-hover/card:shadow-xl group-hover/card:-translate-y-1 group-hover/card:border-primary/20 flex flex-col justify-between relative overflow-hidden">
                                    {/* Ad Badge */}
                                    <div className="absolute top-0 right-0 px-2 py-0.5 bg-gray-100 text-[10px] font-bold text-gray-500 rounded-bl-lg z-10">
                                        AD
                                    </div>
                                    <div>
                                        <div className="flex items-center gap-3 mb-3">
                                            <div className="w-10 h-10 rounded-xl bg-gray-50 border border-gray-50 flex items-center justify-center overflow-hidden">
                                                {job.companyLogoUrl ? (
                                                    <img src={job.companyLogoUrl} alt={job.companyName} className="w-full h-full object-cover" />
                                                ) : (
                                                    <div className="text-lg font-bold text-primary">
                                                        {(job.companyName || "C").charAt(0)}
                                                    </div>
                                                )}
                                            </div>
                                            {/* Show Match Rate only if available (logged in) */}
                                            {job.similarity != null && !isNaN(job.similarity) && (
                                                <div className="px-2 py-0.5 rounded-full bg-primary/5 border border-primary/10 flex items-center gap-1">
                                                    <Sparkles className="w-3 h-3 text-amber-400 fill-amber-400" />
                                                    <span className="text-[10px] font-black text-primary">
                                                        AI 매칭률 {Math.round(job.similarity * 100)}%
                                                    </span>
                                                </div>
                                            )}
                                        </div>
                                        <h3 className="font-bold text-gray-900 leading-snug line-clamp-2 mb-1 group-hover/card:text-primary transition-colors">
                                            {job.jobTitle || job.title}
                                        </h3>
                                        <p className="text-xs font-bold text-gray-500">{job.companyName}</p>
                                    </div>
                                    <div className="flex items-center justify-between pt-3 border-t border-gray-50 text-[11px] text-gray-400 font-medium">
                                        <div className="flex items-center gap-1 truncate">
                                            <MapPin className="w-3 h-3" />
                                            {job.location || "전국"}
                                        </div>
                                        <span className="shrink-0 text-primary font-bold">{job.salaryDisplay || "회사내규"}</span>
                                    </div>
                                </div>
                            </Link>
                        ))
                    ) : (
                        <div className="flex items-center justify-center w-full py-10">
                            <p className="text-sm text-muted-foreground font-medium">현재 맞춤 추천 공고가 없습니다.</p>
                        </div>
                    )}

                    {recommendations && recommendations.length > 5 && (
                        <div className="flex-none w-[60px] flex items-center justify-center">
                            <Link to="/jobs?sortBy=match">
                                <Button variant="ghost" size="icon" className="w-12 h-12 rounded-full border border-gray-200 hover:bg-white hover:shadow-md transition-all">
                                    <ChevronRight className="w-5 h-5 text-gray-400" />
                                </Button>
                            </Link>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
