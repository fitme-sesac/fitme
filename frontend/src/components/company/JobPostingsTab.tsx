import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Briefcase,
  Eye,
  Users,
  MoreVertical,
  Edit,
  Trash2,
  Play,
  Clock,
  Loader2,
  Mail,
} from "lucide-react";
import { getJobPostings, deleteJobPosting } from "@/features/job/api/jobApi";
import { getApplicants } from "@/api/employers";

const statusConfig: Record<string, { label: string; className: string }> = {
  OPEN: { label: "진행중", className: "bg-success/10 text-success border-success/20" },
  DRAFT: { label: "임시저장", className: "bg-warning/10 text-warning border-warning/20" },
  CLOSED: { label: "마감", className: "bg-muted text-muted-foreground border-border" },
};

function experienceLabel(years: number | null | undefined): string {
  if (years == null || years === 0) return "신입";
  if (years >= 10) return "10년 이상";
  if (years >= 7) return "7년 이상";
  if (years >= 5) return "5년 이상";
  if (years >= 3) return "3년 이상";
  if (years >= 1) return "1년 이상";
  return "신입";
}

interface JobPostingsTabProps {
  /** 목록 새로고침 트리거 (변경 시 refetch) */
  refetchKey?: number;
  /** 수정하기 클릭 시 호출 (모달 열기용) */
  onEditJob?: (jobUid: string) => void;
}

export function JobPostingsTab({ refetchKey = 0, onEditJob }: JobPostingsTabProps) {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<{
    jobId: number;
    jobUid: string;
    title: string;
    status: string;
    viewCount: number;
    applicationCount: number;
    stack?: string;
    requiredExperience?: number;
    recruitmentCapacity?: number;
    salaryDisplay?: string;
    salaryText?: string;
    createdAt?: string;
    updatedAt?: string;
  }[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [applicantsModalJob, setApplicantsModalJob] = useState<{
    jobId: number;
    jobUid: string;
    title: string;
  } | null>(null);
  const [applicantsList, setApplicantsList] = useState<
    { applicationId: number; jobId: number; name?: string; email?: string; jobTitle?: string; appliedAt?: string; status?: string }[]
  >([]);
  const [applicantsLoading, setApplicantsLoading] = useState(false);

  const fetchJobs = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getJobPostings({ page: 0, size: 50 });
      setJobs(res.jobs ?? []);
      setTotalElements(res.totalElements ?? 0);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error ??
        "채용공고 목록을 불러오는데 실패했습니다.";
      setError(msg);
      setJobs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, [refetchKey]);

  // 지원자 목록: getApplicants → /api/employer/applicants (DB 연동, Mock 아님)
  useEffect(() => {
    if (!applicantsModalJob) {
      setApplicantsList([]);
      return;
    }
    let cancelled = false;
    setApplicantsLoading(true);
    getApplicants("")
      .then((res: { applicants?: { applicationId: number; jobId: number; name?: string; email?: string; jobTitle?: string; appliedAt?: string; status?: string }[] }) => {
        if (cancelled) return;
        const list = (res.applicants ?? []).filter(
          (a: { jobId: number }) => Number(a.jobId) === Number(applicantsModalJob.jobId)
        );
        setApplicantsList(list);
      })
      .catch(() => {
        if (!cancelled) setApplicantsList([]);
      })
      .finally(() => {
        if (!cancelled) setApplicantsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [applicantsModalJob]);

  const handleDelete = async (jobUid: string) => {
    if (!window.confirm("이 채용공고를 삭제하시겠습니까?")) return;
    try {
      await deleteJobPosting(jobUid);
      await fetchJobs();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error ??
        "삭제에 실패했습니다.";
      alert(msg);
    }
  };

  const activeCount = jobs.filter((j) => j.status === "OPEN").length;
  const totalViews = jobs.reduce((s, j) => s + (j.viewCount ?? 0), 0);
  const totalApplicants = jobs.reduce((s, j) => s + (j.applicationCount ?? 0), 0);

  if (loading && jobs.length === 0) {
    return (
      <div className="space-y-4">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          {[1, 2, 3, 4].map((i) => (
            <Card key={i}>
              <CardContent className="p-4">
                <div className="h-10 bg-muted animate-pulse rounded" />
              </CardContent>
            </Card>
          ))}
        </div>
        <Card>
          <CardContent className="p-8 text-center text-muted-foreground">
            채용공고 목록을 불러오는 중...
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 통계 카드 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-primary/10">
                <Briefcase className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">전체 공고</p>
                <p className="text-2xl font-bold text-foreground">{totalElements}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-success/10">
                <Play className="h-5 w-5 text-success" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">진행중</p>
                <p className="text-2xl font-bold text-foreground">{activeCount}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-info/10">
                <Eye className="h-5 w-5 text-info" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">총 조회수</p>
                <p className="text-2xl font-bold text-foreground">{totalViews.toLocaleString()}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-accent/10">
                <Users className="h-5 w-5 text-accent" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">총 지원자</p>
                <p className="text-2xl font-bold text-foreground">{totalApplicants.toLocaleString()}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 채용공고 목록 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">채용공고 목록</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {error && (
            <p className="text-destructive text-sm py-2">{error}</p>
          )}
          {jobs.length === 0 && !error && (
            <p className="text-muted-foreground text-center py-8">등록된 채용공고가 없습니다.</p>
          )}
          {jobs.map((job) => {
            const status = statusConfig[job.status] ?? {
              label: job.status,
              className: "bg-muted text-muted-foreground border-border",
            };
            const techStack = job.stack
              ? job.stack.split(",").map((s) => s.trim()).filter(Boolean)
              : [];
            const detailPath = `/employer/jobs/${job.jobUid ?? job.jobId}`;
            return (
              <div
                key={job.jobUid}
                role="button"
                tabIndex={0}
                className="p-4 rounded-xl border border-border bg-card hover:shadow-card-hover transition-all cursor-pointer"
                onClick={() => navigate(detailPath)}
                onKeyDown={(e) => e.key === "Enter" && navigate(detailPath)}
              >
                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="font-semibold text-foreground">
                        {job.title}
                      </span>
                      <Badge variant="outline" className={status.className}>
                        {status.label}
                      </Badge>
                    </div>
                    {techStack.length > 0 && (
                      <div className="flex flex-wrap gap-2 mb-2">
                        {techStack.map((tech) => (
                          <Badge key={tech} variant="secondary" className="text-xs">
                            {tech}
                          </Badge>
                        ))}
                      </div>
                    )}
                    <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                      <span>{experienceLabel(job.requiredExperience)}</span>
                      {(job.salaryDisplay || job.salaryText) && (
                        <span>{job.salaryDisplay ?? job.salaryText}</span>
                      )}
                      {job.updatedAt && (
                        <span className="flex items-center gap-1">
                          <Clock className="h-3.5 w-3.5" />
                          수정: {job.updatedAt.slice(0, 10)}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-6" onClick={(e) => e.stopPropagation()}>
                    <div className="flex gap-6">
                      <div className="text-center">
                        <p className="text-2xl font-bold text-foreground">
                          {(job.viewCount ?? 0).toLocaleString()}
                        </p>
                        <p className="text-xs text-muted-foreground">조회수</p>
                      </div>
                      <button
                        type="button"
                        className="text-center hover:bg-muted/50 rounded-lg px-2 py-1 transition-colors cursor-pointer"
                        onClick={() =>
                          setApplicantsModalJob({
                            jobId: job.jobId,
                            jobUid: job.jobUid,
                            title: job.title,
                          })
                        }
                      >
                        <p className="text-2xl font-bold text-primary">
                          {job.applicationCount ?? 0}
                        </p>
                        <p className="text-xs text-muted-foreground">지원자</p>
                      </button>
                    </div>

                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreVertical className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={() => onEditJob?.(job.jobUid)}
                        >
                          <Edit className="h-4 w-4 mr-2" />
                          수정하기
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          className="text-destructive"
                          onClick={() => handleDelete(job.jobUid)}
                        >
                          <Trash2 className="h-4 w-4 mr-2" />
                          삭제하기
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </div>
              </div>
            );
          })}
        </CardContent>
      </Card>

      {/* 지원자 목록 모달 */}
      <Dialog open={!!applicantsModalJob} onOpenChange={(open) => !open && setApplicantsModalJob(null)}>
        <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-lg">
              지원자 목록 {applicantsModalJob && `· ${applicantsModalJob.title}`}
            </DialogTitle>
          </DialogHeader>
          {applicantsLoading ? (
            <div className="flex items-center justify-center py-8 text-muted-foreground">
              <Loader2 className="h-8 w-8 animate-spin mr-2" />
              불러오는 중...
            </div>
          ) : applicantsList.length === 0 ? (
            <p className="text-center py-8 text-muted-foreground">지원자가 없습니다.</p>
          ) : (
            <ul className="space-y-3">
              {applicantsList.map((a) => (
                <li
                  key={a.applicationId}
                  className="flex items-center gap-3 p-3 rounded-lg border bg-card"
                >
                  <div className="flex-1 min-w-0">
                    <p className="font-medium truncate">{a.name ?? "-"}</p>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground mt-0.5">
                      <span className="flex items-center gap-1">
                        <Mail className="h-3.5 w-3.5" />
                        {a.email ?? "-"}
                      </span>
                      {a.appliedAt && (
                        <span className="text-xs">
                          {new Date(a.appliedAt).toLocaleDateString("ko-KR")}
                        </span>
                      )}
                    </div>
                  </div>
                  <Badge variant="secondary" className="shrink-0">
                    {a.status ?? "SUBMITTED"}
                  </Badge>
                </li>
              ))}
            </ul>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
