import React, { useState, useEffect, useMemo } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { cn, safeImageUrl } from "@/lib/utils";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
    Calendar as CalendarIcon,
    Clock,
    MapPin,
    Building2,
    Video,
    Bell,
    CheckCircle2,
    XCircle,
    MessageSquare,
    Briefcase,
    ChevronRight,
    LogIn,
    Loader2,
    Send,
    Eye,
    AlarmClock,
    Trophy,
    CreditCard,
    Star,
    Coins,
    Megaphone,
    Mail,
    ChevronLeft,
} from "lucide-react";
import { format, isSameDay, parseISO } from "date-fns";
import { ko } from "date-fns/locale";
import { getMyInterviews, getInterviewsByApplication, InterviewDTO, respondToInterview } from "@/api/interviews";
import { getMyApplications } from "@/api/applications";
import { getReceivedProposals, respondToProposal, ProposalResponse } from "@/api/proposal";
import { useNotificationContext } from "@/contexts/NotificationContext";

const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

const InterviewPage = () => {
    const { user, loading: authLoading } = useAuth();
    const [searchParams, setSearchParams] = useSearchParams();
    const tabFromUrl = ["calendar", "offers", "notifications"].includes(searchParams.get("tab") || "") ? (searchParams.get("tab") as string) : "calendar";
    const [currentDate, setCurrentDate] = useState(new Date());
    const [selectedDate, setSelectedDate] = useState<Date | undefined>(new Date());
    const [interviews, setInterviews] = useState<InterviewDTO[]>([]);
    const [loading, setLoading] = useState(true);

    // 지원 제안 상태
    const [proposals, setProposals] = useState<ProposalResponse[]>([]);
    const [proposalsLoading, setProposalsLoading] = useState(true);
    const [respondingId, setRespondingId] = useState<number | null>(null);

    const notifCtx = useNotificationContext();
    const {
        notifications,
        unreadCount,
        loading: notifLoading,
        fetchRecentNotifications,
        markAsRead,
        markAllAsRead
    } = notifCtx ?? {
        notifications: [],
        unreadCount: 0,
        loading: false,
        fetchRecentNotifications: async () => {},
        markAsRead: async () => {},
        markAllAsRead: async () => {},
    };

    // 면접 일정 조회 (API 실패 시 마이페이지 지원 목록 중 면접예정 건으로 fallback)
    useEffect(() => {
        const fetchInterviews = async () => {
            if (!user) return;
            try {
                const data = await getMyInterviews();
                const list = Array.isArray(data) ? data : (data as any)?.data ?? (data as any)?.content ?? [];
                const arr = Array.isArray(list) ? list : [];
                if (arr.length > 0) {
                    setInterviews(arr);
                    setLoading(false);
                    return;
                }
                const apps = await getMyApplications();
                const appList = Array.isArray(apps) ? apps : (apps as any)?.content ?? [];
                const withId = appList.filter(
                    (a: { applicationId?: number; id?: number }) => (a.applicationId ?? a.id ?? 0) > 0
                );
                if (withId.length === 0) {
                    setInterviews([]);
                    setLoading(false);
                    return;
                }
                const nested = await Promise.all(
                    withId.map((a: { applicationId?: number; id?: number }) =>
                        getInterviewsByApplication(a.applicationId ?? a.id ?? 0)
                            .then((res: InterviewDTO[] | { data?: InterviewDTO[] }) =>
                                Array.isArray(res) ? res : (res as any)?.data ?? []
                            )
                            .catch(() => [] as InterviewDTO[])
                    )
                );
                const flat = (nested.flat() as InterviewDTO[]).filter(Boolean);
                const byId = new Map<number, InterviewDTO>();
                flat.forEach((i) => byId.set(i.interviewId, i));
                setInterviews(Array.from(byId.values()));
            } catch {
                setInterviews([]);
            } finally {
                setLoading(false);
            }
        };

        fetchInterviews();
    }, [user]);

    // 받은 제안 목록 조회
    useEffect(() => {
        const fetchProposals = async () => {
            if (!user) return;
            try {
                const data = await getReceivedProposals(0, 20);
                setProposals(data.content || []);
            } catch {
                setProposals([]);
            } finally {
                setProposalsLoading(false);
            }
        };

        fetchProposals();
    }, [user]);

    // 알림 조회
    useEffect(() => {
        if (user) {
            fetchRecentNotifications(20);
        }
    }, [user, fetchRecentNotifications]);

    // 제안 응답 처리
    const handleProposalRespond = async (proposalId: number, accept: boolean) => {
        setRespondingId(proposalId);
        try {
            await respondToProposal(proposalId, { accept });
            // 목록 갱신
            const data = await getReceivedProposals(0, 20);
            setProposals(data.content || []);
        } catch (error) {
            console.error("Failed to respond to proposal:", error);
        } finally {
            setRespondingId(null);
        }
    };

    // 캘린더 데이터 계산 (useMemo는 조건부 return 전에 호출되어야 함)
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1;

    const calendarDays = useMemo(() => {
        const firstDay = new Date(year, month - 1, 1);
        const lastDay = new Date(year, month, 0);
        const days: { date: Date; isCurrentMonth: boolean }[] = [];
        const startDayOfWeek = firstDay.getDay();
        for (let i = startDayOfWeek - 1; i >= 0; i--) {
            days.push({ date: new Date(year, month - 1, -i), isCurrentMonth: false });
        }
        for (let i = 1; i <= lastDay.getDate(); i++) {
            days.push({ date: new Date(year, month - 1, i), isCurrentMonth: true });
        }
        const remaining = 42 - days.length;
        for (let i = 1; i <= remaining; i++) {
            days.push({ date: new Date(year, month, i), isCurrentMonth: false });
        }
        return days;
    }, [year, month]);

    const getInterviewsForDate = (date: Date) =>
        interviews.filter((i) => {
            const d = new Date(i.startAt);
            return d.getFullYear() === date.getFullYear() && d.getMonth() === date.getMonth() && d.getDate() === date.getDate();
        });

    const isToday = (date: Date) => {
        const t = new Date();
        return date.getFullYear() === t.getFullYear() && date.getMonth() === t.getMonth() && date.getDate() === t.getDate();
    };

    // 선택된 날짜의 면접 일정
    const selectedDateInterviews = selectedDate ? getInterviewsForDate(selectedDate) : [];

    if (!user && !authLoading) {
        return (
            <div className="min-h-screen bg-[#F8F9FA]">
                {/* Guest View (Verified) */}
                <Sidebar />
                <div className="lg:pl-64 flex flex-col min-h-screen">
                    <Header />
                    <main className="flex-1 p-6 lg:p-10 flex items-center justify-center min-h-[calc(100vh-80px)]">
                        <div className="max-w-2xl w-full">
                            <Card className="overflow-hidden border-none shadow-lg">
                                <div className="bg-gradient-to-br from-sky-500/10 to-sky-500/5 p-10 text-center border-b border-sky-100/50">
                                    <div className="mx-auto h-20 w-20 rounded-2xl bg-white shadow-sm flex items-center justify-center mb-6">
                                        <CalendarIcon className="h-10 w-10 text-sky-500" />
                                    </div>
                                    <h1 className="text-2xl font-bold text-gray-900 mb-2">면접 관리</h1>
                                    <p className="text-gray-500">
                                        흩어져 있는 면접 일정과 다양한 채용 제안을 한곳에서 모아보세요
                                    </p>
                                </div>
                                <CardContent className="p-8">
                                    {/* 기능 안내 */}
                                    <div className="grid gap-4 mb-8">
                                        <div className="flex items-center gap-4 p-4 rounded-xl bg-gray-50/50 border border-gray-100/50">
                                            <div className="h-10 w-10 rounded-lg bg-white shadow-sm flex items-center justify-center">
                                                <CalendarIcon className="h-5 w-5 text-sky-500" />
                                            </div>
                                            <div>
                                                <p className="font-bold text-gray-900">면접 캘린더</p>
                                                <p className="text-sm text-gray-500">일정을 캘린더 형태의 깨끗한 화면으로 확인하세요</p>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-4 p-4 rounded-xl bg-gray-50/50 border border-gray-100/50">
                                            <div className="h-10 w-10 rounded-lg bg-white shadow-sm flex items-center justify-center">
                                                <Briefcase className="h-5 w-5 text-sky-500" />
                                            </div>
                                            <div>
                                                <p className="font-bold text-gray-900">맞춤 채용 제안</p>
                                                <p className="text-sm text-gray-500">기업이 내 이력을 검토하고 먼저 보내는 제안을 받아보세요</p>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-4 p-4 rounded-xl bg-gray-50/50 border border-gray-100/50">
                                            <div className="h-10 w-10 rounded-lg bg-white shadow-sm flex items-center justify-center">
                                                <Bell className="h-5 w-5 text-sky-500" />
                                            </div>
                                            <div>
                                                <p className="font-bold text-gray-900">실시간 진행 알림</p>
                                                <p className="text-sm text-gray-500">중요한 면접 일정과 결과 소식을 실시간으로 알려드립니다</p>
                                            </div>
                                        </div>
                                    </div>

                                    {/* 로그인 버튼 */}
                                    <div className="flex flex-col gap-4">
                                        <Button asChild className="w-full text-white h-12 shadow-md border-0 transition-all hover:scale-[1.02]" style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }} size="lg">
                                            <Link to="/auth">
                                                <LogIn className="h-5 w-5 mr-2" />
                                                로그인하고 시작하기
                                            </Link>
                                        </Button>

                                        <p className="text-center text-sm text-gray-400">
                                            아직 회원이 아니신가요?{" "}
                                            <Link to="/auth?mode=signup" className="text-sky-500 font-bold hover:underline">
                                                간편 회원가입
                                            </Link>
                                        </p>
                                    </div>
                                </CardContent>
                            </Card>
                        </div>
                    </main>
                    <Footer />
                </div>
            </div>
        );
    }

    const getStatusBadge = (status: string) => {
        switch (status) {
            case "PROPOSED":
                return <Badge className="bg-sky-500 hover:bg-sky-600 text-white border-none">제안됨</Badge>;
            case "CONFIRMED":
                return <Badge className="bg-green-500 hover:bg-green-600 text-white border-none">확정됨</Badge>; // Changed color for confirmed
            case "SCHEDULED": // Keeping purely for fallback if legacy data exists
                return <Badge className="bg-sky-500 hover:bg-sky-600 text-white border-none">예정</Badge>;
            case "COMPLETED": // Legacy fallback
            case "DONE":
                return <Badge variant="secondary">완료</Badge>;
            case "CANCELED":
                return <Badge variant="destructive">취소</Badge>;
            default:
                return <Badge variant="outline">{status}</Badge>;
        }
    };

    const getOfferStatusBadge = (status: string) => {
        switch (status) {
            case "PENDING":
                return <Badge className="bg-yellow-500 hover:bg-yellow-600 text-white border-none">대기중</Badge>;
            case "VIEWED":
                return <Badge className="bg-sky-500 hover:bg-sky-600 text-white border-none">검토중</Badge>;
            case "ACCEPTED":
                return <Badge className="bg-green-500 hover:bg-green-600 text-white border-none">수락</Badge>;
            case "REJECTED":
                return <Badge variant="destructive">거절</Badge>;
            case "EXPIRED":
                return <Badge variant="secondary">만료</Badge>;
            case "CANCELED":
                return <Badge variant="secondary">취소됨</Badge>;
            default:
                return <Badge variant="outline">{status}</Badge>;
        }
    };

    // 알림 타입별 아이콘 반환
    const getNotificationIcon = (type: string) => {
        switch (type) {
            // 면접 관련
            case "INTERVIEW_SCHEDULED":
                return <CalendarIcon className="h-6 w-6" />;
            case "INTERVIEW_REMINDER":
                return <AlarmClock className="h-6 w-6" />;
            case "INTERVIEW_ACCEPTED":
                return <CheckCircle2 className="h-6 w-6" />;
            case "INTERVIEW_DECLINED":
                return <XCircle className="h-6 w-6" />;
            case "INTERVIEW_RESCHEDULE_REQUEST":
                return <Clock className="h-6 w-6" />;
            case "INTERVIEW_CANCELLED":
            case "INTERVIEW_CANCELED":
                return <XCircle className="h-6 w-6" />;
            case "INTERVIEW_RESULT":
                return <CheckCircle2 className="h-6 w-6" />;
            // 지원 관련
            case "APPLICATION_SUBMITTED":
                return <Send className="h-6 w-6" />;
            case "APPLICATION_VIEWED":
                return <Eye className="h-6 w-6" />;
            case "APPLICATION_STATUS_CHANGED":
                return <Briefcase className="h-6 w-6" />;
            case "NEW_APPLICATION_RECEIVED":
                return <Mail className="h-6 w-6" />;
            // 제안 관련
            case "PROPOSAL_RECEIVED":
                return <Briefcase className="h-6 w-6" />;
            case "PROPOSAL_ACCEPTED":
                return <CheckCircle2 className="h-6 w-6" />;
            case "PROPOSAL_REJECTED":
                return <XCircle className="h-6 w-6" />;
            // 채용 결과
            case "HIRED":
                return <Trophy className="h-6 w-6" />;
            case "REJECTED":
                return <XCircle className="h-6 w-6" />;
            // 결제/크레딧 관련
            case "PAYMENT_COMPLETED":
            case "PAYMENT_FAILED":
            case "PAYMENT_REFUNDED":
                return <CreditCard className="h-6 w-6" />;
            case "CREDIT_CHARGED":
            case "CREDIT_USED":
            case "CREDIT_LOW":
                return <Coins className="h-6 w-6" />;
            // 구독 관련
            case "SUBSCRIPTION_STARTED":
            case "SUBSCRIPTION_RENEWED":
            case "SUBSCRIPTION_EXPIRING":
            case "SUBSCRIPTION_CANCELED":
                return <Star className="h-6 w-6" />;
            // 시스템
            case "SYSTEM_NOTICE":
                return <Megaphone className="h-6 w-6" />;
            default:
                return <Bell className="h-6 w-6" />;
        }
    };

    return (
        <div className="min-h-screen bg-[#F8F9FA]">
            <Sidebar />

            <div className="lg:pl-64 flex flex-col min-h-screen">
                <Header />

                <main className="flex-1 p-6 lg:p-10">
                    <div className="max-w-7xl mx-auto space-y-8">
                        {/* 페이지 헤더 */}
                        <div className="flex items-center justify-between">
                            <div>
                                <h1 className="text-2xl font-bold text-gray-900">면접 관리</h1>
                                <p className="text-gray-500 mt-1">면접 일정과 지원 제안을 한눈에 관리하세요</p>
                            </div>
                            <div className="flex items-center gap-2">
                                <Badge variant="outline" className="border-gray-200 text-gray-600 bg-white gap-2 px-3 py-1">
                                    <Bell className="h-3.5 w-3.5 text-sky-500" />
                                    {unreadCount > 0 ? `${unreadCount} 새 알림` : "알림"}
                                </Badge>
                            </div>
                        </div>

                        <Tabs value={tabFromUrl} onValueChange={(v) => setSearchParams(prev => { prev.set("tab", v); return prev; })} className="w-full">
                            <TabsList className="bg-white p-1 rounded-xl shadow-sm border-none w-full max-w-lg grid grid-cols-3 mb-8">
                                <TabsTrigger value="calendar" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                    <CalendarIcon className="h-4 w-4" />
                                    면접 캘린더
                                </TabsTrigger>
                                <TabsTrigger value="offers" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                    <Briefcase className="h-4 w-4" />
                                    지원 제안
                                    {proposals.filter(p => p.status === "PENDING" || p.status === "VIEWED").length > 0 && (
                                        <Badge variant="secondary" className="ml-1 h-5 px-1.5 bg-gray-100 text-gray-600 border-none">
                                            {proposals.filter(p => p.status === "PENDING" || p.status === "VIEWED").length}
                                        </Badge>
                                    )}
                                </TabsTrigger>
                                <TabsTrigger value="notifications" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                    <Bell className="h-4 w-4" />
                                    알림
                                    {unreadCount > 0 && (
                                        <Badge className="ml-1 h-5 px-1.5 bg-red-500 text-white border-none">
                                            {unreadCount}
                                        </Badge>
                                    )}
                                </TabsTrigger>
                            </TabsList>

                            {/* 면접 캘린더 탭 - 기업 면접 일정과 동일한 그리드 캘린더 */}
                            <TabsContent value="calendar" className="mt-0">
                                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                                    {/* 캘린더 (기업 CompanyInterviews와 동일한 그리드) */}
                                    <Card className="lg:col-span-4 border-none shadow-sm bg-white overflow-hidden h-fit sticky top-24">
                                        <CardHeader className="flex flex-row items-center justify-between pb-2">
                                            <CardTitle className="text-lg font-bold text-sky-600 flex items-center gap-2">
                                                <CalendarIcon className="h-5 w-5" />
                                                면접 캘린더
                                            </CardTitle>
                                            <div className="flex items-center gap-2">
                                                <Button
                                                    variant="outline"
                                                    size="icon"
                                                    onClick={() => setCurrentDate(new Date(year, month - 2, 1))}
                                                >
                                                    <ChevronLeft className="h-4 w-4" />
                                                </Button>
                                                <span className="font-semibold min-w-[100px] text-center text-sm">
                                                    {year}년 {month}월
                                                </span>
                                                <Button
                                                    variant="outline"
                                                    size="icon"
                                                    onClick={() => setCurrentDate(new Date(year, month, 1))}
                                                >
                                                    <ChevronRight className="h-4 w-4" />
                                                </Button>
                                                <Button
                                                    variant="outline"
                                                    size="sm"
                                                    onClick={() => {
                                                        setCurrentDate(new Date());
                                                        setSelectedDate(new Date());
                                                    }}
                                                >
                                                    오늘
                                                </Button>
                                            </div>
                                        </CardHeader>
                                        <CardContent>
                                            {loading ? (
                                                <div className="flex justify-center py-12">
                                                    <Loader2 className="h-8 w-8 animate-spin text-sky-500" />
                                                </div>
                                            ) : (
                                                <div className="border rounded-lg overflow-hidden">
                                                    <div className="grid grid-cols-7 bg-muted">
                                                        {weekDays.map((day, idx) => (
                                                            <div
                                                                key={day}
                                                                className={`text-center py-2 text-sm font-medium ${
                                                                    idx === 0 ? "text-red-500" : idx === 6 ? "text-blue-500" : ""
                                                                }`}
                                                            >
                                                                {day}
                                                            </div>
                                                        ))}
                                                    </div>
                                                    <div className="grid grid-cols-7">
                                                        {calendarDays.map(({ date, isCurrentMonth }, idx) => {
                                                            const dayInterviews = getInterviewsForDate(date);
                                                            const isSelected =
                                                                selectedDate && date.toDateString() === selectedDate.toDateString();
                                                            return (
                                                                <div
                                                                    key={idx}
                                                                    onClick={() => setSelectedDate(date)}
                                                                    className={cn(
                                                                        "min-h-[72px] p-1 border-t border-l cursor-pointer transition-colors",
                                                                        !isCurrentMonth && "bg-muted/30 text-muted-foreground",
                                                                        isToday(date) && "bg-sky-50",
                                                                        isSelected && "bg-sky-100 ring-1 ring-sky-500",
                                                                        "hover:bg-muted/50"
                                                                    )}
                                                                >
                                                                    <div
                                                                        className={cn(
                                                                            "text-sm font-medium mb-0.5",
                                                                            idx % 7 === 0 ? "text-red-500" : idx % 7 === 6 ? "text-blue-500" : ""
                                                                        )}
                                                                    >
                                                                        {date.getDate()}
                                                                    </div>
                                                                    {dayInterviews.slice(0, 2).map((interview) => (
                                                                        <div
                                                                            key={interview.interviewId}
                                                                            className="text-xs p-1 rounded mb-0.5 truncate bg-sky-100 text-sky-700"
                                                                        >
                                                                            {interview.companyName}
                                                                        </div>
                                                                    ))}
                                                                    {dayInterviews.length > 2 && (
                                                                        <div className="text-xs text-muted-foreground">+{dayInterviews.length - 2}</div>
                                                                    )}
                                                                </div>
                                                            );
                                                        })}
                                                    </div>
                                                </div>
                                            )}
                                            <div className="mt-4 flex items-center gap-2 text-xs text-gray-500">
                                                <div className="h-2 w-2 rounded-full bg-sky-500" />
                                                <span>면접 예정일</span>
                                            </div>
                                        </CardContent>
                                    </Card>

                                    {/* 선택된 날짜의 일정 및 다가오는 면접 */}
                                    <div className="lg:col-span-8 space-y-8">
                                        <Card className="border-none shadow-sm bg-white">
                                            <CardHeader className="border-b border-gray-50">
                                                <CardTitle className="text-lg font-bold">
                                                    {selectedDate
                                                        ? format(selectedDate, "M월 d일 (EEEE)", { locale: ko })
                                                        : "날짜를 선택하세요"}
                                                </CardTitle>
                                            </CardHeader>
                                            <CardContent className="p-6">
                                                {loading ? (
                                                    <div className="text-center py-20 text-gray-500">일정을 불러오는 중...</div>
                                                ) : selectedDateInterviews.length > 0 ? (
                                                    <div className="space-y-4">
                                                        {selectedDateInterviews.map((interview) => (
                                                            <div
                                                                key={interview.interviewId}
                                                                className="p-5 rounded-xl border border-gray-50 bg-[#FBFBFC] hover:shadow-md transition-all group"
                                                            >
                                                                <div className="flex items-start justify-between">
                                                                    <div className="space-y-3">
                                                                        <div className="flex items-center gap-3">
                                                                            <div className="p-2 bg-white rounded-lg shadow-sm">
                                                                                <Building2 className="h-5 w-5 text-gray-400" />
                                                                            </div>
                                                                            <span className="font-bold text-lg text-gray-900">{interview.companyName}</span>
                                                                            {getStatusBadge(interview.status)}
                                                                        </div>
                                                                        <p className="text-sm font-medium text-gray-600">{interview.jobTitle}</p>
                                                                        <div className="flex flex-wrap gap-5 text-sm text-gray-500">
                                                                            <span className="flex items-center gap-2">
                                                                                <Clock className="h-4 w-4 text-gray-400" />
                                                                                {format(parseISO(interview.startAt), "a h:mm", { locale: ko })}
                                                                            </span>
                                                                            <span className="flex items-center gap-2">
                                                                                {interview.method === "VIDEO" || interview.method === "PHONE" ? (
                                                                                    <Video className="h-4 w-4 text-gray-400" />
                                                                                ) : (
                                                                                    <MapPin className="h-4 w-4 text-gray-400" />
                                                                                )}
                                                                                {interview.location || interview.meetingUrl || "장소/링크 미정"}
                                                                            </span>
                                                                        </div>
                                                                    </div>
                                                                    <Button variant="outline" size="sm" className="border-gray-200 hover:bg-white">
                                                                        상세보기
                                                                    </Button>
                                                                </div>
                                                            </div>
                                                        ))}
                                                    </div>
                                                ) : (
                                                    <div className="flex flex-col items-center justify-center py-20 text-center">
                                                        <div className="h-16 w-16 bg-gray-50 rounded-full flex items-center justify-center mb-6">
                                                            <CalendarIcon className="h-8 w-8 text-gray-200" />
                                                        </div>
                                                        <p className="text-gray-400 font-medium">
                                                            {selectedDate
                                                                ? "이 날짜에 예정된 면접이 없습니다"
                                                                : "날짜를 선택하면 면접 일정을 확인할 수 있습니다"}
                                                        </p>
                                                    </div>
                                                )}
                                            </CardContent>
                                        </Card>

                                        {/* 전체 면접 일정 */}
                                        <div className="space-y-4">
                                            <div className="flex items-center justify-between px-1">
                                                <h2 className="text-lg font-bold text-gray-900">전체 면접 일정</h2>
                                                <div className="flex items-center gap-2 text-sm text-gray-500">
                                                    <span className="flex items-center gap-1">
                                                        <div className="h-2 w-2 rounded-full bg-sky-500" />
                                                        예정
                                                    </span>
                                                    <span className="flex items-center gap-1">
                                                        <div className="h-2 w-2 rounded-full bg-gray-400" />
                                                        완료/취소
                                                    </span>
                                                </div>
                                            </div>
                                            <div className="space-y-3">
                                                {interviews.length > 0 ? (
                                                    [...interviews]
                                                        .sort((a, b) => new Date(b.startAt).getTime() - new Date(a.startAt).getTime())
                                                        .map((interview) => {
                                                            const isPast = new Date(interview.startAt) < new Date();
                                                            const isProposed = interview.status === "PROPOSED";
                                                            
                                                            return (
                                                                <div
                                                                    key={interview.interviewId}
                                                                    className={`p-5 rounded-xl bg-white shadow-sm hover:shadow-md transition-all group ${
                                                                        isPast || interview.status === "CANCELED" || interview.status === "DONE" 
                                                                            ? "opacity-75" 
                                                                            : ""
                                                                    }`}
                                                                >
                                                                    <div className="flex items-center justify-between">
                                                                        <div className="flex items-center gap-5">
                                                                            <div className={`h-14 w-14 rounded-xl flex items-center justify-center transition-colors ${
                                                                                isPast || interview.status === "CANCELED" || interview.status === "DONE"
                                                                                    ? "bg-gray-100"
                                                                                    : "bg-sky-50 group-hover:bg-sky-100"
                                                                            }`}>
                                                                                {interview.method === "VIDEO" ? (
                                                                                    <Video className={`h-7 w-7 ${isPast ? "text-gray-400" : "text-sky-500"}`} />
                                                                                ) : interview.method === "PHONE" ? (
                                                                                    <Video className={`h-7 w-7 ${isPast ? "text-gray-400" : "text-sky-500"}`} />
                                                                                ) : (
                                                                                    <Building2 className={`h-7 w-7 ${isPast ? "text-gray-400" : "text-sky-500"}`} />
                                                                                )}
                                                                            </div>
                                                                            <div>
                                                                                <p className={`font-bold transition-colors ${
                                                                                    isPast ? "text-gray-500" : "text-gray-900 group-hover:text-sky-600"
                                                                                }`}>
                                                                                    {interview.companyName} - {interview.jobTitle}
                                                                                </p>
                                                                                <div className="flex items-center gap-3 text-sm text-gray-400 mt-1">
                                                                                    <span className="flex items-center gap-1.5">
                                                                                        <CalendarIcon className="h-3.5 w-3.5" />
                                                                                        {format(parseISO(interview.startAt), "M월 d일 (EEE) a h:mm", { locale: ko })}
                                                                                    </span>
                                                                                    <span className="text-gray-200">|</span>
                                                                                    <span>{interview.method === 'ONSITE' ? '대면' : interview.method === 'VIDEO' ? '화상' : '전화'} 면접</span>
                                                                                    {interview.location && (
                                                                                        <>
                                                                                            <span className="text-gray-200">|</span>
                                                                                            <span className="flex items-center gap-1">
                                                                                                <MapPin className="h-3 w-3" />
                                                                                                {interview.location}
                                                                                            </span>
                                                                                        </>
                                                                                    )}
                                                                                </div>
                                                                            </div>
                                                                        </div>
                                                                        <div className="flex items-center gap-3">
                                                                            {getStatusBadge(interview.status)}
                                                                            {isProposed && !isPast && (
                                                                                <div className="flex gap-2">
                                                                                    <Button
                                                                                        size="sm"
                                                                                        className="bg-green-600 hover:bg-green-700 text-white"
                                                                                        onClick={async () => {
                                                                                            try {
                                                                                                await respondToInterview(interview.interviewId, "ACCEPT");
                                                                                                const data = await getMyInterviews();
                                                                                                const list = Array.isArray(data) ? data : (data as any)?.data ?? (data as any)?.content ?? [];
                                                                                                setInterviews(Array.isArray(list) ? list : []);
                                                                                            } catch {}
                                                                                        }}
                                                                                    >
                                                                                        <CheckCircle2 className="h-4 w-4 mr-1" />
                                                                                        수락
                                                                                    </Button>
                                                                                    <Button
                                                                                        size="sm"
                                                                                        variant="outline"
                                                                                        className="text-red-600 border-red-200 hover:bg-red-50"
                                                                                        onClick={async () => {
                                                                                            try {
                                                                                                await respondToInterview(interview.interviewId, "DECLINE");
                                                                                                const data = await getMyInterviews();
                                                                                                const list = Array.isArray(data) ? data : (data as any)?.data ?? (data as any)?.content ?? [];
                                                                                                setInterviews(Array.isArray(list) ? list : []);
                                                                                            } catch {}
                                                                                        }}
                                                                                    >
                                                                                        <XCircle className="h-4 w-4 mr-1" />
                                                                                        거절
                                                                                    </Button>
                                                                                </div>
                                                                            )}
                                                                            {!isProposed && (
                                                                                <ChevronRight className="h-5 w-5 text-gray-300 group-hover:text-sky-500 transition-colors" />
                                                                            )}
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            );
                                                        })
                                                ) : (
                                                    <div className="flex flex-col items-center justify-center py-12 bg-white rounded-xl shadow-sm">
                                                        <div className="h-12 w-12 bg-gray-50 rounded-full flex items-center justify-center mb-4">
                                                            <Briefcase className="h-6 w-6 text-gray-300" />
                                                        </div>
                                                        <p className="text-gray-500 font-medium">면접 일정이 없습니다.</p>
                                                        <p className="text-gray-400 text-sm mt-1">지원한 공고에서 면접 요청이 들어오면 여기에 표시됩니다.</p>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </TabsContent>

                            {/* 지원 제안 탭 */}
                            <TabsContent value="offers" className="mt-0">
                                {proposalsLoading ? (
                                    <div className="flex flex-col items-center justify-center py-20">
                                        <Loader2 className="h-8 w-8 animate-spin text-sky-500 mb-4" />
                                        <p className="text-gray-500">제안 목록을 불러오는 중...</p>
                                    </div>
                                ) : proposals.length > 0 ? (
                                    <div className="grid gap-6">
                                        {proposals.map((proposal) => (
                                            <Card key={proposal.proposalId} className="border-none shadow-sm bg-white overflow-hidden hover:shadow-md transition-all">
                                                <CardContent className="p-8">
                                                    <div className="flex flex-col lg:flex-row justify-between gap-8">
                                                        <div className="space-y-5 flex-1">
                                                            <div className="flex items-center flex-wrap gap-3">
                                                                {safeImageUrl(proposal.employerLogo) ? (
                                                                    <img src={safeImageUrl(proposal.employerLogo)} alt={proposal.employerName} className="h-12 w-12 rounded-xl object-cover" />
                                                                ) : (
                                                                    <div className="p-2.5 bg-sky-50 rounded-xl">
                                                                        <Building2 className="h-6 w-6 text-sky-500" />
                                                                    </div>
                                                                )}
                                                                <h3 className="font-bold text-xl text-gray-900">{proposal.employerName}</h3>
                                                                {getOfferStatusBadge(proposal.status)}
                                                            </div>
                                                            <div>
                                                                <p className="text-lg font-bold text-gray-700">{proposal.title}</p>
                                                                {proposal.offeredPosition && (
                                                                    <p className="text-sm text-gray-500 mt-1">포지션: {proposal.offeredPosition}</p>
                                                                )}
                                                                {proposal.offeredSalary && (
                                                                    <p className="text-sm text-sky-600 font-bold mt-1.5 flex items-center gap-1.5">
                                                                        <span className="text-xl">💰</span> 예상 연봉: {proposal.offeredSalary}
                                                                    </p>
                                                                )}
                                                            </div>
                                                            {proposal.message && (
                                                                <div className="p-5 bg-[#FBFBFC] rounded-2xl border border-gray-50 flex items-start gap-4">
                                                                    <MessageSquare className="h-5 w-5 text-gray-300 mt-1 flex-shrink-0" />
                                                                    <p className="text-sm text-gray-600 leading-relaxed font-medium">
                                                                        {proposal.message}
                                                                    </p>
                                                                </div>
                                                            )}
                                                            <div className="flex items-center gap-5 text-xs font-bold uppercase tracking-wider text-gray-400">
                                                                <span className="flex items-center gap-2">
                                                                    <Clock className="h-4 w-4" />
                                                                    수신: {new Date(proposal.createdAt).toLocaleDateString()}
                                                                </span>
                                                                {proposal.expiresAt && (
                                                                    <>
                                                                        <span className="w-1 h-1 bg-gray-200 rounded-full" />
                                                                        <span className="flex items-center gap-2 text-rose-400">
                                                                            <XCircle className="h-4 w-4" />
                                                                            제안 마감: {new Date(proposal.expiresAt).toLocaleDateString()}
                                                                        </span>
                                                                    </>
                                                                )}
                                                            </div>
                                                        </div>

                                                        {(proposal.status === "PENDING" || proposal.status === "VIEWED") && (
                                                            <div className="flex flex-row lg:flex-col gap-3 lg:w-48">
                                                                <Button
                                                                    className="flex-1 btn-gradient-primary border-0"
                                                                    onClick={() => handleProposalRespond(proposal.proposalId, true)}
                                                                    disabled={respondingId === proposal.proposalId}
                                                                >
                                                                    {respondingId === proposal.proposalId ? (
                                                                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                                                    ) : (
                                                                        <CheckCircle2 className="h-4 w-4 mr-2" />
                                                                    )}
                                                                    수락하기
                                                                </Button>
                                                                <Button
                                                                    variant="outline"
                                                                    className="flex-1 border-gray-200 text-gray-600 hover:bg-gray-50"
                                                                    onClick={() => handleProposalRespond(proposal.proposalId, false)}
                                                                    disabled={respondingId === proposal.proposalId}
                                                                >
                                                                    <XCircle className="h-4 w-4 mr-2" />
                                                                    거절하기
                                                                </Button>
                                                            </div>
                                                        )}
                                                    </div>
                                                </CardContent>
                                            </Card>
                                        ))}
                                    </div>
                                ) : (
                                    <Card className="border-none shadow-sm bg-white">
                                        <CardContent className="p-8">
                                            <div className="flex flex-col items-center justify-center py-12">
                                                <div className="h-16 w-16 bg-gray-50 rounded-full flex items-center justify-center mb-6">
                                                    <Briefcase className="h-8 w-8 text-gray-300" />
                                                </div>
                                                <p className="text-gray-500 font-medium">받은 제안이 없습니다</p>
                                                <p className="text-gray-400 text-sm mt-2">기업에서 보낸 제안이 여기에 표시됩니다</p>
                                            </div>
                                        </CardContent>
                                    </Card>
                                )}
                            </TabsContent>

                            {/* 알림 탭 */}
                            <TabsContent value="notifications" className="mt-0">
                                <Card className="border-none shadow-sm bg-white overflow-hidden">
                                    <CardHeader className="border-b border-gray-50 flex flex-row items-center justify-between py-6">
                                        <CardTitle className="text-xl font-bold">면접 알림 센터</CardTitle>
                                        {unreadCount > 0 && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="text-sky-500 hover:bg-sky-50 hover:text-sky-600 font-bold"
                                                onClick={markAllAsRead}
                                            >
                                                모두 읽음 처리
                                            </Button>
                                        )}
                                    </CardHeader>
                                    <CardContent className="p-0">
                                        {notifLoading ? (
                                            <div className="flex flex-col items-center justify-center py-20">
                                                <Loader2 className="h-8 w-8 animate-spin text-sky-500 mb-4" />
                                                <p className="text-gray-500">알림을 불러오는 중...</p>
                                            </div>
                                        ) : notifications.length > 0 ? (
                                            <div className="divide-y divide-gray-50">
                                                {notifications.map((notification: any) => (
                                                    <div
                                                        key={notification.id}
                                                        className={cn(
                                                            "flex items-start gap-5 p-6 transition-all cursor-pointer group hover:bg-gray-50",
                                                            !notification.isRead && "bg-sky-50/30 ring-1 ring-inset ring-sky-50"
                                                        )}
                                                        onClick={() => markAsRead(notification.id)}
                                                    >
                                                        <div className={cn(
                                                            "h-12 w-12 rounded-2xl flex items-center justify-center flex-shrink-0 transition-transform group-hover:scale-110",
                                                            !notification.isRead ? "bg-sky-500 text-white shadow-lg shadow-sky-100" : "bg-gray-100 text-gray-400"
                                                        )}>
                                                            {getNotificationIcon(notification.type)}
                                                        </div>
                                                        <div className="flex-1 space-y-1">
                                                            <div className="flex items-center justify-between">
                                                                <p className={cn("font-bold text-lg", !notification.isRead ? "text-gray-900" : "text-gray-500")}>
                                                                    {notification.title}
                                                                </p>
                                                                <span className="text-xs font-bold text-gray-400">
                                                                    <Clock className="h-3 w-3 inline mr-1" />
                                                                    {notification.createdAt ? new Date(notification.createdAt).toLocaleDateString() : ""}
                                                                </span>
                                                            </div>
                                                            <p className="text-sm text-gray-500 leading-relaxed">{notification.message}</p>
                                                        </div>
                                                        {!notification.isRead && (
                                                            <div className="h-2.5 w-2.5 rounded-full bg-sky-500 mt-2 flex-shrink-0 shadow-sm" />
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <div className="flex flex-col items-center justify-center py-20">
                                                <div className="h-16 w-16 bg-gray-50 rounded-full flex items-center justify-center mb-6">
                                                    <Bell className="h-8 w-8 text-gray-300" />
                                                </div>
                                                <p className="text-gray-500 font-medium">알림이 없습니다</p>
                                                <p className="text-gray-400 text-sm mt-2">면접 일정 및 지원 관련 알림이 여기에 표시됩니다</p>
                                            </div>
                                        )}
                                    </CardContent>
                                </Card>
                            </TabsContent>
                        </Tabs>
                    </div>
                </main>
                <Footer />
            </div>
        </div>
    );
};

export default InterviewPage;
