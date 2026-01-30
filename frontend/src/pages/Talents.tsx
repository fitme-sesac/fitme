import { useState, useRef, useMemo } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Loader2, RotateCcw, ChevronDown, Check, ChevronRight, ChevronLeft, Sparkles, UserSearch } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Slider } from "@/components/ui/slider";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { WideTalentCard } from "@/components/home/WideTalentCard";
import { SideJobList } from "@/components/home/SideJobList"; // Reusing SideJobList or create a SideTalentList later
import { useAuth } from "@/contexts/AuthContext";
import { useApplicants } from "@/hooks/useEmployers"; // Assuming this or similar hook brings talent data

// Mock Data for initial view / fallback
const mockTalents = [
  { id: 1, name: "김서연", title: "Senior Frontend Developer", summary: "대규모 서비스 성능 최적화와 접근성 개선에 관심이 많은 프론트엔드 개발자입니다.", experience: "5년", location: "서울", education: "컴퓨터공학과 학사", skills: ["React", "TypeScript", "Next.js", "GraphQL"], salary: "8,000만 ~ 1억 2,000만원", matchScore: 95, avatar: null, lastUpdated: "1일 전", isNew: true },
  { id: 2, name: "박민준", title: "AI/ML Engineer", summary: "딥러닝과 MLOps를 활용한 서비스 적용 경험이 있으며, 실험 파이프라인 구축에 강점이 있습니다.", experience: "4년", location: "판교", education: "인공지능학과 석사", skills: ["Python", "TensorFlow", "PyTorch", "Deep Learning", "MLOps"], salary: "9,000만 ~ 1억 3,000만원", matchScore: 88, avatar: null, lastUpdated: "2일 전", isNew: false },
  { id: 3, name: "이하은", title: "Product Designer", summary: "사용자 리서치와 디자인 시스템 구축을 통해 제품 경험을 개선해 온 디자이너입니다.", experience: "6년", location: "서울", education: "시각디자인학과 학사", skills: ["Figma", "UI/UX", "Prototyping", "Design System"], salary: "7,500만 ~ 1억원", matchScore: 82, avatar: null, lastUpdated: "3일 전", isNew: true },
  { id: 4, name: "최준호", title: "Backend Developer", summary: "MSA와 클라우드 네이티브 아키텍처 설계 경험이 있으며, 고가용성 서비스를 지향합니다.", experience: "5년", location: "서울", education: "소프트웨어학과 학사", skills: ["Java", "Spring", "Kubernetes", "AWS", "MSA"], salary: "8,500만 ~ 1억 2,000만원", matchScore: 90, avatar: null, lastUpdated: "1일 전", isNew: false },
  { id: 5, name: "정수진", title: "DevOps Engineer", summary: "CI/CD와 인프라 자동화로 배포 안정성과 개발 생산성을 높이는 데 기여해 왔습니다.", experience: "4년", location: "경기 성남", education: "컴퓨터공학과 학사", skills: ["Node.js", "PostgreSQL", "Docker", "Terraform", "CI/CD", "Monitoring"], salary: "8,000만 ~ 1억 1,000만원", matchScore: 85, avatar: null, lastUpdated: "2일 전", isNew: true },
  { id: 6, name: "한지우", title: "Data Engineer", summary: "대용량 데이터 파이프라인과 실시간 스트리밍 구축 경험이 있습니다.", experience: "5년", location: "판교", education: "통계학과 석사", skills: ["Python", "Spark", "Airflow", "Kafka", "BigQuery"], salary: "9,000만 ~ 1억 2,000만원", matchScore: 87, avatar: null, lastUpdated: "4일 전", isNew: false },
];

const FILTER_OPTIONS: Record<string, string[]> = {
  "포지션": ["프론트엔드", "백엔드", "풀스택", "Android", "iOS", "DevOps", "데이터 엔지니어", "AI/ML", "PM/PO", "디자이너"],
  "경력": [], // Slider
  "스킬": ["Java", "Python", "JavaScript", "TypeScript", "React", "Vue", "Spring", "Node.js", "AWS", "Docker", "Figma"],
  "희망 연봉": [], // Slider
  "지역": ["서울", "판교", "강남", "서초", "송파", "경기", "인천", "대전", "대구", "부산", "광주", "제주"],
};

const EXPERIENCE_LABELS = ["신입", "1년", "2년", "3년", "4년", "5년", "6년", "7년", "8년", "9년", "10년+"];
const getExperienceLabel = (value: number) => EXPERIENCE_LABELS[value] ?? "신입";
const SALARY_LABELS = ["3,000만원", "4,000만원", "5,000만원", "6,000만원", "7,000만원", "8,000만원", "9,000만원", "1억원+"];
const getSalaryLabel = (value: number) => SALARY_LABELS[value] ?? "3,000만원";

export default function Talents() {
  const { isCompany } = useAuth();
  // In a real scenario, useApplicants or a similar hook would fetch talents
  const { data: applicantsData, isLoading } = useApplicants("");

  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [activePopup, setActivePopup] = useState<string | null>(null);
  const filterScrollRef = useRef<HTMLDivElement>(null);

  // Sliders
  const [experienceRange, setExperienceRange] = useState<number[]>([0, 10]);
  const [salaryRange, setSalaryRange] = useState<number[]>([0, 7]);

  // Filters
  type FilterValue = string | string[];
  const [selectedFilters, setSelectedFilters] = useState<Record<string, FilterValue>>({
    "포지션": [],
    "경력": [],
    "스킬": [],
    "희망 연봉": [],
    "지역": [],
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

  const MULTI_SELECT_KEYS = ["포지션", "스킬", "지역"];

  const applyFilter = (filterKey: string, value: string) => {
    const newFilters = { ...selectedFilters };
    if (value === "전체") {
      newFilters[filterKey] = [];
      setSelectedFilters(newFilters);
      setActivePopup(null);
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
    setActivePopup(null);
  };

  // Data Processing (Mock + Applicants)
  const displayTalents = useMemo(() => {
    // Simple logic: if company has applicants, maybe mix them or show them. 
    // For 'Talent Pool' page, it usually shows ALL public talents, not just applicants.
    // We will stick to 'mockTalents' for the 'pool' visualization if no API for public talents exists yet.
    // Or if 'applicantsData' is effectively the 'talent pool' in this context. Use mockTalents as base.

    let talents = [...mockTalents];

    // Filter Logic Implementation (Client-side for mock)
    const posSelected = getSelectedArray("포지션");
    const skillSelected = getSelectedArray("스킬");
    const locSelected = getSelectedArray("지역");

    if (posSelected.length > 0) {
      talents = talents.filter(t => posSelected.some(p => t.title.includes(p) || t.summary.includes(p)));
    }
    if (skillSelected.length > 0) {
      talents = talents.filter(t => skillSelected.some(s => t.skills.some(ts => ts.toLowerCase().includes(s.toLowerCase()))));
    }
    if (locSelected.length > 0) {
      talents = talents.filter(t => locSelected.some(l => t.location.includes(l)));
    }

    // Experience Filter (Approximate parsing for mock strings like "5년")
    if (experienceRange[0] > 0 || experienceRange[1] < 10) {
      talents = talents.filter(t => {
        const expNum = parseInt(t.experience.replace(/[^0-9]/g, "")) || 0; // "5년" -> 5
        // Handle "신입" -> 0
        const flowExp = t.experience.includes("신입") ? 0 : expNum;
        return flowExp >= experienceRange[0] && flowExp <= experienceRange[1];
      });
    }

    return talents;
  }, [selectedFilters, experienceRange, mockTalents]);

  const resetFilters = () => {
    setExperienceRange([0, 10]);
    setSalaryRange([0, 7]);
    setSelectedFilters({
      "포지션": [],
      "경력": [],
      "스킬": [],
      "희망 연봉": [],
      "지역": [],
    });
    setActivePopup(null);
  };

  return (
    <div className="min-h-screen bg-background text-foreground font-sans selection:bg-primary/20">
      <Sidebar />

      <div className="lg:pl-64 transition-all duration-300">
        <Header />

        <main className="container max-w-7xl mx-auto py-6 px-4 md:px-8">
          {/* Top Banner similar to Jobs RecommendedSection but for Talents */}
          <div className="mb-8 p-6 rounded-3xl bg-gradient-to-r from-indigo-50 to-blue-50 border border-blue-100 flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-blue-600 font-bold">
                <Sparkles className="h-5 w-5" />
                <span>AI 인재 매칭</span>
              </div>
              <h1 className="text-2xl md:text-3xl font-bold text-gray-900">
                우리 회사에 딱 맞는 <span className="text-primary relative inline-block">
                  인재
                  <svg className="absolute -bottom-1 left-0 w-full h-1.5 text-blue-200 fill-current -z-10" viewBox="0 0 100 10" preserveAspectRatio="none">
                    <path d="M0 5 Q 50 10 100 5 L 100 10 L 0 10 Z" />
                  </svg>
                </span>를 찾아보세요
              </h1>
              <p className="text-muted-foreground">
                48시간 이내 응답률 95%! 적극적으로 구직 중인 인재들이에요.
              </p>
            </div>
            <div className="flex-shrink-0">
              <Button size="lg" className="rounded-full shadow-lg bg-white text-primary border border-blue-200 hover:bg-blue-50" asChild>
                <Link to="/company/dashboard">
                  <UserSearch className="mr-2 h-5 w-5" />
                  내 공고와 매칭된 인재 보기
                </Link>
              </Button>
            </div>
          </div>


          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-8">
            {/* Center Feed (8 cols) */}
            <div className="lg:col-span-8 space-y-6">

              {/* Sticky Filter Bar */}
              <div className="sticky top-16 z-30 bg-background/95 backdrop-blur-sm -mx-4 px-4 md:mx-0 md:px-0 mb-6 border-b border-border/40">
                <div className="py-4 relative group">
                  {/* Scroll Buttons */}
                  <button onClick={() => scrollFilter("left")} className="absolute left-0 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-white/90 backdrop-blur shadow-md rounded-full flex items-center justify-center text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:scale-110 disabled:opacity-0 border border-gray-100">
                    <ChevronLeft className="w-5 h-5" />
                  </button>
                  <button onClick={() => scrollFilter("right")} className="absolute right-0 top-1/2 -translate-y-1/2 z-20 w-8 h-8 bg-white/90 backdrop-blur shadow-md rounded-full flex items-center justify-center text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:scale-110 border border-gray-100">
                    <ChevronRight className="w-5 h-5" />
                  </button>

                  {/* Filter List */}
                  <div ref={filterScrollRef} className="flex items-center gap-2 overflow-x-auto pb-1 px-8 scrollbar-hide [&::-webkit-scrollbar]:hidden scroll-smooth" style={{ scrollbarWidth: 'none' }}>
                    <Button
                      variant="ghost"
                      className="rounded-full shrink-0 h-10 px-4 text-muted-foreground hover:text-primary hover:bg-primary/10 font-bold flex items-center gap-1.5 mr-2"
                      onClick={resetFilters}
                    >
                      <RotateCcw className="w-4 h-4" /> 초기화
                    </Button>

                    {Object.keys(FILTER_OPTIONS).map((key) => (
                      <Button
                        key={key}
                        variant={activePopup === key ? "default" : "outline"}
                        className={`rounded-full border-gray-300 font-normal px-4 h-10 shrink-0 transition-colors ${activePopup === key
                            ? "bg-gray-900 text-white border-transparent"
                            : getSelectedArray(key).length > 0 || (key === "경력" && (experienceRange[0] > 0 || experienceRange[1] < 10)) || (key === "희망 연봉" && (salaryRange[0] > 0 || salaryRange[1] < 7))
                              ? "bg-primary/10 text-primary border-primary"
                              : "bg-white text-gray-600 hover:bg-gray-50 hover:text-primary hover:border-primary"
                          }`}
                        onClick={() => setActivePopup(activePopup === key ? null : key)}
                      >
                        {key}
                        {/* Filter Status Badge */}
                        {(() => {
                          if (key === "경력" && (experienceRange[0] > 0 || experienceRange[1] < 10)) {
                            return <span className="ml-1 text-xs">({getExperienceLabel(experienceRange[0])}~{getExperienceLabel(experienceRange[1])})</span>;
                          }
                          if (key === "희망 연봉" && (salaryRange[0] > 0 || salaryRange[1] < 7)) {
                            return <span className="ml-1 text-xs">({getSalaryLabel(salaryRange[0])}~{getSalaryLabel(salaryRange[1])})</span>;
                          }
                          const arr = getSelectedArray(key);
                          if (arr.length === 0) return null;
                          if (arr.length === 1) return <span className="ml-1 text-xs">({arr[0]})</span>;
                          return <span className="ml-1 text-xs">({arr.length}개)</span>;
                        })()}
                        <ChevronDown className={`ml-2 h-3 w-3 transition-transform ${activePopup === key ? "rotate-180" : ""}`} />
                      </Button>
                    ))}
                  </div>
                </div>

                {/* Expanded Filter Detail Area */}
                {activePopup && (
                  <div className="pb-4 animate-in slide-in-from-top-2 fade-in duration-200">
                    {(activePopup === "경력") && (
                      <div className="p-4 bg-muted/30 rounded-xl border border-border">
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
                      </div>
                    )}
                    {(activePopup === "희망 연봉") && (
                      <div className="p-4 bg-muted/30 rounded-xl border border-border">
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
                      </div>
                    )}
                    {(activePopup !== "경력" && activePopup !== "희망 연봉" && FILTER_OPTIONS[activePopup]) && (
                      <div className="flex flex-wrap gap-2 p-4 bg-muted/30 rounded-xl border border-border">
                        <button
                          onClick={() => applyFilter(activePopup, "전체")}
                          className={`px-3 py-1.5 rounded-lg text-sm transition-colors border ${getSelectedArray(activePopup).length === 0 ? "bg-primary/10 border-primary text-primary font-bold" : "bg-muted/30 border-border text-muted-foreground hover:bg-muted/50 hover:border-primary/30"}`}
                        >
                          전체
                        </button>
                        {FILTER_OPTIONS[activePopup].map((option) => {
                          const selected = getSelectedArray(activePopup).includes(option);
                          return (
                            <button
                              key={option}
                              onClick={() => applyFilter(activePopup, option)}
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
                    )}
                  </div>
                )}
              </div>

              {/* Talent Count */}
              <div className="flex items-center justify-between">
                <div className="text-sm font-bold text-gray-900">
                  검색 결과 <span className="text-[#10B981]">{displayTalents.length}</span>명
                </div>
                <div className="flex items-center gap-4 text-xs sm:text-sm">
                  <button className="text-gray-400 hover:text-gray-600 transition-colors">최신순</button>
                  <button className="text-gray-900 font-bold flex items-center gap-1">
                    <Check className="h-3 w-3 text-[#10B981]" /> 매칭률순
                  </button>
                </div>
              </div>

              {/* Talent Feed */}
              <div className="grid grid-cols-1 gap-6">
                {displayTalents.map((talent) => (
                  <WideTalentCard
                    key={talent.id}
                    {...talent}
                  />
                ))}
                {displayTalents.length === 0 && (
                  <div className="text-center py-20 bg-card rounded-3xl border border-dashed border-border">
                    <p className="text-lg font-medium text-muted-foreground">조건에 맞는 인재가 없습니다.</p>
                    <Button variant="link" onClick={resetFilters} className="mt-2 text-primary">
                      필터 초기화
                    </Button>
                  </div>
                )}
              </div>

            </div>

            {/* Right Sidebar (4 cols) */}
            <div className="hidden lg:block lg:col-span-4">
              <div className="sticky top-20 space-y-6">
                {/* Can reuse SideJobList or create SideTalentList/Banners */}
                <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-3xl p-6 text-white shadow-xl">
                  <h3 className="text-lg font-bold mb-2">프리미엄 인재 열람권</h3>
                  <p className="text-gray-300 text-sm mb-4">
                    검증된 Top 5% 인재의 연락처를 확인하고<br />직접 스카우트 제안을 보내보세요.
                  </p>
                  <Button className="w-full bg-white text-gray-900 hover:bg-gray-100 font-bold">
                    열람권 구매하기
                  </Button>
                </div>

                <SideJobList />
              </div>
            </div>
          </div>
        </main>
        <Footer />
      </div>
    </div>
  );
}
