import { useState, useRef, useEffect, useMemo } from "react";
import { useSearchParams, Link, useNavigate } from "react-router-dom";
import { Loader2, RotateCcw, ChevronDown, Check, ChevronRight, ChevronLeft, Sparkles, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Slider } from "@/components/ui/slider";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { JobCard } from "@/components/home/JobCard";
import { WideJobCard } from "@/components/home/WideJobCard";
import { RecommendedSection } from "@/components/home/RecommendedSection";
import { SideJobList } from "@/components/home/SideJobList";
import { CompanySidebar } from "@/components/home/CompanySidebar";
import { CompanyMarketingBanner } from "@/components/home/CompanyMarketingBanner";
import { useAuth } from "@/contexts/AuthContext";
import { usePublicJobs, useFilterOptions, usePositionCounts } from "@/hooks/useJobs";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";

// --- Types ---
export interface Job {
    jobId: number;
    companyName: string;
    companyLogoUrl?: string; // Optional if sometimes missing
    title: string;
    location: string;
    salaryDisplay?: string;
    stack: string[]; // job_posting.stack (기술 스택)
    position?: string; // stack 기반 도출: "프론트엔드" | "백엔드" | "풀스택"
    createdAt: string; // ISO date string
    adBidCredit?: number;
    matchInfo?: {
        overallMatchRate?: number;
        matchRate?: number;
        stackMatchRate?: number;
        experienceMatchRate?: number;
        vectorMatchRate?: number;
        matchLevel?: string;
        matchedStacks?: string[];
        missingStacks?: string[];
        requiredStacks?: string[];
    };
    requiredExperience?: number; // 경력 (년)
    employer?: {
        description?: string; // 기업 요약
        summary?: string;
    };
}

interface JobsResponse {
    jobs: Job[];
    totalPages: number;
    totalElements: number;
}

// 기본 필터 옵션 (DB에서 로드 전 표시용)
const DEFAULT_SKILL_OPTIONS = ["전체", "Java", "Python", "JavaScript", "TypeScript", "React", "Vue", "Spring", "Node.js", "Django", "AWS", "Docker", "Kubernetes"];
const DEFAULT_INDUSTRY_OPTIONS = ["전체", "커머스", "금융/핀테크", "O2O", "소셜/커뮤니티", "게임", "SaaS", "인공지능", "블록체인", "모빌리티", "여행", "헬스케어", "에듀테크", "HR테크", "부동산", "푸드테크", "보안", "IoT", "IT서비스"];
const DEFAULT_POSITION_OPTIONS = ["전체", "서버/백엔드", "프론트엔드", "웹 풀스택", "안드로이드", "iOS", "크로스플랫폼", "머신러닝/AI", "데이터 엔지니어", "DevOps", "게임 클라이언트", "임베디드", "QA 엔지니어"];

const EXPERIENCE_LABELS = ["신입", "1년", "2년", "3년", "4년", "5년", "6년", "7년", "8년", "9년", "10년+"];
const getExperienceLabel = (value: number) => EXPERIENCE_LABELS[value] ?? "신입";
const SALARY_LABELS = ["3,000만원", "4,000만원", "5,000만원", "6,000만원", "7,000만원", "8,000만원", "9,000만원", "1억원+"];
const getSalaryLabel = (value: number) => SALARY_LABELS[value] ?? "3,000만원";

/** 근무지: 1단계 큰 도시(광역) → 2단계 세부 지역 */
const LOCATION_REGIONS: { region: string; subRegions: string[] }[] = [
    { region: "서울", subRegions: ["강남구", "서초구", "송파구", "마포구", "영등포구", "구로구", "강서구", "양천구", "노원구", "은평구", "종로구", "중구", "용산구", "성동구", "광진구", "동대문구", "성북구", "도봉구", "동작구", "관악구", "금천구", "서대문구"] },
    { region: "경기", subRegions: ["판교/분당", "성남", "수원", "용인", "고양", "부천", "안양", "안산", "화성", "시흥", "파주", "김포", "광명", "군포", "의왕", "이천", "오산", "평택", "의정부", "하남"] },
    { region: "인천", subRegions: ["남동구", "부평구", "서구", "연수구", "미추홀구", "동구", "중구", "계양구", "강화", "옹진"] },
    { region: "강원", subRegions: ["춘천", "강릉", "원주", "동해", "태백", "속초", "삼척", "홍천", "횡성", "영월", "평창", "정선", "철원", "화천", "양구", "인제", "고성", "양양"] },
    { region: "대전", subRegions: ["서구", "유성구", "대덕구", "동구", "중구"] },
    { region: "세종", subRegions: [] },
    { region: "충북", subRegions: ["청주", "충주", "제천", "보은", "옥천", "영동", "증평", "진천", "괴산", "음성", "단양"] },
    { region: "충남", subRegions: ["천안", "공주", "보령", "아산", "서산", "논산", "계룡", "당진", "금산", "부여", "서천", "청양", "홍성", "예산", "태안"] },
    { region: "대구", subRegions: ["수성구", "달서구", "북구", "동구", "서구", "남구", "중구", "달성", "군위"] },
    { region: "부산", subRegions: ["해운대구", "수영구", "남구", "동구", "동래구", "부산진구", "연제구", "금정구", "서구", "사하구", "영도구", "중구", "기장"] },
    { region: "울산", subRegions: ["남구", "중구", "북구", "울주"] },
    { region: "광주", subRegions: ["서구", "북구", "광산구", "남구", "동구"] },
    { region: "전북", subRegions: ["전주", "군산", "익산", "정읍", "남원", "김제", "완주", "진안", "무주", "장수", "임실", "순창", "고창", "부안"] },
    { region: "전남", subRegions: ["목포", "여수", "순천", "나주", "광양", "담양", "곡성", "구례", "고흥", "보성", "화순", "장흥", "강진", "해남", "영암", "무안", "함평", "영광", "장성", "완도", "진도", "신안"] },
    { region: "경북", subRegions: ["포항", "경주", "김천", "안동", "구미", "영주", "영천", "상주", "문경", "경산", "군위", "의성", "청송", "영양", "영덕", "청도", "고령", "성주", "칠곡", "예천", "봉화", "울진", "울릉"] },
    { region: "경남", subRegions: ["창원", "진주", "통영", "사천", "김해", "밀양", "거제", "양산", "의령", "함안", "창녕", "고성", "남해", "하동", "산청", "함양", "거창", "합천"] },
    { region: "제주", subRegions: ["제주시", "서귀포시"] },
];


export default function Jobs() {
    const { isCompany, user } = useAuth();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const [keyword, setKeyword] = useState(searchParams.get("keyword") || "");
    const [page, setPage] = useState(0);
    const [activePopup, setActivePopup] = useState<string | null>(null);
    const [locationDrillRegion, setLocationDrillRegion] = useState<string | null>(null);
    const filterScrollRef = useRef<HTMLDivElement>(null);

    // Login Prompt Logic
    const [showLoginPrompt, setShowLoginPrompt] = useState(false);

    useEffect(() => {
        if (!user) {
            const timer = setTimeout(() => {
                setShowLoginPrompt(true);
            }, 5000); // 5초 후 로그인 유도 (커뮤니티보다 조금 더 빠르게)
            return () => clearTimeout(timer);
        }
    }, [user]);

    // 경력·연봉: 슬라이더로만 관리
    const [experienceRange, setExperienceRange] = useState<number[]>([0, 10]);
    const [salaryRange, setSalaryRange] = useState<number[]>([0, 7]);

    // 필터 상태: 다중 선택은 string[], 경력/연봉은 슬라이더 사용
    type FilterValue = string | string[];
    const [selectedFilters, setSelectedFilters] = useState<Record<string, FilterValue>>({
        경력: [],
        스킬: [],
        연봉: [],
        근무지: [],
        "서비스 분야": [],
        "포지션": [],
    });

    const getSelectedArray = (key: string): string[] => {
        const v = selectedFilters[key];
        return Array.isArray(v) ? v : (v ? [v] : []);
    };

    const scrollFilter = (direction: "left" | "right") => {
        if (filterScrollRef.current) {
            const amount = direction === "left" ? -200 : 200;
            filterScrollRef.current.scrollBy({ left: amount, behavior: "smooth" });
        }
    };

    // DB에서 필터 옵션 조회
    const { data: filterOptionsData } = useFilterOptions();

    // 서버 사이드 필터링을 위한 파라미터 구성
    const selectedSkills = getSelectedArray("스킬");
    const selectedLocations = getSelectedArray("근무지");
    const selectedIndustries = getSelectedArray("서비스 분야");
    const selectedPositions = getSelectedArray("포지션");

    // 현재 필터 조건에 따른 포지션별 카운트 조회 (포지션 필터 제외한 다른 필터 조건 적용)
    const { data: positionCountsData } = usePositionCounts({
        keyword: searchParams.get("keyword") || "",
        stack: selectedSkills.length > 0 ? selectedSkills.join(",") : "",
        location: selectedLocations.length > 0 ? selectedLocations[0] : "",
        minExperience: experienceRange[0] > 0 ? experienceRange[0] : null,
        maxExperience: experienceRange[1] < 10 ? experienceRange[1] : null,
        industry: selectedIndustries.length > 0 ? selectedIndustries.join(",") : "",
    });

    // 포지션 카운트 기반으로 0개인 포지션 필터링 (전체는 항상 표시)
    const filteredPositionOptions = useMemo(() => {
        const baseOptions = filterOptionsData?.positionCategories?.length > 0
            ? filterOptionsData.positionCategories
            : DEFAULT_POSITION_OPTIONS;
        
        if (!positionCountsData) return baseOptions;
        
        return baseOptions.filter((option: string) => {
            if (option === "전체") return true;
            const count = positionCountsData[option] || 0;
            return count > 0;
        });
    }, [filterOptionsData, positionCountsData]);
    
    // 필터 옵션 (DB 데이터 우선, 없으면 기본값 사용)
    // 포지션을 가장 왼쪽에 배치
    const FILTER_OPTIONS: Record<string, string[]> = useMemo(() => ({
        // 포지션: DB 기반 동적 옵션 + 카운트 기반 필터링 - 가장 왼쪽 배치
        "포지션": filteredPositionOptions,
        "경력": [], // 슬라이더로 관리
        "스킬": filterOptionsData?.stacks?.length > 0
            ? ["전체", ...filterOptionsData.stacks]
            : DEFAULT_SKILL_OPTIONS,
        "연봉": [], // 슬라이더로 관리
        "근무지": [], // 2단계(광역→세부) UI로 별도 렌더
        // 서비스 분야: DB 기반 동적 옵션 (실제 기업 업종만 표시, 한글 변환됨)
        "서비스 분야": filterOptionsData?.industries?.length > 0
            ? filterOptionsData.industries
            : DEFAULT_INDUSTRY_OPTIONS
    }), [filterOptionsData, filteredPositionOptions]);
    
    // 서버 API 호출 - 필터 파라미터 전달 (포지션 + 서비스 분야 서버사이드 필터링 적용)
    const { data: rawData, isLoading, error } = usePublicJobs({
        page,
        size: 12,
        keyword: searchParams.get("keyword") || "",
        stack: selectedSkills.length > 0 ? selectedSkills.join(",") : "",
        location: selectedLocations.length > 0 ? selectedLocations[0] : "", // 첫 번째 지역만 서버로 전달
        minExperience: experienceRange[0] > 0 ? experienceRange[0] : null,
        maxExperience: experienceRange[1] < 10 ? experienceRange[1] : null,
        position: selectedPositions.length > 0 ? selectedPositions.join(",") : "",
        industry: selectedIndustries.length > 0 ? selectedIndustries.join(",") : "",
    });

    const data = rawData as JobsResponse | undefined;

    const jobs = data?.jobs || [];
    const totalPages = data?.totalPages || 0;
    const totalElements = data?.totalElements || 0;

    // 클라이언트 사이드 추가 필터링 (서버에서 지원하지 않는 필터만)
    // 서버 필터: keyword, stack, location, experience, position, industry
    // 클라이언트 필터: salary, 다중 지역
    const getFilteredJobs = () => {
        let filteredJobs = [...jobs];
        const locationSelected = getSelectedArray("근무지");

        // 근무지 추가 필터링: 서버는 첫 번째 지역만 처리하므로, 다중 지역은 클라이언트에서 추가 필터링
        if (locationSelected.length > 1) {
            filteredJobs = filteredJobs.filter(job =>
                locationSelected.some(loc => job.location?.includes(loc))
            );
        }

        // 연봉 필터: 슬라이더 범위 (0~7 → 3,000만원~1억원+) - 클라이언트 사이드
        if (salaryRange[0] > 0 || salaryRange[1] < 7) {
            const minVal = 3000 + salaryRange[0] * 1000; // 만원: 0→3000, 1→4000, ..., 7→10000
            const maxVal = salaryRange[1] === 7 ? 99999 : 3000 + salaryRange[1] * 1000;
            filteredJobs = filteredJobs.filter(job => {
                const s = job.salaryDisplay?.replace(/[^0-9]/g, "") || "";
                if (!s) return true;
                const num = parseInt(s.slice(0, 5), 10) || 0; // 만원 단위 추정
                return num >= minVal && num <= maxVal;
            });
        }

        // 정렬: (1) 매칭율 우선 (2) 최신순
        filteredJobs.sort((a, b) => {
            const aRate = a.matchInfo?.overallMatchRate ?? a.matchInfo?.matchRate;
            const bRate = b.matchInfo?.overallMatchRate ?? b.matchInfo?.matchRate;
            if (aRate !== undefined && bRate !== undefined) {
                const rateDiff = (bRate || 0) - (aRate || 0);
                if (rateDiff !== 0) return rateDiff;
            }
            if (aRate !== undefined && bRate === undefined) return -1;
            if (aRate === undefined && bRate !== undefined) return 1;
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });

        return filteredJobs;
    };

    const filteredJobs = getFilteredJobs();

    const MULTI_SELECT_KEYS = ["스킬", "근무지", "서비스 분야", "포지션"];

    // 필터 적용 함수: 다중 선택 시 토글 (추가/제거), "전체" 시 비우기
    // 필터 선택 후에도 팝업은 열린 상태 유지 (닫기 버튼으로만 닫힘)
    const applyFilter = (filterKey: string, value: string) => {
        const newFilters = { ...selectedFilters };
        if (value === "전체" || value === "") {
            newFilters[filterKey] = MULTI_SELECT_KEYS.includes(filterKey) ? [] : "";
            setSelectedFilters(newFilters);
            setPage(0);
            return;
        }
        if (MULTI_SELECT_KEYS.includes(filterKey)) {
            const arr = getSelectedArray(filterKey);
            const next = arr.includes(value) ? arr.filter(x => x !== value) : [...arr, value];
            newFilters[filterKey] = next;
        } else {
            newFilters[filterKey] = value;
        }
        setSelectedFilters(newFilters);
        setPage(0);
    };


    return (
        <div className="min-h-screen bg-background text-foreground font-sans selection:bg-primary/20">
            <Sidebar />

            <div className="lg:pl-64 transition-all duration-300">
                <Header />

                <main className="container max-w-7xl mx-auto py-6 px-4 md:px-8">
                    {/* 상단 추천/광고 섹션 (탭 형태) */}
                    {isCompany ? <CompanyMarketingBanner /> : <RecommendedSection />}

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
                                            onClick={() => {
                                                setKeyword("");
                                                setPage(0);
                                                setActivePopup(null);
                                                setLocationDrillRegion(null);
                                                setExperienceRange([0, 10]);
                                                setSalaryRange([0, 7]);
                                                setSelectedFilters({
                                                    경력: [],
                                                    스킬: [],
                                                    연봉: [],
                                                    근무지: [],
                                                    "서비스 분야": [],
                                                    "포지션": [],
                                                });
                                            }}
                                        >
                                            <RotateCcw className="w-4 h-4" /> 초기화
                                        </Button>

                                        {Object.keys(FILTER_OPTIONS).map((key) => (
                                            <Button
                                                key={key}
                                                variant={activePopup === key ? "default" : "outline"}
                                                className={`rounded-full border-gray-300 font-normal px-4 h-10 shrink-0 transition-colors ${activePopup === key
                                                    ? "bg-gray-900 text-white border-transparent"
                                                    : selectedFilters[key] ? "bg-primary/10 text-primary border-primary"
                                                        : "bg-white text-gray-600 hover:bg-gray-50 hover:text-primary hover:border-primary"
                                                    }`}
                                                onClick={() => {
                                                    // 같은 필터를 다시 클릭해도 닫히지 않음 (열린 상태 유지)
                                                    // 다른 필터를 선택하면 해당 필터로 전환
                                                    setActivePopup(key);
                                                    if (key !== "근무지") setLocationDrillRegion(null);
                                                }}
                                            >
                                                {key} {(() => {
                                                    if (key === "경력" && (experienceRange[0] > 0 || experienceRange[1] < 10)) {
                                                        return <span className="ml-1 text-xs">({getExperienceLabel(experienceRange[0])}~{getExperienceLabel(experienceRange[1])})</span>;
                                                    }
                                                    if (key === "연봉" && (salaryRange[0] > 0 || salaryRange[1] < 7)) {
                                                        return <span className="ml-1 text-xs">({getSalaryLabel(salaryRange[0])}~{getSalaryLabel(salaryRange[1])})</span>;
                                                    }
                                                    const arr = getSelectedArray(key);
                                                    if (arr.length === 0) return null;
                                                    if (arr.length === 1) return <span className="ml-1 text-xs">({arr[0]})</span>;
                                                    return <span className="ml-1 text-xs">({arr.length}개)</span>;
                                                })()}
                                                <ChevronDown className={`ml-2 h-3 w-3 transition-transform ${activePopup === key ? "rotate-180" : ""} `} />
                                            </Button>
                                        ))}
                                    </div>
                                </div>

                                {/* Expanded Filter Detail Area */}
                                {activePopup && (
                                    <div className="pb-4 animate-in slide-in-from-top-2 fade-in duration-200">
                                        {/* 닫기 버튼 */}
                                        <div className="flex justify-end mb-2">
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="h-7 w-7 p-0 text-gray-400 hover:text-gray-600 hover:bg-gray-100"
                                                onClick={() => {
                                                    setActivePopup(null);
                                                    setLocationDrillRegion(null);
                                                }}
                                            >
                                                <X className="h-4 w-4" />
                                            </Button>
                                        </div>
                                        {(activePopup === "경력" || activePopup === "연봉") ? (
                                            <div className="p-4 bg-muted/30 rounded-xl border border-border">
                                                {activePopup === "경력" && (
                                                    <>
                                                        <div className="flex justify-between mb-4 text-sm font-medium text-muted-foreground">
                                                            <span>{getExperienceLabel(experienceRange[0])}</span>
                                                            <span className="text-primary font-bold">~</span>
                                                            <span>{getExperienceLabel(experienceRange[1])}</span>
                                                        </div>
                                                        <Slider
                                                            value={experienceRange}
                                                            onValueChange={(v) => { setExperienceRange(v); setPage(0); }}
                                                            min={0}
                                                            max={10}
                                                            step={1}
                                                            className="w-full"
                                                        />
                                                        <div className="flex justify-between mt-2 text-xs text-muted-foreground">
                                                            <span>신입</span>
                                                            <span>10년+</span>
                                                        </div>
                                                    </>
                                                )}
                                                {activePopup === "연봉" && (
                                                    <>
                                                        <div className="flex justify-between mb-4 text-sm font-medium text-muted-foreground">
                                                            <span>{getSalaryLabel(salaryRange[0])}</span>
                                                            <span className="text-primary font-bold">~</span>
                                                            <span>{getSalaryLabel(salaryRange[1])}</span>
                                                        </div>
                                                        <Slider
                                                            value={salaryRange}
                                                            onValueChange={(v) => { setSalaryRange(v); setPage(0); }}
                                                            min={0}
                                                            max={7}
                                                            step={1}
                                                            className="w-full"
                                                        />
                                                        <div className="flex justify-between mt-2 text-xs text-muted-foreground">
                                                            <span>3,000만원</span>
                                                            <span>1억원+</span>
                                                        </div>
                                                    </>
                                                )}
                                            </div>
                                        ) : activePopup === "근무지" ? (
                                            <div className="p-4 bg-muted/30 rounded-xl border border-border">
                                                {locationDrillRegion === null ? (
                                                    <>
                                                        <p className="text-xs font-medium text-muted-foreground mb-3">큰 도시(광역)를 선택하면 세부 지역을 고를 수 있어요</p>
                                                        <div className="flex flex-wrap gap-2">
                                                            <button
                                                                onClick={() => applyFilter("근무지", "")}
                                                                className={`px-3 py-1.5 rounded-lg text-sm transition-colors border ${getSelectedArray("근무지").length === 0 ? "bg-primary/10 border-primary text-primary font-bold" : "bg-muted/30 border-border text-muted-foreground hover:bg-muted/50 hover:border-primary/30"}`}
                                                            >
                                                                전체
                                                            </button>
                                                            {LOCATION_REGIONS.map(({ region }) => (
                                                                <button
                                                                    key={region}
                                                                    onClick={() => setLocationDrillRegion(region)}
                                                                    className="px-3 py-1.5 rounded-lg text-sm transition-colors border bg-muted/30 border-border text-muted-foreground hover:bg-muted/50 hover:border-primary/30"
                                                                >
                                                                    {region}
                                                                    <ChevronRight className="inline-block w-3.5 h-3.5 ml-0.5 align-middle" />
                                                                </button>
                                                            ))}
                                                        </div>
                                                    </>
                                                ) : (
                                                    <>
                                                        <button
                                                            type="button"
                                                            onClick={() => setLocationDrillRegion(null)}
                                                            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-primary mb-3"
                                                        >
                                                            <ChevronLeft className="w-4 h-4" /> {locationDrillRegion} 선택으로 돌아가기
                                                        </button>
                                                        <p className="text-xs font-medium text-muted-foreground mb-2">{locationDrillRegion} 세부 지역</p>
                                                        <div className="flex flex-wrap gap-2">
                                                            <button
                                                                onClick={() => {
                                                                    applyFilter("근무지", locationDrillRegion);
                                                                }}
                                                                className={`px-3 py-1.5 rounded-lg text-sm transition-colors border ${getSelectedArray("근무지").includes(locationDrillRegion) ? "bg-primary/10 border-primary text-primary font-bold" : "bg-muted/30 border-border text-muted-foreground hover:bg-muted/50 hover:border-primary/30"}`}
                                                            >
                                                                {locationDrillRegion} 전체
                                                            </button>
                                                            {LOCATION_REGIONS.find(r => r.region === locationDrillRegion)?.subRegions.map((sub) => {
                                                                const value = `${locationDrillRegion} ${sub}`;
                                                                const selected = getSelectedArray("근무지").includes(value);
                                                                return (
                                                                    <button
                                                                        key={sub}
                                                                        onClick={() => applyFilter("근무지", value)}
                                                                        className={`px-3 py-1.5 rounded-lg text-sm transition-colors border ${selected ? "bg-primary/10 border-primary text-primary font-bold" : "bg-muted/30 border-border text-muted-foreground hover:bg-muted/50 hover:border-primary/30"}`}
                                                                    >
                                                                        {sub}
                                                                    </button>
                                                                );
                                                            })}
                                                        </div>
                                                    </>
                                                )}
                                            </div>
                                        ) : FILTER_OPTIONS[activePopup]?.length > 0 ? (
                                            <div className="flex flex-wrap gap-2 p-4 bg-muted/30 rounded-xl border border-border">
                                                {FILTER_OPTIONS[activePopup].map((option) => {
                                                    const isMulti = MULTI_SELECT_KEYS.includes(activePopup);
                                                    const arr = getSelectedArray(activePopup);
                                                    const selected = isMulti
                                                        ? (option === "전체" ? arr.length === 0 : arr.includes(option))
                                                        : (selectedFilters[activePopup] === option || (option === "전체" && selectedFilters[activePopup] === ""));
                                                    return (
                                                        <button
                                                            key={option}
                                                            onClick={() => {
                                                                const val = option === "전체" ? "" : option;
                                                                applyFilter(activePopup, val);
                                                            }}
                                                            className={`px-3 py-1.5 rounded-lg text-sm transition-colors border ${selected
                                                                ? "bg-primary/10 border-primary text-primary font-bold"
                                                                : "bg-muted/30 border-border text-muted-foreground hover:bg-muted/50 hover:border-primary/30"
                                                                }`}
                                                        >
                                                            {option}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        ) : null}
                                    </div>
                                )}

                                {/* Count & Sort */}
                                <div className="flex items-center justify-between py-3">
                                    <div className="text-sm font-bold text-gray-900">
                                        {filteredJobs.length !== jobs.length ? (
                                            <>
                                                검색 결과 <span className="text-[#10B981]">{filteredJobs.length.toLocaleString()}</span>개
                                                <span className="text-gray-400 ml-2">(전체 {totalElements.toLocaleString()}개)</span>
                                            </>
                                        ) : (
                                            <>
                                                전체 <span className="text-[#10B981]">{totalElements.toLocaleString()}</span>개
                                            </>
                                        )}
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
                                    {isCompany ? (
                                        <>
                                            <p className="text-[#4E5968] text-sm tracking-tight">인재들에게 보낸 제안과 지원 현황을 한눈에 확인해보세요</p>
                                            <Link to="/company/dashboard" className="text-[#3B82F6] text-sm font-bold flex items-center hover:underline whitespace-nowrap">
                                                제안/신청 확인하기 <ChevronRight className="ml-0.5 h-4 w-4" />
                                            </Link>
                                        </>
                                    ) : (
                                        <>
                                            <p className="text-[#4E5968] text-sm tracking-tight">지금 프로필을 등록하면, 스카우트 제안을 받을 수 있어요</p>
                                            <Link to="/resume" className="text-[#3B82F6] text-sm font-bold flex items-center hover:underline whitespace-nowrap">
                                                내가 받을 스카우트 제안 보기 <ChevronRight className="ml-0.5 h-4 w-4" />
                                            </Link>
                                        </>
                                    )}
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

                            {/* 채용공고 리스트 (피드) - Wide Card Layout */}
                            {!isLoading && filteredJobs.length > 0 && (
                                <div className="grid grid-cols-1 gap-6">
                                    {filteredJobs.map((job) => (
                                        <WideJobCard
                                            key={job.jobId}
                                            id={job.jobId}
                                            company={job.companyName}
                                            logo={job.companyLogoUrl || job.companyName.charAt(0)}
                                            title={job.title}
                                            location={job.location}
                                            salary={job.salaryDisplay || "회사내규"}
                                            skills={Array.isArray(job.stack) ? job.stack : job.stack != null ? [String(job.stack)] : []}
                                            postedAt={new Date(job.createdAt).toLocaleDateString()}
                                            createdAt={job.createdAt}
                                            isAd={(job.adBidCredit ?? 0) > 0}
                                            matchScore={job.matchInfo?.overallMatchRate ?? job.matchInfo?.matchRate}
                                            requiredExperience={job.requiredExperience}
                                            companySummary={job.employer?.summary || job.employer?.description}
                                            employmentType="정규직"
                                        />
                                    ))}
                                </div>
                            )}

                            {/* 필터 결과 없음 */}
                            {!isLoading && filteredJobs.length === 0 && jobs.length > 0 && (
                                <div className="text-center py-20 bg-card rounded-3xl border border-dashed border-border">
                                    <p className="text-lg font-medium text-muted-foreground">선택한 필터 조건에 맞는 채용공고가 없습니다</p>
                                    <Button variant="link" onClick={() => {
                                        setExperienceRange([0, 10]);
                                        setSalaryRange([0, 7]);
                                        setSelectedFilters({
                                            경력: [],
                                            스킬: [],
                                            연봉: [],
                                            근무지: [],
                                            "서비스 분야": [],
                                            "포지션": [],
                                        });
                                        setLocationDrillRegion(null);
                                    }} className="mt-2 text-primary">
                                        필터 초기화
                                    </Button>
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

                        {/* 우측 사이드바 (4 cols) - 고정 */}
                        <div className="hidden lg:block lg:col-span-4">
                            <div className="sticky top-20 space-y-6">
                                {isCompany ? (
                                    <CompanySidebar />
                                ) : (
                                    <SideJobList />
                                )}

                                {/* 추가 위젯 (예: 인기 태그) */}
                                <div className="bg-card rounded-2xl border border-border p-5">
                                    <h3 className="font-bold mb-4 text-sm text-muted-foreground">인기 검색 키워드</h3>
                                    <div className="flex flex-wrap gap-2">
                                        {["Python", "Java", "React", "Spring Boot", "AI", "Data Engineer", "Frontend"].map(tag => (
                                            <button
                                                key={tag}
                                                onClick={() => {
                                                    setKeyword(tag);
                                                    setSearchParams({ keyword: tag });
                                                    setPage(0);
                                                }}
                                                className="px-3 py-1.5 rounded-lg bg-secondary/50 text-xs font-medium hover:bg-primary/10 hover:text-primary transition-colors"
                                            >
                                                #{tag}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>
                <Footer />
            </div>

            {/* Login Prompt Dialog */}
            <Dialog open={showLoginPrompt} onOpenChange={setShowLoginPrompt}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>로그인이 필요합니다</DialogTitle>
                        <DialogDescription>
                            더 많은 채용 공고와 상세 정보를 확인하시려면 로그인이 필요합니다.<br />
                            3초 만에 로그인하고 맞춤형 공고를 추천받아보세요!
                        </DialogDescription>
                    </DialogHeader>
                    <div className="flex justify-end gap-3 mt-4">
                        <Button variant="outline" onClick={() => setShowLoginPrompt(false)}>
                            구경하기
                        </Button>
                        <Button onClick={() => navigate("/auth")} className="bg-sky-500 hover:bg-sky-600">
                            로그인 하러가기
                        </Button>
                    </div>
                </DialogContent>
            </Dialog>
        </div>
    );
}
