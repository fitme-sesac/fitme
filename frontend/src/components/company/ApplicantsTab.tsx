import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { 
  Mail, 
  Calendar, 
  FileText, 
  MessageSquare,
  CheckCircle,
  XCircle,
  Clock,
  ChevronDown,
  ChevronUp,
  Sparkles,
  Loader2,
  CheckCircle2,
  AlertCircle,
  UserCheck,
  UserX,
  CalendarPlus
} from "lucide-react";
import { useApplicants, useUpdateApplicantStatus } from "@/hooks/useEmployers";
import { toast } from "sonner";
import { getResume } from "@/api/resumes";

const statusConfig: Record<string, { label: string; className: string; icon: typeof Clock }> = {
  SUBMITTED: { label: "지원완료", className: "bg-info/10 text-info border-info/20", icon: Clock },
  submitted: { label: "지원완료", className: "bg-info/10 text-info border-info/20", icon: Clock },
  VIEWED: { label: "검토중", className: "bg-info/10 text-info border-info/20", icon: Clock },
  INTERVIEW: { label: "면접예정", className: "bg-warning/10 text-warning border-warning/20", icon: Calendar },
  interview: { label: "면접예정", className: "bg-warning/10 text-warning border-warning/20", icon: Calendar },
  HIRED: { label: "채용확정", className: "bg-success/10 text-success border-success/20", icon: CheckCircle },
  hired: { label: "채용확정", className: "bg-success/10 text-success border-success/20", icon: CheckCircle },
  REJECTED: { label: "불합격", className: "bg-muted text-muted-foreground border-border", icon: XCircle },
  rejected: { label: "불합격", className: "bg-muted text-muted-foreground border-border", icon: XCircle },
  CANCELED: { label: "취소", className: "bg-muted text-muted-foreground border-border", icon: XCircle },
};

const matchLevelLabel: Record<string, string> = {
  EXCELLENT: "🎯 아주 잘 맞아요!",
  GOOD: "👍 잘 맞는 인재예요",
  MODERATE: "📚 도전해볼 만해요",
  LOW: "🌱 새로운 인재예요",
};

function matchRateDisplay(matchInfo: { overallMatchRate?: number; matchRate?: number } | null | undefined): number {
  if (!matchInfo) return 0;
  return matchInfo.overallMatchRate ?? matchInfo.matchRate ?? 0;
}

export function ApplicantsTab() {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState("");
  const [sortBy, setSortBy] = useState<"latest" | "match">("match");
  const [expandedId, setExpandedId] = useState<number | null>(null);
  
  // 상태 변경 다이얼로그
  const [actionDialog, setActionDialog] = useState<{
    open: boolean;
    type: "interview" | "hire" | "reject" | null;
    applicant: any;
  }>({ open: false, type: null, applicant: null });

  const { data, isLoading, error, refetch } = useApplicants(statusFilter);
  const updateStatusMutation = useUpdateApplicantStatus();
  const applicants = data?.applicants ?? [];
  const total = data?.total ?? 0;

  // 지원자 상태 변경 처리
  const handleStatusChange = async (status: string) => {
    if (!actionDialog.applicant) return;
    
    try {
      await updateStatusMutation.mutateAsync({
        applicationId: actionDialog.applicant.applicationId,
        status,
      });
      
      toast.success(
        status === "INTERVIEW" ? "면접 상태로 변경되었습니다. 면접 일정 페이지에서 일정을 추가해주세요." :
        status === "HIRED" ? "채용이 확정되었습니다!" :
        "지원자 상태가 변경되었습니다."
      );
      
      setActionDialog({ open: false, type: null, applicant: null });
      refetch();
      
      // 면접 선택 시 면접 페이지로 이동 옵션 제공
      if (status === "INTERVIEW") {
        navigate(`/company/interviews?applicationId=${actionDialog.applicant.applicationId}`);
      }
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "상태 변경에 실패했습니다.");
    }
  };

  const sortedApplicants = useMemo(() => {
    const list = [...applicants];
    if (sortBy === "match") {
      list.sort((a, b) => matchRateDisplay(b.matchInfo) - matchRateDisplay(a.matchInfo));
    } else {
      list.sort((a, b) => (b.appliedAt || "").localeCompare(a.appliedAt || ""));
    }
    return list;
  }, [applicants, sortBy]);

  const stats = useMemo(() => {
    const submitted = applicants.filter((a) => (a.status || "").toUpperCase() === "SUBMITTED" || a.status === "submitted").length;
    const interview = applicants.filter((a) => (a.status || "").toUpperCase() === "INTERVIEW" || a.status === "interview").length;
    const hired = applicants.filter((a) => (a.status || "").toUpperCase() === "HIRED" || a.status === "hired").length;
    return { total, submitted, interview, hired };
  }, [applicants, total]);

  const [resumeDialog, setResumeDialog] = useState<{
    open: boolean;
    loading: boolean;
    error: string | null;
    resume: any | null;
  }>({
    open: false,
    loading: false,
    error: null,
    resume: null,
  });

  const handleOpenResume = async (applicant: any) => {
    if (!applicant.resumeId) {
      toast.error("이력서 정보가 없습니다.");
      return;
    }

    setResumeDialog({
      open: true,
      loading: true,
      error: null,
      resume: null,
    });

    try {
      const res = await getResume(applicant.resumeId);
      setResumeDialog((prev) => ({
        ...prev,
        loading: false,
        resume: res,
      }));
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || "이력서를 불러오지 못했습니다.";
      toast.error(msg);
      setResumeDialog((prev) => ({
        ...prev,
        loading: false,
        error: msg,
      }));
    }
  };

  const renderResumeContent = (resume: any) => {
    if (!resume) return null;

    const skills: string[] = Array.isArray(resume.reStack)
      ? resume.reStack
      : typeof resume.reStack === "string"
        ? resume.reStack.split(",").map((s: string) => s.trim()).filter(Boolean)
        : [];

    const careers = Array.isArray(resume.careers) ? resume.careers : [];

    return (
      <div className="space-y-5 py-2">
        <div>
          <h3 className="text-lg font-semibold text-foreground mb-1">
            {resume.title ?? "제목 없는 이력서"}
          </h3>
          {resume.summary && (
            <p className="text-sm text-muted-foreground whitespace-pre-wrap">
              {resume.summary}
            </p>
          )}
        </div>

        {careers.length > 0 && (
          <div className="space-y-2">
            <p className="text-sm font-semibold text-foreground">경력</p>
            <ul className="space-y-2">
              {careers.map((c: any) => (
                <li key={c.id} className="border-b last:border-0 pb-2 last:pb-0">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium text-sm text-foreground">
                      {c.companyName ?? "(회사명)"}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {c.startDate ?? ""} ~ {c.endDate ?? (c.current ? "재직 중" : "")}
                    </span>
                  </div>
                  {c.role && (
                    <p className="text-xs text-primary mt-0.5">
                      {c.role}
                    </p>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}

        {skills.length > 0 && (
          <div className="space-y-2">
            <p className="text-sm font-semibold text-foreground">기술 스택</p>
            <div className="flex flex-wrap gap-1.5">
              {skills.map((s, idx) => (
                <Badge key={idx} variant="secondary" className="text-xs">
                  {s}
                </Badge>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-6 text-center text-muted-foreground">
          지원자 목록을 불러오지 못했습니다. 로그인(기업회원) 후 다시 시도해 주세요.
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {/* 필터 */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4">
            <Select value={statusFilter || "all"} onValueChange={(v) => setStatusFilter(v === "all" ? "" : v)}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="상태" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체 상태</SelectItem>
                <SelectItem value="SUBMITTED">지원완료</SelectItem>
                <SelectItem value="INTERVIEW">면접예정</SelectItem>
                <SelectItem value="HIRED">채용확정</SelectItem>
                <SelectItem value="REJECTED">불합격</SelectItem>
              </SelectContent>
            </Select>
            <Select value={sortBy} onValueChange={(v) => setSortBy(v as "latest" | "match")}>
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="정렬" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="latest">최신순</SelectItem>
                <SelectItem value="match">매칭률순</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* 통계 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-foreground">{stats.total}</p>
            <p className="text-sm text-muted-foreground">전체 지원자</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-info">{stats.submitted}</p>
            <p className="text-sm text-muted-foreground">검토대기</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-warning">{stats.interview}</p>
            <p className="text-sm text-muted-foreground">면접예정</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-success">{stats.hired}</p>
            <p className="text-sm text-muted-foreground">채용확정</p>
          </CardContent>
        </Card>
      </div>

      {/* 지원자 목록 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">지원자 목록 (채용공고 vs 이력서 스택 매칭)</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {sortedApplicants.length === 0 ? (
            <p className="text-center py-8 text-muted-foreground">지원자가 없습니다.</p>
          ) : (
            sortedApplicants.map((applicant) => {
              const statusKey = (applicant.status || "").toUpperCase();
              const config = statusConfig[applicant.status || ""] ?? statusConfig[statusKey] ?? statusConfig.SUBMITTED;
              const StatusIcon = config.icon;
              const matchInfo = applicant.matchInfo;
              const rate = matchRateDisplay(matchInfo);
              const isExpanded = expandedId === applicant.applicationId;

              return (
                <div
                  key={applicant.applicationId}
                  className="p-4 rounded-xl border border-border bg-card hover:shadow-card-hover transition-all cursor-pointer"
                  role="button"
                  tabIndex={0}
                  onClick={() => setExpandedId(isExpanded ? null : applicant.applicationId)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      setExpandedId(isExpanded ? null : applicant.applicationId);
                    }
                  }}
                >
                  <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                    <div className="flex items-start gap-4">
                      <Avatar className="h-12 w-12">
                        <AvatarFallback className="bg-primary/10 text-primary">
                          {(applicant.name || "?").slice(0, 2)}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="flex items-center gap-2 mb-1">
                          <h3 className="font-semibold text-foreground">{applicant.name || "-"}</h3>
                          <Badge variant="outline" className={config.className}>
                            <StatusIcon className="h-3 w-3 mr-1" />
                            {config.label}
                          </Badge>
                        </div>
                        <p className="text-sm text-muted-foreground mb-2">{applicant.jobTitle || "-"}</p>
                        <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Mail className="h-3 w-3" />
                            {applicant.email || "-"}
                          </span>
                          <span className="flex items-center gap-1">
                            <Calendar className="h-3 w-3" />
                            {applicant.appliedAt ? new Date(applicant.appliedAt).toLocaleDateString("ko-KR") : "-"}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div
                      className="flex items-center gap-4"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {/* AI 매칭 점수 (채용공고 stack vs 이력서 re_stack·tech_stack) */}
                      <div className="text-center px-4 py-2 rounded-lg bg-primary/5 border border-primary/20">
                        <p className="text-2xl font-bold text-primary">{rate}%</p>
                        <p className="text-xs text-muted-foreground">AI 매칭</p>
                      </div>

                      <div className="flex flex-col gap-2">
                        {/* 상태별 액션 버튼 */}
                        {(statusKey === "SUBMITTED" || statusKey === "VIEWED") && (
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              className="bg-primary hover:bg-primary/90"
                              onClick={() => setActionDialog({ open: true, type: "interview", applicant })}
                            >
                              <CalendarPlus className="h-4 w-4 mr-1" />
                              면접
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              className="text-green-600 border-green-200 hover:bg-green-50"
                              onClick={() => setActionDialog({ open: true, type: "hire", applicant })}
                            >
                              <UserCheck className="h-4 w-4 mr-1" />
                              채용
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              className="text-red-600 border-red-200 hover:bg-red-50"
                              onClick={() => setActionDialog({ open: true, type: "reject", applicant })}
                            >
                              <UserX className="h-4 w-4 mr-1" />
                              거절
                            </Button>
                          </div>
                        )}
                        {statusKey === "INTERVIEW" && (
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => navigate(`/company/interviews?applicationId=${applicant.applicationId}`)}
                            >
                              <Calendar className="h-4 w-4 mr-1" />
                              면접 일정
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              className="text-green-600 border-green-200 hover:bg-green-50"
                              onClick={() => setActionDialog({ open: true, type: "hire", applicant })}
                            >
                              <UserCheck className="h-4 w-4 mr-1" />
                              채용
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              className="text-red-600 border-red-200 hover:bg-red-50"
                              onClick={() => setActionDialog({ open: true, type: "reject", applicant })}
                            >
                              <UserX className="h-4 w-4 mr-1" />
                              거절
                            </Button>
                          </div>
                        )}
                        
                        <div className="flex gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            title={applicant.resumeTitle ? `이력서 보기: ${applicant.resumeTitle}` : "이력서 보기"}
                            onClick={() => handleOpenResume(applicant)}
                          >
                            <FileText className="h-4 w-4 mr-1" />
                            이력서
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setExpandedId(isExpanded ? null : applicant.applicationId)}
                          >
                            {isExpanded ? <ChevronUp className="h-4 w-4 mr-1" /> : <ChevronDown className="h-4 w-4 mr-1" />}
                            매칭 상세
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 매칭 상세 (구직자 채용공고 상세와 동일 구조) */}
                  {isExpanded && matchInfo && (
                    <div className="mt-4 pt-4 border-t space-y-4">
                      <div className="flex items-center gap-2">
                        <Sparkles className="h-5 w-5 text-primary" />
                        <span className="font-semibold">나와의 매칭률 (공고 스택 vs 이력서 스택)</span>
                      </div>
                      <div className="flex flex-wrap gap-4">
                        <div className="rounded-lg bg-muted/50 p-2 text-center min-w-[64px]">
                          <span className="font-semibold text-green-600">{matchInfo.stackMatchRate ?? 0}%</span>
                          <div className="text-muted-foreground text-xs">기술스택</div>
                        </div>
                        <div className="rounded-lg bg-muted/50 p-2 text-center min-w-[64px]">
                          <span className="font-semibold text-orange-500">{matchInfo.experienceMatchRate ?? 0}%</span>
                          <div className="text-muted-foreground text-xs">경력</div>
                        </div>
                        <div className="rounded-lg bg-muted/50 p-2 text-center min-w-[64px]">
                          <span className="font-semibold text-violet-600">{matchInfo.vectorMatchRate ?? 0}%</span>
                          <div className="text-muted-foreground text-xs">AI분석</div>
                        </div>
                      </div>
                      {matchInfo.matchLevel && (
                        <Badge variant="secondary">{matchLevelLabel[matchInfo.matchLevel] ?? matchInfo.matchLevel}</Badge>
                      )}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <p className="text-xs font-semibold text-green-600 mb-1 flex items-center gap-1">
                            <CheckCircle2 className="h-3.5 w-3.5" /> 보유 기술 ({matchInfo.matchedStacks?.length ?? 0}개)
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {(matchInfo.matchedStacks?.length ?? 0) > 0
                              ? matchInfo.matchedStacks.map((s, i) => (
                                  <Badge key={i} variant="secondary" className="bg-green-500/10 text-green-700 text-xs">
                                    {s}
                                  </Badge>
                                ))
                              : <span className="text-muted-foreground text-xs">없음</span>}
                          </div>
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-amber-600 mb-1 flex items-center gap-1">
                            <AlertCircle className="h-3.5 w-3.5" /> 부족 기술 ({matchInfo.missingStacks?.length ?? 0}개)
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {(matchInfo.missingStacks?.length ?? 0) > 0
                              ? matchInfo.missingStacks.map((s, i) => (
                                  <Badge key={i} variant="secondary" className="bg-amber-500/10 text-amber-700 text-xs">
                                    {s}
                                  </Badge>
                                ))
                              : <span className="text-muted-foreground text-xs">없음</span>}
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </CardContent>
      </Card>

      {/* 상태 변경 확인 다이얼로그 */}
      <Dialog open={actionDialog.open} onOpenChange={(open) => !open && setActionDialog({ open: false, type: null, applicant: null })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {actionDialog.type === "interview" && (
                <>
                  <CalendarPlus className="h-5 w-5 text-primary" />
                  면접 진행
                </>
              )}
              {actionDialog.type === "hire" && (
                <>
                  <UserCheck className="h-5 w-5 text-green-600" />
                  채용 확정
                </>
              )}
              {actionDialog.type === "reject" && (
                <>
                  <UserX className="h-5 w-5 text-red-600" />
                  지원 거절
                </>
              )}
            </DialogTitle>
            <DialogDescription>
              <span className="font-semibold">{actionDialog.applicant?.name}</span>님
              {actionDialog.type === "interview" && " 에게 면접을 요청하시겠습니까? 면접 일정 페이지로 이동하여 일정을 추가할 수 있습니다."}
              {actionDialog.type === "hire" && " 을 채용 확정하시겠습니까? 지원자에게 합격 알림이 발송됩니다."}
              {actionDialog.type === "reject" && " 의 지원을 거절하시겠습니까? 지원자에게 불합격 알림이 발송됩니다."}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="flex gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => setActionDialog({ open: false, type: null, applicant: null })}
            >
              취소
            </Button>
            <Button
              onClick={() => {
                const statusMap = {
                  interview: "INTERVIEW",
                  hire: "HIRED",
                  reject: "REJECTED",
                };
                handleStatusChange(statusMap[actionDialog.type!]);
              }}
              disabled={updateStatusMutation.isPending}
              className={
                actionDialog.type === "hire" ? "bg-green-600 hover:bg-green-700" :
                actionDialog.type === "reject" ? "bg-red-600 hover:bg-red-700" :
                ""
              }
            >
              {updateStatusMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              {actionDialog.type === "interview" && "면접 요청"}
              {actionDialog.type === "hire" && "채용 확정"}
              {actionDialog.type === "reject" && "거절"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 이력서 상세 모달 */}
      <Dialog
        open={resumeDialog.open}
        onOpenChange={(open) => {
          if (!open) {
            setResumeDialog({
              open: false,
              loading: false,
              error: null,
              resume: null,
            });
          }
        }}
      >
        <DialogContent className="sm:max-w-[640px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>이력서 보기</DialogTitle>
          </DialogHeader>
          {resumeDialog.loading ? (
            <div className="flex justify-center py-10">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : resumeDialog.error ? (
            <p className="text-sm text-destructive py-4">{resumeDialog.error}</p>
          ) : (
            renderResumeContent(resumeDialog.resume)
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
