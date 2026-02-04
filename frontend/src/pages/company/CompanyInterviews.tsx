import { useState, useMemo, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  Plus,
  Clock,
  MapPin,
  Video,
  Phone,
  User,
  Pencil,
  Trash2,
  Loader2,
  AlertCircle,
  ListOrdered,
  CalendarDays,
} from "lucide-react";
import { useInterviews, useAllInterviews, useCreateInterview, useUpdateInterview, useDeleteInterview, useApplicants } from "@/hooks/useEmployers";
import { useQueryClient } from "@tanstack/react-query";

interface Interview {
  interviewId: number;
  applicationId: number;
  applicantName: string;
  jobTitle: string;
  stage: string;
  method: string;
  location?: string;
  meetingUrl?: string;
  startAt: string;
  endAt: string;
  status: string;
  memo?: string;
}

interface Applicant {
  applicationId: number;
  name: string;
  jobTitle: string;
  status: string;
}

const stageLabels: Record<string, string> = {
  "1ST": "1차 면접",
  "2ND": "2차 면접",
  FINAL: "최종 면접",
};

const methodLabels: Record<string, string> = {
  ONSITE: "대면",
  VIDEO: "화상",
  PHONE: "전화",
};

const methodIcons: Record<string, typeof MapPin> = {
  ONSITE: MapPin,
  VIDEO: Video,
  PHONE: Phone,
};

const statusLabels: Record<string, { label: string; className: string }> = {
  PROPOSED: { label: "제안됨", className: "bg-info/10 text-info" },
  CONFIRMED: { label: "확정", className: "bg-success/10 text-success" },
  CANCELED: { label: "취소됨", className: "bg-muted text-muted-foreground" },
  DONE: { label: "완료", className: "bg-secondary text-secondary-foreground" },
};

const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

export default function CompanyInterviews() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingInterview, setEditingInterview] = useState<Interview | null>(null);
  const queryClient = useQueryClient();

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth() + 1;

  const { data: interviewData, isLoading } = useInterviews(year, month);
  const { data: allInterviewData, isLoading: isLoadingAll } = useAllInterviews();
  const { data: applicantData } = useApplicants("");
  const createMutation = useCreateInterview();
  const updateMutation = useUpdateInterview();
  const deleteMutation = useDeleteInterview();

  const interviews: Interview[] = interviewData?.interviews ?? [];
  const allInterviews: Interview[] = allInterviewData?.interviews ?? [];
  const applicants: Applicant[] = applicantData?.applicants ?? [];

  // URL에서 applicationId가 있으면 해당 지원자로 모달 열기
  useEffect(() => {
    const applicationId = searchParams.get("applicationId");
    if (applicationId && applicants.length > 0) {
      const targetApplicant = applicants.find(
        (a) => String(a.applicationId) === applicationId
      );
      if (targetApplicant) {
        setForm({
          applicationId: applicationId,
          stage: "1ST",
          method: "ONSITE",
          location: "",
          meetingUrl: "",
          startAt: formatDateTimeLocal(new Date(), 10, 0),
          endAt: formatDateTimeLocal(new Date(), 11, 0),
          memo: "",
        });
        setEditingInterview(null);
        setShowModal(true);
        // URL에서 applicationId 제거
        searchParams.delete("applicationId");
        setSearchParams(searchParams, { replace: true });
      }
    }
  }, [applicants, searchParams, setSearchParams]);

  // 폼 상태
  const [form, setForm] = useState({
    applicationId: "",
    stage: "1ST",
    method: "ONSITE",
    location: "",
    meetingUrl: "",
    startAt: "",
    endAt: "",
    memo: "",
  });

  // 캘린더 날짜 계산
  const calendarDays = useMemo(() => {
    const firstDay = new Date(year, month - 1, 1);
    const lastDay = new Date(year, month, 0);
    const days: { date: Date; isCurrentMonth: boolean }[] = [];

    // 이전 달
    const startDayOfWeek = firstDay.getDay();
    for (let i = startDayOfWeek - 1; i >= 0; i--) {
      days.push({ date: new Date(year, month - 1, -i), isCurrentMonth: false });
    }

    // 현재 달
    for (let i = 1; i <= lastDay.getDate(); i++) {
      days.push({ date: new Date(year, month - 1, i), isCurrentMonth: true });
    }

    // 다음 달 (6주 = 42일)
    const remaining = 42 - days.length;
    for (let i = 1; i <= remaining; i++) {
      days.push({ date: new Date(year, month, i), isCurrentMonth: false });
    }

    return days;
  }, [year, month]);

  const getInterviewsForDate = (date: Date): Interview[] => {
    return interviews.filter((interview) => {
      const interviewDate = new Date(interview.startAt);
      return (
        interviewDate.getFullYear() === date.getFullYear() &&
        interviewDate.getMonth() === date.getMonth() &&
        interviewDate.getDate() === date.getDate()
      );
    });
  };

  const isToday = (date: Date) => {
    const today = new Date();
    return (
      date.getFullYear() === today.getFullYear() &&
      date.getMonth() === today.getMonth() &&
      date.getDate() === today.getDate()
    );
  };

  const formatTime = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
  };

  const formatDateTimeLocal = (date: Date, hour = 10, minute = 0) => {
    const d = new Date(date);
    d.setHours(hour, minute, 0, 0);
    return d.toISOString().slice(0, 16);
  };

  const handleDateClick = (date: Date) => {
    setSelectedDate(date);
  };

  const openCreateModal = (date?: Date) => {
    const targetDate = date || selectedDate || new Date();
    setForm({
      applicationId: "",
      stage: "1ST",
      method: "ONSITE",
      location: "",
      meetingUrl: "",
      startAt: formatDateTimeLocal(targetDate, 10, 0),
      endAt: formatDateTimeLocal(targetDate, 11, 0),
      memo: "",
    });
    setEditingInterview(null);
    setShowModal(true);
  };

  const openEditModal = (interview: Interview) => {
    setForm({
      applicationId: String(interview.applicationId),
      stage: interview.stage,
      method: interview.method,
      location: interview.location || "",
      meetingUrl: interview.meetingUrl || "",
      startAt: interview.startAt?.slice(0, 16) || "",
      endAt: interview.endAt?.slice(0, 16) || "",
      memo: interview.memo || "",
    });
    setEditingInterview(interview);
    setShowModal(true);
  };

  const handleSave = async () => {
    if (!form.applicationId || !form.startAt || !form.endAt) {
      alert("필수 항목을 입력해주세요.");
      return;
    }

    try {
      if (editingInterview) {
        await updateMutation.mutateAsync({
          interviewId: editingInterview.interviewId,
          interview: {
            ...form,
            applicationId: Number(form.applicationId),
          },
        });
      } else {
        await createMutation.mutateAsync({
          ...form,
          applicationId: Number(form.applicationId),
        });
      }
      setShowModal(false);
      queryClient.invalidateQueries({ queryKey: ["interviews"] });
    } catch (err: any) {
      alert(err.response?.data?.error || "저장에 실패했습니다.");
    }
  };

  const handleDelete = async (interviewId: number) => {
    if (!confirm("면접 일정을 삭제하시겠습니까?")) return;

    try {
      await deleteMutation.mutateAsync(interviewId);
      queryClient.invalidateQueries({ queryKey: ["interviews"] });
    } catch (err: any) {
      alert(err.response?.data?.error || "삭제에 실패했습니다.");
    }
  };

  const todayInterviews = getInterviewsForDate(selectedDate || new Date());

  // 전체 면접 일정 (날짜순 정렬) - 전체 조회 API 사용
  const allInterviewsSorted = useMemo(() => {
    return [...allInterviews].sort((a, b) => 
      new Date(b.startAt).getTime() - new Date(a.startAt).getTime()
    );
  }, [allInterviews]);

  // 다가오는 면접 (미래 일정만) - 전체 조회 API 사용
  const upcomingInterviews = useMemo(() => {
    const now = new Date();
    return allInterviews
      .filter((i) => new Date(i.startAt) >= now && i.status !== "CANCELED" && i.status !== "DONE")
      .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime());
  }, [allInterviews]);

  // 지난 면접 - 전체 조회 API 사용
  const pastInterviews = useMemo(() => {
    const now = new Date();
    return allInterviews
      .filter((i) => new Date(i.startAt) < now || i.status === "DONE")
      .sort((a, b) => new Date(b.startAt).getTime() - new Date(a.startAt).getTime());
  }, [allInterviews]);

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString("ko-KR", { month: "long", day: "numeric", weekday: "short" });
  };

  return (
    <div className="flex min-h-screen bg-background">
      <Sidebar />

      <div className="flex-1 flex flex-col lg:ml-64">
        <Header />

        <main className="flex-1 p-6">
          <div className="max-w-7xl mx-auto space-y-6">
            {/* 헤더 */}
            <div className="flex justify-between items-center">
              <div>
                <h1 className="text-2xl font-bold text-foreground">면접 일정</h1>
                <p className="text-muted-foreground">지원자와의 면접 일정을 관리하세요</p>
              </div>
              <Button onClick={() => openCreateModal()}>
                <Plus className="h-4 w-4 mr-2" />
                일정 추가
              </Button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* 캘린더 */}
              <Card className="lg:col-span-2">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="flex items-center gap-2">
                    <Calendar className="h-5 w-5 text-primary" />
                    면접 캘린더
                  </CardTitle>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="icon"
                      onClick={() =>
                        setCurrentDate(new Date(year, month - 2, 1))
                      }
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <span className="font-semibold min-w-[120px] text-center">
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
                  {isLoading ? (
                    <div className="flex items-center justify-center py-12">
                      <Loader2 className="h-8 w-8 animate-spin text-primary" />
                    </div>
                  ) : (
                    <div className="border rounded-lg overflow-hidden">
                      {/* 요일 헤더 */}
                      <div className="grid grid-cols-7 bg-muted">
                        {weekDays.map((day, idx) => (
                          <div
                            key={day}
                            className={`text-center py-2 text-sm font-medium ${
                              idx === 0
                                ? "text-red-500"
                                : idx === 6
                                ? "text-blue-500"
                                : ""
                            }`}
                          >
                            {day}
                          </div>
                        ))}
                      </div>
                      {/* 날짜 그리드 */}
                      <div className="grid grid-cols-7">
                        {calendarDays.map(({ date, isCurrentMonth }, idx) => {
                          const dayInterviews = getInterviewsForDate(date);
                          const isSelected =
                            selectedDate &&
                            date.toDateString() === selectedDate.toDateString();

                          return (
                            <div
                              key={idx}
                              onClick={() => handleDateClick(date)}
                              className={`min-h-[80px] p-1 border-t border-l cursor-pointer transition-colors
                                ${!isCurrentMonth ? "bg-muted/30 text-muted-foreground" : ""}
                                ${isToday(date) ? "bg-primary/5" : ""}
                                ${isSelected ? "bg-primary/10 ring-1 ring-primary" : ""}
                                hover:bg-muted/50
                              `}
                            >
                              <div
                                className={`text-sm font-medium mb-1 ${
                                  idx % 7 === 0
                                    ? "text-red-500"
                                    : idx % 7 === 6
                                    ? "text-blue-500"
                                    : ""
                                }`}
                              >
                                {date.getDate()}
                              </div>
                              {dayInterviews.slice(0, 2).map((interview) => {
                                const MethodIcon = methodIcons[interview.method] || MapPin;
                                return (
                                  <div
                                    key={interview.interviewId}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      openEditModal(interview);
                                    }}
                                    className={`text-xs p-1 rounded mb-0.5 truncate cursor-pointer hover:opacity-80
                                      ${
                                        interview.method === "VIDEO"
                                          ? "bg-info/20 text-info"
                                          : interview.method === "PHONE"
                                          ? "bg-warning/20 text-warning"
                                          : "bg-success/20 text-success"
                                      }
                                    `}
                                    title={`${interview.applicantName} - ${formatTime(interview.startAt)}`}
                                  >
                                    <MethodIcon className="h-3 w-3 inline mr-1" />
                                    {formatTime(interview.startAt)} {interview.applicantName}
                                  </div>
                                );
                              })}
                              {dayInterviews.length > 2 && (
                                <div className="text-xs text-muted-foreground">
                                  +{dayInterviews.length - 2}개 더
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* 면접 일정 탭 */}
              <Card>
                <Tabs defaultValue="selected" className="w-full">
                  <CardHeader className="pb-2">
                    <TabsList className="grid w-full grid-cols-3">
                      <TabsTrigger value="selected" className="text-xs">
                        <CalendarDays className="h-3 w-3 mr-1" />
                        선택 날짜
                      </TabsTrigger>
                      <TabsTrigger value="upcoming" className="text-xs">
                        <Clock className="h-3 w-3 mr-1" />
                        다가오는
                      </TabsTrigger>
                      <TabsTrigger value="all" className="text-xs">
                        <ListOrdered className="h-3 w-3 mr-1" />
                        전체
                      </TabsTrigger>
                    </TabsList>
                  </CardHeader>
                  <CardContent>
                    {/* 선택한 날짜의 일정 */}
                    <TabsContent value="selected" className="mt-0 space-y-3">
                      <div className="text-sm font-medium text-muted-foreground mb-2">
                        {selectedDate
                          ? `${selectedDate.getMonth() + 1}월 ${selectedDate.getDate()}일 일정`
                          : "오늘의 면접"}
                      </div>
                      {todayInterviews.length > 0 ? (
                        todayInterviews.map((interview) => {
                          const MethodIcon = methodIcons[interview.method] || MapPin;
                          const statusConfig = statusLabels[interview.status] || statusLabels.PROPOSED;

                          return (
                            <div
                              key={interview.interviewId}
                              className="p-3 rounded-lg border bg-card hover:shadow-sm transition-shadow"
                            >
                              <div className="flex justify-between items-start mb-2">
                                <div className="flex flex-wrap gap-1">
                                  <Badge
                                    variant="outline"
                                    className={
                                      interview.method === "VIDEO"
                                        ? "bg-info/10 text-info"
                                        : interview.method === "PHONE"
                                        ? "bg-warning/10 text-warning"
                                        : "bg-success/10 text-success"
                                    }
                                  >
                                    <MethodIcon className="h-3 w-3 mr-1" />
                                    {methodLabels[interview.method]}
                                  </Badge>
                                  <Badge variant="secondary">
                                    {stageLabels[interview.stage]}
                                  </Badge>
                                  <Badge variant="outline" className={statusConfig.className}>
                                    {statusConfig.label}
                                  </Badge>
                                </div>
                                <div className="flex gap-1">
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7"
                                    onClick={() => openEditModal(interview)}
                                  >
                                    <Pencil className="h-3 w-3" />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7 text-destructive"
                                    onClick={() => handleDelete(interview.interviewId)}
                                  >
                                    <Trash2 className="h-3 w-3" />
                                  </Button>
                                </div>
                              </div>
                              <div className="flex items-center gap-2 mb-1">
                                <User className="h-4 w-4 text-muted-foreground" />
                                <span className="font-medium">{interview.applicantName}</span>
                              </div>
                              <div className="text-sm text-muted-foreground mb-1">
                                {interview.jobTitle}
                              </div>
                              <div className="text-sm text-muted-foreground flex items-center gap-1">
                                <Clock className="h-3 w-3" />
                                {formatTime(interview.startAt)} - {formatTime(interview.endAt)}
                              </div>
                              {interview.location && (
                                <div className="text-sm text-muted-foreground flex items-center gap-1 mt-1">
                                  <MapPin className="h-3 w-3" />
                                  {interview.location}
                                </div>
                              )}
                              {interview.meetingUrl && (
                                <div className="text-sm text-muted-foreground flex items-center gap-1 mt-1">
                                  <Video className="h-3 w-3" />
                                  <a
                                    href={interview.meetingUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="text-primary hover:underline truncate"
                                  >
                                    회의 링크
                                  </a>
                                </div>
                              )}
                            </div>
                          );
                        })
                      ) : (
                        <div className="text-center py-8 text-muted-foreground">
                          <Calendar className="h-12 w-12 mx-auto mb-2 opacity-50" />
                          <p>예정된 면접이 없습니다.</p>
                          <Button
                            variant="outline"
                            size="sm"
                            className="mt-3"
                            onClick={() => openCreateModal()}
                          >
                            <Plus className="h-4 w-4 mr-1" />
                            일정 추가
                          </Button>
                        </div>
                      )}
                    </TabsContent>

                    {/* 다가오는 면접 */}
                    <TabsContent value="upcoming" className="mt-0 space-y-3 max-h-[500px] overflow-y-auto">
                      <div className="text-sm font-medium text-muted-foreground mb-2">
                        다가오는 면접 ({upcomingInterviews.length}건)
                      </div>
                      {upcomingInterviews.length > 0 ? (
                        upcomingInterviews.map((interview) => {
                          const MethodIcon = methodIcons[interview.method] || MapPin;
                          const statusConfig = statusLabels[interview.status] || statusLabels.PROPOSED;

                          return (
                            <div
                              key={interview.interviewId}
                              className="p-3 rounded-lg border bg-card hover:shadow-sm transition-shadow"
                            >
                              <div className="flex justify-between items-start mb-2">
                                <div className="flex flex-wrap gap-1">
                                  <Badge
                                    variant="outline"
                                    className={
                                      interview.method === "VIDEO"
                                        ? "bg-info/10 text-info"
                                        : interview.method === "PHONE"
                                        ? "bg-warning/10 text-warning"
                                        : "bg-success/10 text-success"
                                    }
                                  >
                                    <MethodIcon className="h-3 w-3 mr-1" />
                                    {methodLabels[interview.method]}
                                  </Badge>
                                  <Badge variant="outline" className={statusConfig.className}>
                                    {statusConfig.label}
                                  </Badge>
                                </div>
                                <div className="flex gap-1">
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7"
                                    onClick={() => openEditModal(interview)}
                                  >
                                    <Pencil className="h-3 w-3" />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7 text-destructive"
                                    onClick={() => handleDelete(interview.interviewId)}
                                  >
                                    <Trash2 className="h-3 w-3" />
                                  </Button>
                                </div>
                              </div>
                              <div className="flex items-center gap-2 mb-1">
                                <User className="h-4 w-4 text-muted-foreground" />
                                <span className="font-medium">{interview.applicantName}</span>
                              </div>
                              <div className="text-sm text-muted-foreground mb-1">
                                {interview.jobTitle}
                              </div>
                              <div className="text-sm text-primary font-medium flex items-center gap-1">
                                <CalendarDays className="h-3 w-3" />
                                {formatDate(interview.startAt)}
                              </div>
                              <div className="text-sm text-muted-foreground flex items-center gap-1">
                                <Clock className="h-3 w-3" />
                                {formatTime(interview.startAt)} - {formatTime(interview.endAt)}
                              </div>
                            </div>
                          );
                        })
                      ) : (
                        <div className="text-center py-8 text-muted-foreground">
                          <Clock className="h-12 w-12 mx-auto mb-2 opacity-50" />
                          <p>다가오는 면접이 없습니다.</p>
                        </div>
                      )}
                    </TabsContent>

                    {/* 전체 면접 일정 */}
                    <TabsContent value="all" className="mt-0 space-y-3 max-h-[500px] overflow-y-auto">
                      <div className="text-sm font-medium text-muted-foreground mb-2">
                        전체 면접 일정 ({allInterviewsSorted.length}건)
                      </div>
                      {allInterviewsSorted.length > 0 ? (
                        allInterviewsSorted.map((interview) => {
                          const MethodIcon = methodIcons[interview.method] || MapPin;
                          const statusConfig = statusLabels[interview.status] || statusLabels.PROPOSED;
                          const isPast = new Date(interview.startAt) < new Date();

                          return (
                            <div
                              key={interview.interviewId}
                              className={`p-3 rounded-lg border bg-card hover:shadow-sm transition-shadow ${
                                isPast ? "opacity-60" : ""
                              }`}
                            >
                              <div className="flex justify-between items-start mb-2">
                                <div className="flex flex-wrap gap-1">
                                  <Badge
                                    variant="outline"
                                    className={
                                      interview.method === "VIDEO"
                                        ? "bg-info/10 text-info"
                                        : interview.method === "PHONE"
                                        ? "bg-warning/10 text-warning"
                                        : "bg-success/10 text-success"
                                    }
                                  >
                                    <MethodIcon className="h-3 w-3 mr-1" />
                                    {methodLabels[interview.method]}
                                  </Badge>
                                  <Badge variant="secondary">
                                    {stageLabels[interview.stage]}
                                  </Badge>
                                  <Badge variant="outline" className={statusConfig.className}>
                                    {statusConfig.label}
                                  </Badge>
                                </div>
                                <div className="flex gap-1">
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7"
                                    onClick={() => openEditModal(interview)}
                                  >
                                    <Pencil className="h-3 w-3" />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7 text-destructive"
                                    onClick={() => handleDelete(interview.interviewId)}
                                  >
                                    <Trash2 className="h-3 w-3" />
                                  </Button>
                                </div>
                              </div>
                              <div className="flex items-center gap-2 mb-1">
                                <User className="h-4 w-4 text-muted-foreground" />
                                <span className="font-medium">{interview.applicantName}</span>
                              </div>
                              <div className="text-sm text-muted-foreground mb-1">
                                {interview.jobTitle}
                              </div>
                              <div className={`text-sm font-medium flex items-center gap-1 ${isPast ? "text-muted-foreground" : "text-primary"}`}>
                                <CalendarDays className="h-3 w-3" />
                                {formatDate(interview.startAt)}
                              </div>
                              <div className="text-sm text-muted-foreground flex items-center gap-1">
                                <Clock className="h-3 w-3" />
                                {formatTime(interview.startAt)} - {formatTime(interview.endAt)}
                              </div>
                            </div>
                          );
                        })
                      ) : (
                        <div className="text-center py-8 text-muted-foreground">
                          <ListOrdered className="h-12 w-12 mx-auto mb-2 opacity-50" />
                          <p>면접 일정이 없습니다.</p>
                        </div>
                      )}
                    </TabsContent>
                  </CardContent>
                </Tabs>
              </Card>
            </div>
          </div>
        </main>

        <Footer />
      </div>

      {/* 면접 일정 추가/수정 모달 */}
      <Dialog open={showModal} onOpenChange={setShowModal}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>
              {editingInterview ? "면접 일정 수정" : "면접 일정 추가"}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-4">
            {/* 지원자 선택 */}
            <div className="space-y-2">
              <Label>지원자 *</Label>
              <Select
                value={form.applicationId}
                onValueChange={(v) => setForm({ ...form, applicationId: v })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="지원자 선택" />
                </SelectTrigger>
                <SelectContent>
                  {applicants.map((applicant) => (
                    <SelectItem
                      key={applicant.applicationId}
                      value={String(applicant.applicationId)}
                    >
                      {applicant.name} - {applicant.jobTitle}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* 면접 단계 / 방식 */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>면접 단계</Label>
                <Select
                  value={form.stage}
                  onValueChange={(v) => setForm({ ...form, stage: v })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="1ST">1차 면접</SelectItem>
                    <SelectItem value="2ND">2차 면접</SelectItem>
                    <SelectItem value="FINAL">최종 면접</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>면접 방식</Label>
                <Select
                  value={form.method}
                  onValueChange={(v) => setForm({ ...form, method: v })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ONSITE">대면</SelectItem>
                    <SelectItem value="VIDEO">화상</SelectItem>
                    <SelectItem value="PHONE">전화</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* 일시 */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>시작 일시 *</Label>
                <Input
                  type="datetime-local"
                  value={form.startAt}
                  onChange={(e) => setForm({ ...form, startAt: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label>종료 일시 *</Label>
                <Input
                  type="datetime-local"
                  value={form.endAt}
                  onChange={(e) => setForm({ ...form, endAt: e.target.value })}
                />
              </div>
            </div>

            {/* 장소/URL */}
            {form.method === "ONSITE" && (
              <div className="space-y-2">
                <Label>면접 장소</Label>
                <Input
                  placeholder="예: 본사 3층 회의실"
                  value={form.location}
                  onChange={(e) => setForm({ ...form, location: e.target.value })}
                />
              </div>
            )}
            {form.method === "VIDEO" && (
              <div className="space-y-2">
                <Label>화상회의 URL</Label>
                <Input
                  placeholder="예: https://meet.google.com/xxx"
                  value={form.meetingUrl}
                  onChange={(e) => setForm({ ...form, meetingUrl: e.target.value })}
                />
              </div>
            )}

            {/* 메모 */}
            <div className="space-y-2">
              <Label>메모</Label>
              <Textarea
                placeholder="면접 관련 메모..."
                value={form.memo}
                onChange={(e) => setForm({ ...form, memo: e.target.value })}
              />
            </div>

            {/* 알림 안내 */}
            <div className="flex items-center gap-2 p-3 bg-info/10 rounded-lg text-sm text-info">
              <AlertCircle className="h-4 w-4" />
              <span>저장 시 지원자에게 면접 일정 알림이 발송됩니다.</span>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setShowModal(false)}>
              취소
            </Button>
            <Button
              onClick={handleSave}
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {createMutation.isPending || updateMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
              ) : null}
              {editingInterview ? "수정" : "저장"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
