import { useRef, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ChevronRight, ChevronLeft, Building2, Flame, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";

// Mock Data Structure matching Developer Jobs
const RECOMMENDED_JOBS = [
    {
        id: 101,
        company: "LINE",
        title: "LINE Global Messenger Frontend Developer",
        deadline: "~02.28",
        type: "recommend",
        icon: "L"
    },
    {
        id: 102,
        company: "Toss Bank",
        title: "Core Banking Server Developer (Java/Kotlin)",
        deadline: "채용시 마감",
        type: "recommend",
        icon: "T"
    },
    {
        id: 103,
        company: "Danggeun",
        title: "당근페이 서비스 서버 개발자",
        deadline: "~02.15",
        type: "recommend",
        icon: "D"
    },
    {
        id: 104,
        company: "Kakao",
        title: "카카오톡 메시징 서버 개발자",
        deadline: "~03.10",
        type: "recommend",
        icon: "K"
    },
    {
        id: 105,
        company: "Coupang",
        title: "E-Commerce Search Engine Engineer",
        deadline: "상시 채용",
        type: "recommend",
        icon: "C"
    },
    {
        id: 106,
        company: "Woowa Bros",
        title: "배달의민족 정산 시스템 개발자",
        deadline: "~02.20",
        type: "recommend",
        icon: "W"
    },
    {
        id: 107,
        company: "Naver",
        title: "네이버 검색 FE 개발자",
        deadline: "~03.05",
        type: "recommend",
        icon: "N"
    },
    {
        id: 108,
        company: "Bucketplace",
        title: "오늘의집 3D 인테리어 엔진 개발",
        deadline: "채용시 마감",
        type: "recommend",
        icon: "B"
    },
];

const TABS = [
    { id: "fit", label: "내 조건에 딱 맞는 채용 공고", icon: Sparkles },
    { id: "career", label: "커리어에 딱 맞는 채용 공고", icon: Building2 },
];

export function RecommendedSection() {
    const [activeTab, setActiveTab] = useState("fit");
    const scrollContainerRef = useRef<HTMLDivElement>(null);

    const scroll = (direction: "left" | "right") => {
        if (scrollContainerRef.current) {
            const scrollAmount = direction === "left" ? -400 : 400;
            scrollContainerRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
        }
    };

    return (
        <div className="mb-8 space-y-6">
            {/* Tabs */}
            <div className="flex flex-wrap items-center gap-2">
                {TABS.map((tab) => (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm ${activeTab === tab.id
                            ? "bg-[#333] text-white font-bold shadow-md"
                            : "bg-white text-gray-500 font-medium hover:bg-gray-100 border border-transparent"
                            }`}
                    >
                        {tab.id === "recommend" && <span>✨</span>}
                        {tab.id === "hot" && <span>🔥</span>}
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* Horizontal Scroll List */}
            <div className="w-full relative group">
                {/* Gradient Masks */}
                <div className="absolute left-0 top-0 bottom-0 w-12 bg-gradient-to-r from-background to-transparent z-10 pointer-events-none" />
                <div className="absolute right-0 top-0 bottom-0 w-12 bg-gradient-to-l from-background to-transparent z-10 pointer-events-none" />

                {/* Scroll Buttons */}
                <button
                    onClick={() => scroll("left")}
                    className="absolute left-2 top-1/2 -translate-y-1/2 z-20 w-10 h-10 bg-white/90 backdrop-blur shadow-lg rounded-full flex items-center justify-center text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:scale-110 disabled:opacity-0"
                    aria-label="Scroll left"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <button
                    onClick={() => scroll("right")}
                    className="absolute right-2 top-1/2 -translate-y-1/2 z-20 w-10 h-10 bg-white/90 backdrop-blur shadow-lg rounded-full flex items-center justify-center text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:scale-110"
                    aria-label="Scroll right"
                >
                    <ChevronRight className="w-6 h-6" />
                </button>

                <div
                    ref={scrollContainerRef}
                    className="flex gap-4 overflow-x-auto pb-6 pt-2 px-4 scrollbar-hide overscroll-x-contain"
                    style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                >
                    {RECOMMENDED_JOBS.map((job) => (
                        <Link
                            key={job.id}
                            to={`/jobs/${job.id}`}
                            className="flex-none w-[210px] group"
                        >
                            <div className="h-[190px] bg-white rounded-2xl border border-gray-100 p-4 shadow-sm transition-all duration-300 hover:shadow-xl hover:-translate-y-1 hover:border-primary/20 flex flex-col justify-between relative overflow-hidden">
                                <div>
                                    <div className="flex items-center gap-3 mb-3">
                                        <div className="w-10 h-10 rounded-xl bg-[#EFE9E3] flex items-center justify-center text-lg font-bold text-[#333]">
                                            {job.icon}
                                        </div>
                                        <div>
                                            <span className="text-[10px] font-bold text-[#4E56C0] bg-[#F5F3FF] px-2 py-0.5 rounded-full">
                                                95% 일치
                                            </span>
                                        </div>
                                    </div>
                                    <h3 className="font-bold text-gray-900 leading-snug line-clamp-2 mb-1 group-hover:text-primary transition-colors">
                                        {job.title}
                                    </h3>
                                    <p className="text-xs text-gray-500">{job.company}</p>
                                </div>
                                <div className="flex items-center justify-between pt-3 border-t border-gray-50 text-xs text-gray-400">
                                    <span>서울 강남</span>
                                    <span>{job.deadline}</span>
                                </div>
                            </div>
                        </Link>
                    ))}

                    {/* 'More' Card */}
                    <div className="flex-none w-[60px] flex items-center justify-center">
                        <Button variant="ghost" size="icon" className="w-12 h-12 rounded-full border border-gray-200 hover:bg-white hover:shadow-md transition-all">
                            <ChevronRight className="w-5 h-5 text-gray-400" />
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
}
