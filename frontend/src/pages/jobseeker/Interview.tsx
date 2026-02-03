import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { cn } from "@/lib/utils";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Calendar } from "@/components/ui/calendar";
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
} from "lucide-react";
import { format, isSameDay, parseISO } from "date-fns";
import { ko } from "date-fns/locale";
import { getMyInterviews, InterviewDTO } from "@/api/interviews";

// 지원 제안 목데이터 (API 연동 아직 안됨)
const offers = [
    {
        id: 1,
        company: "토스",
        position: "시니어 프론트엔드 개발자",
        receivedAt: "2026-01-28",
        salary: "7,000만원 ~ 9,000만원",
        message: "안녕하세요, 토스 개발팀입니다. 귀하의 이력서를 검토한 결과, 저희 팀에 적합하다고 판단되어 면접 제안드립니다.",
        status: "pending",
        deadline: "2026-02-05"
    },
    {
        id: 2,
        company: "당근마켓",
        position: "React Native 개발자",
        receivedAt: "2026-01-25",
        salary: "6,000만원 ~ 8,000만원",
        message: "당근마켓에서 귀하의 프로필을 보고 연락드립니다. 모바일 앱 개발 경험이 인상적입니다.",
        status: "pending",
        deadline: "2026-02-03"
    },
    {
        id: 3,
        company: "쿠팡",
        position: "풀스택 개발자",
        receivedAt: "2026-01-20",
        salary: "5,500만원 ~ 7,500만원",
        message: "쿠팡 테크팀에서 함께할 개발자를 찾고 있습니다.",
        status: "accepted",
        deadline: "2026-01-28"
    },
];

// 알림 목데이터 (API 연동 필요)
const notifications = [
    {
        id: 1,
        type: "interview_reminder",
        title: "면접 알림",
        message: "네이버 면접이 내일입니다. 준비 사항을 확인하세요.",
        date: "2026-01-30",
        read: false
    },
    {
        id: 2,
        type: "offer_received",
        title: "새로운 제안",
        message: "토스에서 면접 제안이 도착했습니다.",
        date: "2026-01-28",
        read: false
    },
];

const InterviewPage = () => {
    const { user, loading: authLoading } = useAuth();
    const [selectedDate, setSelectedDate] = useState<Date | undefined>(new Date());
    const [interviews, setInterviews] = useState<InterviewDTO[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchInterviews = async () => {
            if (!user) return;
            try {
                const data = await getMyInterviews();
                setInterviews(data);
            } catch (error) {
                console.error("Failed to fetch interviews:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchInterviews();
    }, [user]);

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

    // 선택된 날짜의 면접 일정
    const selectedDateInterviews = interviews.filter(
        (interview) => selectedDate && isSameDay(parseISO(interview.startAt), selectedDate)
    );

    // 면접이 있는 날짜들
    const interviewDates = interviews.map((interview) => parseISO(interview.startAt));

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
            case "pending":
                return <Badge className="bg-yellow-500 hover:bg-yellow-600 text-white border-none">검토중</Badge>;
            case "accepted":
                return <Badge className="bg-green-500 hover:bg-green-600 text-white border-none">수락</Badge>;
            case "rejected":
                return <Badge variant="destructive">거절</Badge>;
            default:
                return <Badge variant="outline">{status}</Badge>;
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
                                    {notifications.filter(n => !n.read).length} 새 알림
                                </Badge>
                            </div>
                        </div>

                        <Tabs defaultValue="calendar" className="w-full">
                            <TabsList className="bg-white p-1 rounded-xl shadow-sm border-none w-full max-w-lg grid grid-cols-3 mb-8">
                                <TabsTrigger value="calendar" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                    <CalendarIcon className="h-4 w-4" />
                                    면접 캘린더
                                </TabsTrigger>
                                <TabsTrigger value="offers" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                    <Briefcase className="h-4 w-4" />
                                    지원 제안
                                    <Badge variant="secondary" className="ml-1 h-5 px-1.5 bg-gray-100 text-gray-600 border-none">
                                        {offers.filter(o => o.status === "pending").length}
                                    </Badge>
                                </TabsTrigger>
                                <TabsTrigger value="notifications" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                    <Bell className="h-4 w-4" />
                                    알림
                                    {notifications.filter(n => !n.read).length > 0 && (
                                        <Badge className="ml-1 h-5 px-1.5 bg-red-500 text-white border-none">
                                            {notifications.filter(n => !n.read).length}
                                        </Badge>
                                    )}
                                </TabsTrigger>
                            </TabsList>

                            {/* 면접 캘린더 탭 */}
                            <TabsContent value="calendar" className="mt-0">
                                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                                    {/* 캘린더 */}
                                    <Card className="lg:col-span-4 border-none shadow-sm bg-white overflow-hidden h-fit sticky top-24">
                                        <CardHeader className="pb-2">
                                            <CardTitle className="text-lg font-bold text-sky-600">캘린더</CardTitle>
                                        </CardHeader>
                                        <CardContent>
                                            <Calendar
                                                mode="single"
                                                selected={selectedDate}
                                                onSelect={setSelectedDate}
                                                locale={ko}
                                                className="rounded-md border-none p-0 w-full flex justify-center"
                                                classNames={{
                                                    month: "w-full max-w-sm space-y-4",
                                                    table: "w-full border-collapse space-y-1",
                                                    head_row: "flex w-full justify-between",
                                                    row: "flex w-full mt-2 justify-between",
                                                    cell: "h-10 w-10 text-center text-sm p-0 relative [&:has([aria-selected].day-range-end)]:rounded-r-md [&:has([aria-selected].day-outside)]:bg-accent/50 [&:has([aria-selected])]:bg-accent first:[&:has([aria-selected])]:rounded-l-md last:[&:has([aria-selected])]:rounded-r-md focus-within:relative focus-within:z-20",
                                                    day: "h-10 w-10 p-0 font-normal aria-selected:opacity-100 hover:bg-sky-100 rounded-full transition-colors",
                                                    head_cell: "text-muted-foreground rounded-md w-10 font-normal text-[0.8rem]",
                                                    caption: "flex justify-center pt-1 relative items-center mb-4",
                                                    caption_label: "text-base font-bold text-gray-900"
                                                }}
                                                modifiers={{
                                                    interview: interviewDates,
                                                }}
                                                modifiersStyles={{
                                                    interview: {
                                                        backgroundColor: "#0EA5E9",
                                                        color: "white",
                                                        borderRadius: "50%",
                                                    },
                                                }}
                                            />
                                            <div className="mt-6 flex items-center gap-2 text-xs text-gray-500">
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

                                        {/* 전체 면접 일정 요약 */}
                                        <div className="space-y-4">
                                            <h2 className="text-lg font-bold text-gray-900 px-1">다가오는 면접 일정</h2>
                                            <div className="space-y-3">
                                                {interviews.length > 0 ? (
                                                    interviews.map((interview) => (
                                                        <div
                                                            key={interview.interviewId}
                                                            className="flex items-center justify-between p-5 rounded-xl bg-white shadow-sm hover:shadow-md transition-all cursor-pointer group"
                                                        >
                                                            <div className="flex items-center gap-5">
                                                                <div className="h-14 w-14 rounded-xl bg-sky-50 flex items-center justify-center group-hover:bg-sky-100 transition-colors">
                                                                    {interview.method === "VIDEO" ? (
                                                                        <Video className="h-7 w-7 text-sky-500" />
                                                                    ) : interview.method === "PHONE" ? ( // PHONE case added if needed, or map appropriately
                                                                        <Video className="h-7 w-7 text-sky-500" />
                                                                    ) : (
                                                                        <Building2 className="h-7 w-7 text-sky-500" />
                                                                    )}
                                                                </div>
                                                                <div>
                                                                    <p className="font-bold text-gray-900 group-hover:text-sky-600 transition-colors">
                                                                        {interview.companyName} - {interview.jobTitle}
                                                                    </p>
                                                                    <div className="flex items-center gap-3 text-sm text-gray-400 mt-1">
                                                                        <span className="flex items-center gap-1.5">
                                                                            <CalendarIcon className="h-3.5 w-3.5" />
                                                                            {format(parseISO(interview.startAt), "M월 d일 (EEE) a h:mm", { locale: ko })}
                                                                        </span>
                                                                        <span className="text-gray-200">|</span>
                                                                        <span>{interview.method === 'ONSITE' ? '대면' : interview.method === 'VIDEO' ? '화상' : '전화'} 면접</span>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                            <div className="flex items-center gap-4">
                                                                {getStatusBadge(interview.status)}
                                                                <ChevronRight className="h-5 w-5 text-gray-300 group-hover:text-sky-500 transition-colors" />
                                                            </div>
                                                        </div>
                                                    ))
                                                ) : (
                                                    <div className="flex flex-col items-center justify-center py-12 bg-white rounded-xl shadow-sm">
                                                        <div className="h-12 w-12 bg-gray-50 rounded-full flex items-center justify-center mb-4">
                                                            <Briefcase className="h-6 w-6 text-gray-300" />
                                                        </div>
                                                        <p className="text-gray-500 font-medium">예정된 면접 일정이 없습니다.</p>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </TabsContent>

                            {/* 지원 제안 탭 */}
                            <TabsContent value="offers" className="mt-0">
                                <div className="grid gap-6">
                                    {offers.map((offer) => (
                                        <Card key={offer.id} className="border-none shadow-sm bg-white overflow-hidden hover:shadow-md transition-all">
                                            <CardContent className="p-8">
                                                <div className="flex flex-col lg:flex-row justify-between gap-8">
                                                    <div className="space-y-5 flex-1">
                                                        <div className="flex items-center flex-wrap gap-3">
                                                            <div className="p-2.5 bg-sky-50 rounded-xl">
                                                                <Building2 className="h-6 w-6 text-sky-500" />
                                                            </div>
                                                            <h3 className="font-bold text-xl text-gray-900">{offer.company}</h3>
                                                            {getOfferStatusBadge(offer.status)}
                                                        </div>
                                                        <div>
                                                            <p className="text-lg font-bold text-gray-700">{offer.position}</p>
                                                            <p className="text-sm text-sky-600 font-bold mt-1.5 flex items-center gap-1.5">
                                                                <span className="text-xl">💰</span> 예상 연봉: {offer.salary}
                                                            </p>
                                                        </div>
                                                        <div className="p-5 bg-[#FBFBFC] rounded-2xl border border-gray-50 flex items-start gap-4">
                                                            <MessageSquare className="h-5 w-5 text-gray-300 mt-1 flex-shrink-0" />
                                                            <p className="text-sm text-gray-600 leading-relaxed font-medium">
                                                                {offer.message}
                                                            </p>
                                                        </div>
                                                        <div className="flex items-center gap-5 text-xs font-bold uppercase tracking-wider text-gray-400">
                                                            <span className="flex items-center gap-2">
                                                                <Clock className="h-4 w-4" />
                                                                수신: {offer.receivedAt}
                                                            </span>
                                                            <span className="w-1 h-1 bg-gray-200 rounded-full" />
                                                            <span className="flex items-center gap-2 text-rose-400">
                                                                <XCircle className="h-4 w-4" />
                                                                제안 마감: {offer.deadline}
                                                            </span>
                                                        </div>
                                                    </div>

                                                    {offer.status === "pending" && (
                                                        <div className="flex flex-row lg:flex-col gap-3 lg:w-48">
                                                            <Button className="flex-1 btn-gradient-primary border-0">
                                                                <CheckCircle2 className="h-4 w-4 mr-2" />
                                                                수락하기
                                                            </Button>
                                                            <Button variant="outline" className="flex-1 border-gray-200 text-gray-600 hover:bg-gray-50">
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
                            </TabsContent>

                            {/* 알림 탭 */}
                            <TabsContent value="notifications" className="mt-0">
                                <Card className="border-none shadow-sm bg-white overflow-hidden">
                                    <CardHeader className="border-b border-gray-50 flex flex-row items-center justify-between py-6">
                                        <CardTitle className="text-xl font-bold">면접 알림 센터</CardTitle>
                                        <Button variant="ghost" size="sm" className="text-sky-500 hover:bg-sky-50 hover:text-sky-600 font-bold">
                                            모두 읽음 처리
                                        </Button>
                                    </CardHeader>
                                    <CardContent className="p-0">
                                        <div className="divide-y divide-gray-50">
                                            {notifications.map((notification) => (
                                                <div
                                                    key={notification.id}
                                                    className={cn(
                                                        "flex items-start gap-5 p-6 transition-all cursor-pointer group hover:bg-gray-50",
                                                        !notification.read && "bg-sky-50/30 ring-1 ring-inset ring-sky-50"
                                                    )}
                                                >
                                                    <div className={cn(
                                                        "h-12 w-12 rounded-2xl flex items-center justify-center flex-shrink-0 transition-transform group-hover:scale-110",
                                                        !notification.read ? "bg-sky-500 text-white shadow-lg shadow-sky-100" : "bg-gray-100 text-gray-400"
                                                    )}>
                                                        {notification.type === "interview_reminder" && <CalendarIcon className="h-6 w-6" />}
                                                        {notification.type === "offer_received" && <Briefcase className="h-6 w-6" />}
                                                    </div>
                                                    <div className="flex-1 space-y-1">
                                                        <div className="flex items-center justify-between">
                                                            <p className={cn("font-bold text-lg", !notification.read ? "text-gray-900" : "text-gray-500")}>
                                                                {notification.title}
                                                            </p>
                                                            <span className="text-xs font-bold text-gray-400">
                                                                <Clock className="h-3 w-3 inline mr-1" />
                                                                {notification.date}
                                                            </span>
                                                        </div>
                                                        <p className="text-sm text-gray-500 leading-relaxed">{notification.message}</p>
                                                    </div>
                                                    {!notification.read && (
                                                        <div className="h-2.5 w-2.5 rounded-full bg-sky-500 mt-2 flex-shrink-0 shadow-sm" />
                                                    )}
                                                </div>
                                            ))}
                                        </div>
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
