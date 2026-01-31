import { useState, useEffect } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from "@/components/ui/accordion";
import {
    Search,
    HelpCircle,
    MessageSquare,
    Mail,
    Phone,
    Clock,
    ChevronRight,
    Loader2,
    FileQuestion,
    Headphones,
    BookOpen,
    Shield,
    FileText,
    AlertCircle,
    MapPin,
    Target
} from "lucide-react";
import { Link } from "react-router-dom";

// API base URL
const API_BASE = "http://localhost:8080/api/v1";

interface FAQ {
    id: number;
    question: string;
    answer?: string;
    isPublic?: boolean;
    createdAt: string;
    updatedAt?: string;
}

interface FAQPage {
    content: FAQ[];
    totalPages: number;
    totalElements: number;
    number: number;
}

// 메뉴 그룹 정의
const MENU_GROUPS = [
    {
        title: "고객지원",
        items: [
            { id: "notice", label: "공지사항", icon: FileText },
            { id: "faq", label: "자주 묻는 질문 (FAQ)", icon: HelpCircle },
            { id: "inquiry", label: "1:1 문의하기", icon: MessageSquare },
        ]
    },
    {
        title: "약관 및 정책",
        items: [
            { id: "terms", label: "서비스 이용약관", icon: Shield },
            { id: "privacy", label: "개인정보처리방침", icon: Shield },
            { id: "location", label: "위치기반서비스 이용약관", icon: MapPin },
            { id: "youth", label: "청소년보호정책", icon: Shield },
            { id: "deletion", label: "게시물 삭제 규정", icon: AlertCircle },
            { id: "cctv", label: "영상기기 운영방침", icon: AlertCircle },
        ]
    }
];

export default function Support() {
    const [activeTab, setActiveTab] = useState("faq");
    const [faqs, setFaqs] = useState<FAQ[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");
    const [searchResults, setSearchResults] = useState<FAQ[] | null>(null);

    // FAQ 목록 조회
    useEffect(() => {
        if (activeTab === 'faq') {
            fetchFAQs();
        }
    }, [activeTab]);

    const fetchFAQs = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${API_BASE}/faqs?page=0&size=20`);
            if (response.ok) {
                const data: FAQPage = await response.json();
                setFaqs(data.content || []);
            }
        } catch (error) {
            console.error("FAQ 조회 실패:", error);
            // Mock data fallback
            setFaqs([
                { id: 1, question: "회원가입은 어떻게 하나요?", answer: "홈페이지 우측 상단의 '회원가입' 버튼을 클릭하여 이메일 또는 소셜 계정으로 가입할 수 있습니다.", createdAt: new Date().toISOString() },
                { id: 2, question: "비밀번호를 잊어버렸어요.", answer: "로그인 페이지에서 '비밀번호 찾기'를 클릭하시면 등록된 이메일로 비밀번호 재설정 링크가 발송됩니다.", createdAt: new Date().toISOString() },
                { id: 3, question: "채용공고는 어떻게 등록하나요?", answer: "기업 회원으로 로그인 후 '채용공고 등록' 메뉴에서 공고를 작성하실 수 있습니다.", createdAt: new Date().toISOString() },
                { id: 4, question: "이력서 수정은 어떻게 하나요?", answer: "마이페이지 > 이력서 관리에서 등록된 이력서를 수정할 수 있습니다.", createdAt: new Date().toISOString() },
                { id: 5, question: "지원한 공고 내역은 어디서 확인하나요?", answer: "마이페이지 > 지원 현황에서 지원한 모든 공고의 진행 상태를 확인할 수 있습니다.", createdAt: new Date().toISOString() },
            ]);
        } finally {
            setIsLoading(false);
        }
    };

    // FAQ 검색
    const handleSearch = async () => {
        if (!searchQuery.trim()) {
            setSearchResults(null);
            return;
        }

        setIsLoading(true);
        if (activeTab !== 'faq') setActiveTab('faq');

        try {
            const response = await fetch(
                `${API_BASE}/faqs/search-full?keyword=${encodeURIComponent(searchQuery)}&page=0&size=20`
            );
            if (response.ok) {
                const data: FAQPage = await response.json();
                setSearchResults(data.content || []);
            }
        } catch (error) {
            console.error("FAQ 검색 실패:", error);
            const filtered = faqs.filter(
                faq =>
                    faq.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
                    (faq.answer && faq.answer.toLowerCase().includes(searchQuery.toLowerCase()))
            );
            setSearchResults(filtered);
        } finally {
            setIsLoading(false);
        }
    };

    const displayedFAQs = searchResults !== null ? searchResults : faqs;

    const renderContent = () => {
        // 공통 헤더 스타일
        const ContentHeader = ({ title, date, icon: Icon }: { title: string, date?: string, icon?: any }) => (
            <div className="mb-8 border-b pb-6">
                <div className="flex items-center gap-3 mb-4">
                    {Icon && (
                        <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-sky-100 dark:bg-sky-900/20">
                            <Icon className="h-6 w-6 text-sky-600" />
                        </div>
                    )}
                    <div>
                        <h2 className="text-3xl font-bold text-foreground tracking-tight">{title}</h2>
                        {date && <p className="text-sm text-muted-foreground mt-1">{date} 기준</p>}
                    </div>
                </div>
            </div>
        );

        switch (activeTab) {
            case "faq":
                return (
                    <div className="bg-card rounded-2xl shadow-sm border p-8 min-h-[600px]">
                        <ContentHeader title="자주 묻는 질문" icon={HelpCircle} />

                        {/* FAQ 검색 및 필터 */}
                        <div className="mb-6 relative w-full">
                            <Input
                                type="text"
                                placeholder="질문을 검색해보세요..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                                className="pl-11 h-12 rounded-xl border-border bg-muted/30 focus:bg-background transition-colors"
                            />
                            <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                        </div>

                        {searchResults !== null && (
                            <div className="mb-4 flex items-center justify-between">
                                <p className="text-sm text-muted-foreground">
                                    "{searchQuery}" 검색 결과 <span className="font-bold text-foreground">{searchResults.length}건</span>
                                </p>
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => {
                                        setSearchQuery("");
                                        setSearchResults(null);
                                    }}
                                >
                                    전체 보기
                                </Button>
                            </div>
                        )}

                        {isLoading ? (
                            <div className="flex justify-center py-12">
                                <Loader2 className="h-8 w-8 animate-spin text-sky-500" />
                            </div>
                        ) : displayedFAQs.length > 0 ? (
                            <Accordion type="single" collapsible className="w-full space-y-2">
                                {displayedFAQs.map((faq) => (
                                    <AccordionItem key={faq.id} value={`faq-${faq.id}`} className="border rounded-xl px-4 hover:bg-muted/30 transition-colors">
                                        <AccordionTrigger className="text-left hover:text-sky-600 hover:no-underline py-4 text-base font-medium">
                                            <span className="flex items-center gap-3">
                                                <span className="text-sky-600 font-bold">Q.</span>
                                                {faq.question}
                                            </span>
                                        </AccordionTrigger>
                                        <AccordionContent className="text-muted-foreground whitespace-pre-wrap leading-relaxed pb-6 pl-8">
                                            {faq.answer || "답변 준비 중입니다."}
                                        </AccordionContent>
                                    </AccordionItem>
                                ))}
                            </Accordion>
                        ) : (
                            <div className="text-center py-20 bg-muted/30 rounded-xl">
                                <HelpCircle className="h-10 w-10 text-muted-foreground/50 mx-auto mb-3" />
                                <p className="text-muted-foreground">찾으시는 결과가 없습니다.</p>
                            </div>
                        )}
                    </div>
                );
            case "notice":
                return (
                    <div className="bg-card rounded-2xl shadow-sm border p-8 min-h-[600px]">
                        <ContentHeader title="공지사항" icon={FileText} />
                        <div className="space-y-4">
                            {[1, 2, 3].map((i) => (
                                <div key={i} className="group p-5 border rounded-xl hover:border-sky-200 hover:shadow-md transition-all cursor-pointer">
                                    <div className="flex items-start justify-between mb-2">
                                        <div className="flex items-center gap-2">
                                            <Badge variant="secondary" className="bg-sky-100 text-sky-700 hover:bg-sky-200 border-0">중요</Badge>
                                            <h3 className="font-semibold text-lg group-hover:text-sky-600 transition-colors">2024년 개인정보처리방침 개정 안내</h3>
                                        </div>
                                    </div>
                                    <p className="text-muted-foreground line-clamp-2 mb-3">
                                        안녕하세요. FitMe 팀입니다. 2024년 2월 1일자로 개인정보처리방침이 일부 개정될 예정입니다. 주요 변경 사항은 다음과 같습니다...
                                    </p>
                                    <span className="text-sm text-muted-foreground/70">2024.01.15</span>
                                </div>
                            ))}
                        </div>
                    </div>
                );
            case "inquiry":
                return (
                    <div className="bg-card rounded-2xl shadow-sm border p-8 min-h-[600px]">
                        <ContentHeader title="문의/제안/신고" icon={MessageSquare} />
                        <div className="max-w-2xl">
                            <div className="bg-muted/30 p-6 rounded-xl mb-8">
                                <h4 className="font-semibold mb-2">문의 전 확인해주세요!</h4>
                                <ul className="text-sm text-muted-foreground space-y-1 list-disc list-inside">
                                    <li>자주 묻는 질문(FAQ)에서 궁금한 내용을 먼저 확인하실 수 있습니다.</li>
                                    <li>평일 09:00 ~ 18:00 (점심시간 12:00 ~ 13:00) 외에는 답변이 늦어질 수 있습니다.</li>
                                </ul>
                            </div>

                            <div className="space-y-6">
                                <div className="grid grid-cols-2 gap-6">
                                    <div className="space-y-2">
                                        <label className="text-sm font-medium">이름</label>
                                        <Input placeholder="홍길동" className="h-11 rounded-lg" />
                                    </div>
                                    <div className="space-y-2">
                                        <label className="text-sm font-medium">이메일</label>
                                        <Input placeholder="example@email.com" className="h-11 rounded-lg" />
                                    </div>
                                </div>
                                <div className="space-y-2">
                                    <label className="text-sm font-medium">문의 유형</label>
                                    <select className="flex h-11 w-full rounded-lg border border-input bg-background px-3 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2">
                                        <option>서비스 이용 문의</option>
                                        <option>개선 제안</option>
                                        <option>오류/버그 신고</option>
                                        <option>기타</option>
                                    </select>
                                </div>
                                <div className="space-y-2">
                                    <label className="text-sm font-medium">내용</label>
                                    <textarea
                                        className="flex min-h-[200px] w-full rounded-lg border border-input bg-background px-4 py-3 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 resize-none"
                                        placeholder="문의 내용을 상세히 적어주시면 더 정확한 답변이 가능합니다."
                                    />
                                </div>
                                <div className="flex justify-end pt-4">
                                    <Button className="h-11 px-8 bg-gradient-to-r from-sky-500 to-teal-400 hover:from-sky-600 hover:to-teal-500 text-white">
                                        문의 접수하기
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </div>
                );
            default:
                // 약관/정책 페이지 템플릿
                const allItems = MENU_GROUPS.flatMap(g => g.items);
                const currentMenu = allItems.find(item => item.id === activeTab);

                return (
                    <div className="bg-card rounded-2xl shadow-sm border p-8 min-h-[600px]">
                        <ContentHeader
                            title={currentMenu?.label || "약관/정책"}
                            icon={currentMenu?.icon}
                            date="2024년 1월 1일 개정"
                        />
                        <div className="prose max-w-none prose-gray dark:prose-invert">
                            <div className="bg-muted/30 p-6 rounded-xl border border-border/50 text-muted-foreground leading-relaxed whitespace-pre-line">
                                <p className="font-semibold text-foreground mb-4">제 1 조 (목적)</p>
                                <p>본 약관은 FitMe(이하 "회사")가 제공하는 채용 플랫폼 서비스의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.</p>
                                <br />
                                <p className="font-semibold text-foreground mb-4">제 2 조 (용어의 정의)</p>
                                <p>1. "서비스"라 함은 구현되는 단말기와 상관없이 "회원"이 이용할 수 있는 FitMe 및 FitMe 관련 제반 서비스를 의미합니다.</p>
                                <p>2. "회원"이라 함은 회사의 "서비스"에 접속하여 이 약관에 따라 "회사"와 이용계약을 체결하고 "회사"가 제공하는 "서비스"를 이용하는 고객을 말합니다.</p>
                                <br />
                                <p className="text-sm text-muted-foreground mt-8">
                                    * 실제 서비스에서는 해당 {currentMenu?.label}의 전체 내용이 표시됩니다.
                                </p>
                            </div>
                        </div>
                    </div>
                );
        }
    };

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />

            <div className="lg:pl-64 transition-all duration-300">
                <Header />

                <main className="container max-w-5xl mx-auto py-12 px-4 md:px-8">
                    {/* Page Title Area (RocketPunch Style) */}
                    <div className="flex items-end justify-between mb-8 border-b pb-4">
                        <div className="flex items-center gap-3">
                            <div className="flex items-center justify-center w-10 h-10 rounded-lg text-white" style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }}>
                                <Target className="h-6 w-6" />
                            </div>
                            <h1 className="text-2xl font-bold">고객센터</h1>
                        </div>
                        <div className="text-sm text-muted-foreground pb-1">
                            FitMe 서비스 이용에 도움이 필요하신가요?
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
                        {/* Sidebar Menu - Left Column */}
                        <div className="lg:col-span-1">
                            <div className="sticky top-24 space-y-8">

                                {MENU_GROUPS.map((group, groupIndex) => (
                                    <div key={groupIndex}>
                                        <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3 px-4">
                                            {group.title}
                                        </h3>
                                        <div className="space-y-1">
                                            {group.items.map((item) => {
                                                const isActive = activeTab === item.id;
                                                return (
                                                    <button
                                                        key={item.id}
                                                        onClick={() => setActiveTab(item.id)}
                                                        className={`w-full flex items-center justify-between px-4 py-3 text-sm font-medium rounded-xl transition-all duration-200
                                                            ${isActive
                                                                ? "bg-sky-500 text-white shadow-md shadow-sky-200"
                                                                : "text-muted-foreground hover:bg-muted hover:text-foreground"
                                                            }`}
                                                    >
                                                        <span>{item.label}</span>
                                                        {isActive && <ChevronRight className="h-4 w-4 text-white/80" />}
                                                    </button>
                                                );
                                            })}
                                        </div>
                                    </div>
                                ))}

                                {/* Contact Box */}
                                <div className="mt-8 bg-gradient-to-br from-slate-100 to-white dark:from-slate-900 dark:to-background border rounded-2xl p-5 shadow-sm">
                                    <p className="text-xs font-bold text-muted-foreground mb-2">고객센터 운영안내</p>
                                    <div className="flex items-baseline gap-2 mb-2">
                                        <Phone className="h-4 w-4 text-sky-600" />
                                        <p className="text-lg font-bold text-sky-600">1588-0000</p>
                                    </div>
                                    <p className="text-xs text-muted-foreground leading-relaxed">
                                        평일 09:00 - 18:00<br />
                                        (주말/공휴일 휴무)
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Content Area - Right Column */}
                        <div className="lg:col-span-3">
                            {renderContent()}
                        </div>
                    </div>
                </main>

                <Footer />
            </div>
        </div>
    );
}
