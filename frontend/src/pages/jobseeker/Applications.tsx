// frontend/src/pages/jobseeker/Applications.tsx
import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import {
    Building2,
    Calendar,
    Clock,
    Check,
    X,
    Eye,
    FileText,
    ChevronRight,
    Briefcase,
    MapPin,
    DollarSign,
    Send,
    AlertCircle,
    Inbox,
    Video,
    Phone,
    CheckCircle2,
    XCircle,
    Loader2
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuth } from "@/contexts/AuthContext";
import { getMyApplications, cancelApplication } from "@/api/applications";
import { getInterviewsByApplication, respondToInterview, InterviewDTO } from "@/api/interviews";
import { toast } from "sonner";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { format, parseISO } from "date-fns";
import { ko } from "date-fns/locale";

interface Application {
    applicationId: number;
    jobId: number;
    jobTitle: string;
    companyName: string;
    companyLogo?: string;
    status: string;
    appliedAt: string;
    viewedAt?: string;
    location?: string;
    salary?: string;
}

const statusConfig: Record<string, { label: string; color: string; icon: any }> = {
    SUBMITTED: { label: "지원완료", color: "bg-blue-100 text-blue-800 border-blue-200", icon: Send },
    VIEWED: { label: "열람됨", color: "bg-indigo-100 text-indigo-800 border-indigo-200", icon: Eye },
    INTERVIEW: { label: "면접요청", color: "bg-purple-100 text-purple-800 border-purple-200", icon: Calendar },
    HIRED: { label: "합격", color: "bg-green-100 text-green-800 border-green-200", icon: Check },
    REJECTED: { label: "불합격", color: "bg-red-100 text-red-800 border-red-200", icon: X },
    CANCELED: { label: "취소됨", color: "bg-gray-100 text-gray-600 border-gray-200", icon: X },
};

export default function Applications() {
    const { user } = useAuth();
    const [applications, setApplications] = useState<Application[]>([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState("all");
    const [cancelTarget, setCancelTarget] = useState<Application | null>(null);
    const [canceling, setCanceling] = useState(false);
    
    // 면접 관련 상태
    const [interviewsMap, setInterviewsMap] = useState<Record<number, InterviewDTO[]>>({});
    const [respondingInterviewId, setRespondingInterviewId] = useState<number | null>(null);
    const [interviewResponseDialog, setInterviewResponseDialog] = useState<{
        open: boolean;
        type: "accept" | "decline" | null;
        interview: InterviewDTO | null;
        applicationId: number | null;
    }>({ open: false, type: null, interview: null, applicationId: null });

    useEffect(() => {
        loadApplications();
    }, []);

    // 면접 상태인 지원에 대해 면접 일정 조회
    useEffect(() => {
        const fetchInterviews = async () => {
            const interviewApps = applications.filter(app => app.status === "INTERVIEW");
            const newInterviewsMap: Record<number, InterviewDTO[]> = {};
            
            await Promise.all(
                interviewApps.map(async (app) => {
                    try {
                        const data = await getInterviewsByApplication(app.applicationId);
                        const interviews = Array.isArray(data) ? data : (data as any)?.data ?? [];
                        newInterviewsMap[app.applicationId] = interviews;
                    } catch {
                        newInterviewsMap[app.applicationId] = [];
                    }
                })
            );
            
            setInterviewsMap(newInterviewsMap);
        };
        
        if (applications.length > 0) {
            fetchInterviews();
        }
    }, [applications]);

    const loadApplications = async () => {
        try {
            setLoading(true);
            const data = await getMyApplications();
            setApplications(Array.isArray(data) ? data : data.content || []);
        } catch (error: any) {
            // 백엔드 API가 없을 경우를 대비해 빈 배열로 처리
            console.error("지원 현황 조회 실패:", error);
            setApplications([]);
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async () => {
        if (!cancelTarget) return;

        setCanceling(true);
        try {
            await cancelApplication(cancelTarget.applicationId);
            toast.success("지원이 취소되었습니다.");
            setCancelTarget(null);
            loadApplications(); // 새로고침
        } catch (error: any) {
            toast.error(error?.response?.data?.message || "지원 취소에 실패했습니다.");
        } finally {
            setCanceling(false);
        }
    };

    // 면접 응답 처리
    const handleInterviewResponse = async () => {
        const { type, interview, applicationId } = interviewResponseDialog;
        if (!interview || !type) return;

        setRespondingInterviewId(interview.interviewId);
        try {
            await respondToInterview(interview.interviewId, type === "accept" ? "ACCEPT" : "DECLINE");
            toast.success(type === "accept" ? "면접 일정을 수락했습니다!" : "면접 일정을 거절했습니다.");
            setInterviewResponseDialog({ open: false, type: null, interview: null, applicationId: null });
            
            // 면접 목록 새로고침
            if (applicationId) {
                const data = await getInterviewsByApplication(applicationId);
                const interviews = Array.isArray(data) ? data : (data as any)?.data ?? [];
                setInterviewsMap(prev => ({ ...prev, [applicationId]: interviews }));
            }
            
            loadApplications(); // 지원 목록도 새로고침
        } catch (error: any) {
            toast.error(error?.response?.data?.message || "응답 처리에 실패했습니다.");
        } finally {
            setRespondingInterviewId(null);
        }
    };

    // 면접 방식 라벨
    const getMethodLabel = (method: string) => {
        switch (method) {
            case "ONSITE": return "대면 면접";
            case "VIDEO": return "화상 면접";
            case "PHONE": return "전화 면접";
            default: return method;
        }
    };

    // 면접 상태 라벨
    const getInterviewStatusBadge = (status: string) => {
        switch (status) {
            case "PROPOSED":
                return <Badge className="bg-yellow-100 text-yellow-800 border-yellow-200">일정 제안</Badge>;
            case "CONFIRMED":
                return <Badge className="bg-green-100 text-green-800 border-green-200">확정됨</Badge>;
            case "CANCELED":
                return <Badge className="bg-gray-100 text-gray-600 border-gray-200">취소됨</Badge>;
            case "DONE":
                return <Badge className="bg-blue-100 text-blue-800 border-blue-200">완료</Badge>;
            default:
                return <Badge variant="outline">{status}</Badge>;
        }
    };

    const filteredApplications = applications.filter(app => {
        if (activeTab === "all") return true;
        if (activeTab === "progress") return ["SUBMITTED", "VIEWED", "INTERVIEW"].includes(app.status);
        if (activeTab === "completed") return ["HIRED", "REJECTED", "CANCELED"].includes(app.status);
        return true;
    });

    const formatDate = (dateStr?: string) => {
        if (!dateStr) return "-";
        return new Date(dateStr).toLocaleDateString("ko-KR", {
            year: "numeric",
            month: "long",
            day: "numeric"
        });
    };

    // 상태별 카운트
    const counts = {
        all: applications.length,
        progress: applications.filter(a => ["SUBMITTED", "VIEWED", "INTERVIEW"].includes(a.status)).length,
        completed: applications.filter(a => ["HIRED", "REJECTED", "CANCELED"].includes(a.status)).length,
    };

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64">
                <Header />

                <main className="container max-w-4xl mx-auto py-6 px-4 md:px-8">
                    {/* 헤더 */}
                    <div className="mb-8">
                        <div className="flex items-center gap-3 mb-2">
                            <div className="p-2 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-600">
                                <Briefcase className="h-6 w-6 text-white" />
                            </div>
                            <h1 className="text-2xl font-bold">입사지원 현황</h1>
                        </div>
                        <p className="text-muted-foreground">
                            지원한 공고의 진행 상태를 확인하세요.
                        </p>
                    </div>

                    {/* 상태 요약 카드 */}
                    <div className="grid grid-cols-3 gap-4 mb-6">
                        <Card className="bg-gradient-to-br from-blue-50 to-cyan-50 border-blue-100">
                            <CardContent className="p-4 text-center">
                                <p className="text-2xl font-bold text-blue-600">{counts.all}</p>
                                <p className="text-sm text-muted-foreground">전체 지원</p>
                            </CardContent>
                        </Card>
                        <Card className="bg-gradient-to-br from-purple-50 to-indigo-50 border-purple-100">
                            <CardContent className="p-4 text-center">
                                <p className="text-2xl font-bold text-purple-600">{counts.progress}</p>
                                <p className="text-sm text-muted-foreground">진행 중</p>
                            </CardContent>
                        </Card>
                        <Card className="bg-gradient-to-br from-green-50 to-emerald-50 border-green-100">
                            <CardContent className="p-4 text-center">
                                <p className="text-2xl font-bold text-green-600">{counts.completed}</p>
                                <p className="text-sm text-muted-foreground">완료</p>
                            </CardContent>
                        </Card>
                    </div>

                    {/* 탭 필터 */}
                    <Tabs value={activeTab} onValueChange={setActiveTab} className="mb-6">
                        <TabsList className="grid w-full grid-cols-3">
                            <TabsTrigger value="all">전체 ({counts.all})</TabsTrigger>
                            <TabsTrigger value="progress">진행중 ({counts.progress})</TabsTrigger>
                            <TabsTrigger value="completed">완료 ({counts.completed})</TabsTrigger>
                        </TabsList>
                    </Tabs>

                    {/* 지원 목록 */}
                    {loading ? (
                        <div className="flex items-center justify-center py-20">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                        </div>
                    ) : filteredApplications.length === 0 ? (
                        <Card className="text-center py-16">
                            <CardContent>
                                <Inbox className="h-16 w-16 text-muted-foreground/30 mx-auto mb-4" />
                                <h3 className="text-lg font-medium mb-2">지원 내역이 없습니다</h3>
                                <p className="text-muted-foreground text-sm mb-4">
                                    관심 있는 공고에 지원해보세요!
                                </p>
                                <Button asChild>
                                    <Link to="/jobs">채용공고 보러가기</Link>
                                </Button>
                            </CardContent>
                        </Card>
                    ) : (
                        <div className="space-y-4">
                            {filteredApplications.map((app) => {
                                const status = statusConfig[app.status] || statusConfig.SUBMITTED;
                                const StatusIcon = status.icon;
                                const canCancel = app.status === "SUBMITTED";

                                return (
                                    <Card
                                        key={app.applicationId}
                                        className="hover:shadow-md transition-shadow"
                                    >
                                        <CardContent className="p-5">
                                            <div className="flex items-start justify-between gap-4">
                                                <div className="flex items-start gap-4 flex-1">
                                                    {/* 기업 로고 */}
                                                    <Avatar className="h-14 w-14 rounded-xl border">
                                                        <AvatarImage src={app.companyLogo} />
                                                        <AvatarFallback className="rounded-xl bg-gradient-to-br from-blue-50 to-indigo-100 text-blue-600 font-bold">
                                                            {app.companyName?.charAt(0) || "?"}
                                                        </AvatarFallback>
                                                    </Avatar>

                                                    {/* 공고 정보 */}
                                                    <div className="flex-1 min-w-0">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <Badge className={`${status.color} border`}>
                                                                <StatusIcon className="h-3 w-3 mr-1" />
                                                                {status.label}
                                                            </Badge>
                                                        </div>

                                                        <Link
                                                            to={`/jobs/${app.jobId}`}
                                                            className="font-bold text-lg hover:text-primary transition-colors truncate block"
                                                        >
                                                            {app.jobTitle}
                                                        </Link>

                                                        <p className="text-sm text-muted-foreground flex items-center gap-1 mb-2">
                                                            <Building2 className="h-3.5 w-3.5" />
                                                            {app.companyName}
                                                        </p>

                                                        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                                                            {app.location && (
                                                                <span className="flex items-center gap-1">
                                                                    <MapPin className="h-3 w-3" />
                                                                    {app.location}
                                                                </span>
                                                            )}
                                                            <span className="flex items-center gap-1">
                                                                <Calendar className="h-3 w-3" />
                                                                {formatDate(app.appliedAt)} 지원
                                                            </span>
                                                            {app.viewedAt && (
                                                                <span className="flex items-center gap-1">
                                                                    <Eye className="h-3 w-3" />
                                                                    {formatDate(app.viewedAt)} 열람
                                                                </span>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>

                                                {/* 액션 버튼 */}
                                                <div className="flex flex-col items-end gap-2">
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        asChild
                                                    >
                                                        <Link to={`/jobs/${app.jobId}`}>
                                                            공고 보기
                                                            <ChevronRight className="h-4 w-4 ml-1" />
                                                        </Link>
                                                    </Button>

                                                    {canCancel && (
                                                        <Button
                                                            variant="outline"
                                                            size="sm"
                                                            className="text-destructive hover:text-destructive"
                                                            onClick={() => setCancelTarget(app)}
                                                        >
                                                            지원 취소
                                                        </Button>
                                                    )}
                                                </div>
                                            </div>

                                            {/* 면접 일정 정보 (INTERVIEW 상태일 때) */}
                                            {app.status === "INTERVIEW" && interviewsMap[app.applicationId]?.length > 0 && (
                                                <div className="mt-4 pt-4 border-t border-dashed">
                                                    <p className="text-sm font-semibold text-purple-700 mb-3 flex items-center gap-2">
                                                        <Calendar className="h-4 w-4" />
                                                        면접 일정
                                                    </p>
                                                    <div className="space-y-3">
                                                        {interviewsMap[app.applicationId].map((interview) => (
                                                            <div
                                                                key={interview.interviewId}
                                                                className="p-4 rounded-lg bg-gradient-to-br from-purple-50 to-indigo-50 border border-purple-100"
                                                            >
                                                                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                                                                    <div className="space-y-2">
                                                                        <div className="flex items-center gap-2 flex-wrap">
                                                                            {getInterviewStatusBadge(interview.status)}
                                                                            <Badge variant="outline" className="bg-white">
                                                                                {interview.method === "VIDEO" && <Video className="h-3 w-3 mr-1" />}
                                                                                {interview.method === "PHONE" && <Phone className="h-3 w-3 mr-1" />}
                                                                                {interview.method === "ONSITE" && <MapPin className="h-3 w-3 mr-1" />}
                                                                                {getMethodLabel(interview.method)}
                                                                            </Badge>
                                                                            <Badge variant="secondary">
                                                                                {interview.stage === "FIRST" ? "1차 면접" :
                                                                                 interview.stage === "SECOND" ? "2차 면접" : "최종 면접"}
                                                                            </Badge>
                                                                        </div>
                                                                        <div className="flex items-center gap-4 text-sm text-gray-700">
                                                                            <span className="flex items-center gap-1.5 font-medium">
                                                                                <Calendar className="h-4 w-4 text-purple-500" />
                                                                                {format(parseISO(interview.startAt), "M월 d일 (EEE)", { locale: ko })}
                                                                            </span>
                                                                            <span className="flex items-center gap-1.5">
                                                                                <Clock className="h-4 w-4 text-purple-500" />
                                                                                {format(parseISO(interview.startAt), "a h:mm", { locale: ko })}
                                                                            </span>
                                                                        </div>
                                                                        {interview.location && (
                                                                            <p className="text-sm text-gray-600 flex items-center gap-1.5">
                                                                                <MapPin className="h-4 w-4 text-gray-400" />
                                                                                {interview.location}
                                                                            </p>
                                                                        )}
                                                                        {interview.meetingUrl && (
                                                                            <a
                                                                                href={interview.meetingUrl}
                                                                                target="_blank"
                                                                                rel="noopener noreferrer"
                                                                                className="text-sm text-primary hover:underline flex items-center gap-1.5"
                                                                            >
                                                                                <Video className="h-4 w-4" />
                                                                                화상 면접 링크
                                                                            </a>
                                                                        )}
                                                                    </div>

                                                                    {/* 면접 수락/거절 버튼 (PROPOSED 상태일 때만) */}
                                                                    {interview.status === "PROPOSED" && (
                                                                        <div className="flex gap-2">
                                                                            <Button
                                                                                size="sm"
                                                                                className="bg-green-600 hover:bg-green-700"
                                                                                onClick={() => setInterviewResponseDialog({
                                                                                    open: true,
                                                                                    type: "accept",
                                                                                    interview,
                                                                                    applicationId: app.applicationId
                                                                                })}
                                                                                disabled={respondingInterviewId === interview.interviewId}
                                                                            >
                                                                                <CheckCircle2 className="h-4 w-4 mr-1" />
                                                                                수락
                                                                            </Button>
                                                                            <Button
                                                                                size="sm"
                                                                                variant="outline"
                                                                                className="text-red-600 border-red-200 hover:bg-red-50"
                                                                                onClick={() => setInterviewResponseDialog({
                                                                                    open: true,
                                                                                    type: "decline",
                                                                                    interview,
                                                                                    applicationId: app.applicationId
                                                                                })}
                                                                                disabled={respondingInterviewId === interview.interviewId}
                                                                            >
                                                                                <XCircle className="h-4 w-4 mr-1" />
                                                                                거절
                                                                            </Button>
                                                                        </div>
                                                                    )}

                                                                    {interview.status === "CONFIRMED" && (
                                                                        <Button
                                                                            size="sm"
                                                                            variant="outline"
                                                                            asChild
                                                                        >
                                                                            <Link to="/interview">
                                                                                <Calendar className="h-4 w-4 mr-1" />
                                                                                면접 관리
                                                                            </Link>
                                                                        </Button>
                                                                    )}
                                                                </div>
                                                            </div>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}
                                        </CardContent>
                                    </Card>
                                );
                            })}
                        </div>
                    )}
                </main>

                <Footer />
            </div>

            {/* 지원 취소 확인 다이얼로그 */}
            <AlertDialog open={!!cancelTarget} onOpenChange={() => setCancelTarget(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle className="flex items-center gap-2">
                            <AlertCircle className="h-5 w-5 text-destructive" />
                            지원을 취소하시겠습니까?
                        </AlertDialogTitle>
                        <AlertDialogDescription>
                            {cancelTarget?.companyName}의 '{cancelTarget?.jobTitle}' 공고 지원을 취소합니다.
                            취소 후에는 다시 지원할 수 있습니다.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>돌아가기</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleCancel}
                            disabled={canceling}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                            {canceling ? "취소 중..." : "지원 취소"}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {/* 면접 응답 확인 다이얼로그 */}
            <Dialog 
                open={interviewResponseDialog.open} 
                onOpenChange={(open) => !open && setInterviewResponseDialog({ open: false, type: null, interview: null, applicationId: null })}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2">
                            {interviewResponseDialog.type === "accept" ? (
                                <>
                                    <CheckCircle2 className="h-5 w-5 text-green-600" />
                                    면접 일정 수락
                                </>
                            ) : (
                                <>
                                    <XCircle className="h-5 w-5 text-red-600" />
                                    면접 일정 거절
                                </>
                            )}
                        </DialogTitle>
                        <DialogDescription className="space-y-3 pt-2">
                            {interviewResponseDialog.interview && (
                                <div className="p-4 rounded-lg bg-gray-50 space-y-2">
                                    <p className="font-medium text-gray-900">
                                        {interviewResponseDialog.interview.companyName} - {interviewResponseDialog.interview.jobTitle}
                                    </p>
                                    <div className="flex items-center gap-3 text-sm text-gray-600">
                                        <span className="flex items-center gap-1">
                                            <Calendar className="h-4 w-4" />
                                            {format(parseISO(interviewResponseDialog.interview.startAt), "M월 d일 (EEE) a h:mm", { locale: ko })}
                                        </span>
                                    </div>
                                    {interviewResponseDialog.interview.location && (
                                        <p className="text-sm text-gray-600 flex items-center gap-1">
                                            <MapPin className="h-4 w-4" />
                                            {interviewResponseDialog.interview.location}
                                        </p>
                                    )}
                                </div>
                            )}
                            <p>
                                {interviewResponseDialog.type === "accept"
                                    ? "이 면접 일정을 수락하시겠습니까? 수락 시 기업에 알림이 발송됩니다."
                                    : "이 면접 일정을 거절하시겠습니까? 거절 시 기업에 알림이 발송됩니다."}
                            </p>
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setInterviewResponseDialog({ open: false, type: null, interview: null, applicationId: null })}
                        >
                            취소
                        </Button>
                        <Button
                            onClick={handleInterviewResponse}
                            disabled={respondingInterviewId !== null}
                            className={interviewResponseDialog.type === "accept" ? "bg-green-600 hover:bg-green-700" : "bg-red-600 hover:bg-red-700"}
                        >
                            {respondingInterviewId !== null && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                            {interviewResponseDialog.type === "accept" ? "수락하기" : "거절하기"}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
