import { useState, useRef, useMemo, useEffect } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Loader2, RotateCcw, ChevronDown, Check, ChevronRight, ChevronLeft, Sparkles, UserSearch, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Slider } from "@/components/ui/slider";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { WideTalentCard } from "@/components/home/WideTalentCard";
import { SideJobList } from "@/components/home/SideJobList";
import { useAuth } from "@/contexts/AuthContext";
import { getTalents, type TalentListItem } from "@/api/talents";
import { useFilterOptions } from "@/hooks/useJobs";

// 기본 필터 옵션 (DB에서 로드 전 표시용)
const DEFAULT_SKILL_OPTIONS = ["전체", "Java", "Python", "JavaScript", "TypeScript", "React", "Vue", "Spring", "Node.js", "Django", "AWS", "Docker", "Kubernetes"];

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

export default function Talents() {
  const { isCompany } = useAuth();
  const [talents, setTalents] = useState<TalentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);

  const [searchParams, setSearchParams] = useSearchParams();
  const [activePopup, setActivePopup] = useState<string | null>(null);
  const [locationDrillRegion, setLocationDrillRegion] = useState<string | null>(null);
  const filterScrollRef = useRef<HTMLDivElement>(null);

  // 경력·연봉: 슬라이더로만 관리
  const [experienceRange, setExperienceRange] = useState<number[]>([0, 10]);
  const [salaryRange, setSalaryRange] = useState<number[]>([0, 7]);

  // 필터 상태: 다중 선택은 string[], 경력/연봉은 슬라이더 사용
  // 인재풀에는 "서비스 분야", "포지션" 필터 없음
  type FilterValue = string | string[];
  const [selectedFilters, setSelectedFilters] = useState<Record<string, FilterValue>>({
    경력: [],
    스킬: [],
    연봉: [],
    근무지: [],
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

  // 필터 옵션 (DB 데이터 우선, 없으면 기본값 사용)
  // 인재풀에는 "서비스 분야", "포지션" 필터 없음
  const FILTER_OPTIONS: Record<string, string[]> = useMemo(() => ({
    "경력": [], // 슬라이더로 관리
    "스킬": filterOptionsData?.stacks?.length > 0
      ? ["전체", ...filterOptionsData.stacks]
      : DEFAULT_SKILL_OPTIONS,
    "연봉": [], // 슬라이더로 관리
    "근무지": [], // 2단계(광역→세부) UI로 별도 렌더
  }), [filterOptionsData]);

  const MULTI_SELECT_KEYS = ["스킬", "근무지"];

  useEffect(() => {
    setLoading(true);
    getTalents(page, 12)
      .then((res) => {
        setTalents(res.content);
        setTotalElements(res.totalElements);
      })
      .catch(() => setTalents([]))
      .finally(() => setLoading(false));
  }, [page]);

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

  // 스킬 문자열에서 JSON/PostgreSQL 배열 문자 제거
  const cleanSkill = (skill: string): string => {
    if (!skill) return "";
    return skill.replace(/[{}"']/g, "").trim();
  };

  // 스킬 배열 정리 (JSON 문자 제거)
  const cleanSkills = (skills: string[]): string[] => {
    return skills.map(cleanSkill).filter(s => s.length > 0);
  };

  // 클라이언트 필터 (스킬, 근무지, 경력, 연봉)
  const displayTalents = useMemo(() => {
    let list = [...talents];

    // 스킬 필터 (JSON 문자 정리 후 비교)
    const skillSelected = getSelectedArray("스킬");
    if (skillSelected.length > 0) {
      list = list.filter(t => {
        const cleaned = cleanSkills(t.skills || []);
        return cleaned.some(ts => skillSelected.some(s => ts.toLowerCase().includes(s.toLowerCase())));
      });
    }

    // 근무지 필터
    const locSelected = getSelectedArray("근무지");
    if (locSelected.length > 0) {
      list = list.filter(t => locSelected.some(l => (t.location || "").includes(l)));
    }

    // 경력 필터
    if (experienceRange[0] > 0 || experienceRange[1] < 10) {
      list = list.filter(t => {
        const expNum = parseInt(String(t.experience || "0").replace(/[^0-9]/g, "")) || 0;
        const flowExp = String(t.experience || "").includes("신입") ? 0 : expNum;
        return flowExp >= experienceRange[0] && flowExp <= experienceRange[1];
      });
    }

    // 연봉 필터
    if (salaryRange[0] > 0 || salaryRange[1] < 7) {
      const minVal = 3000 + salaryRange[0] * 1000;
      const maxVal = salaryRange[1] === 7 ? 99999 : 3000 + salaryRange[1] * 1000;
      list = list.filter(t => {
        const s = t.salary?.replace(/[^0-9]/g, "") || "";
        if (!s) return true;
        const num = parseInt(s.slice(0, 5), 10) || 0;
        return num >= minVal && num <= maxVal;
      });
    }

    return list;
  }, [selectedFilters, experienceRange, salaryRange, talents]);

  const resetFilters = () => {
    setExperienceRange([0, 10]);
    setSalaryRange([0, 7]);
    setSelectedFilters({
      경력: [],
      스킬: [],
      연봉: [],
      근무지: [],
    });
    setActivePopup(null);
    setLocationDrillRegion(null);
  };

  return (
    <div className="min-h-screen bg-background text-foreground font-sans selection:bg-primary/20">
      <Sidebar />

      <div className="lg:pl-64 transition-all duration-300">
        <Header />

        <main className="container max-w-7xl mx-auto py-6 px-4 md:px-8">
          {/* Top Banner */}
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

                  {/* Filter List */}
                  <div
                    ref={filterScrollRef}
                    className="flex items-center gap-2 overflow-x-auto pb-1 px-8 scrollbar-hide [&::-webkit-scrollbar]:hidden scroll-smooth"
                    style={{ scrollbarWidth: 'none' }}
                  >
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

                {/* Talent Count */}
                <div className="flex items-center justify-between py-3">
                  <div className="text-sm font-bold text-gray-900">
                    {loading ? (
                      <span className="flex items-center gap-2"><Loader2 className="h-4 w-4 animate-spin" /> 로딩 중...</span>
                    ) : displayTalents.length !== talents.length ? (
                      <>
                        검색 결과 <span className="text-[#10B981]">{displayTalents.length.toLocaleString()}</span>명
                        <span className="text-gray-400 ml-2">(전체 {totalElements.toLocaleString()}명)</span>
                      </>
                    ) : (
                      <>전체 <span className="text-[#10B981]">{totalElements.toLocaleString()}</span>명</>
                    )}
                  </div>
                  <div className="flex items-center gap-4 text-xs sm:text-sm">
                    <button className="text-gray-400 hover:text-gray-600 transition-colors">최신순</button>
                    <button className="text-gray-900 font-bold flex items-center gap-1">
                      <Check className="h-3 w-3 text-[#10B981]" /> 매칭률순
                    </button>
                  </div>
                </div>
              </div>

              {/* Talent Feed */}
              <div className="grid grid-cols-1 gap-6">
                {loading ? (
                  <div className="flex flex-col items-center justify-center py-20 space-y-4">
                    <Loader2 className="h-10 w-10 animate-spin text-primary" />
                    <p className="text-muted-foreground animate-pulse">AI가 딱 맞는 인재를 찾고 있어요...</p>
                  </div>
                ) : (
                  displayTalents.map((talent) => (
                    <WideTalentCard
                      key={talent.resumeId ?? talent.id}
                      {...talent}
                      matchScore={talent.matchScore ?? 0}
                    />
                  ))
                )}
                {!loading && displayTalents.length === 0 && (
                  <div className="text-center py-20 bg-card rounded-3xl border border-dashed border-border">
                    <p className="text-lg font-medium text-muted-foreground">선택한 필터 조건에 맞는 인재가 없습니다</p>
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
