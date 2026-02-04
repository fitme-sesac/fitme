import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  BarChart3,
  Eye,
  MousePointer,
  DollarSign,
  Target,
  Plus,
  Loader2,
  Megaphone,
  Calendar,
  Pause,
  Play,
  Square,
  MoreHorizontal,
  Edit,
  Users
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { getAdStats, createAdCampaign, getMyJobPostings, updateAdCampaignStatus, updateAdCampaign, getEmployerProfile } from "@/api/employers";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/contexts/AuthContext";

interface CampaignDTO {
  campaignId: number;
  jobId: number;
  jobTitle: string;
  status: string;
  clicks: number;
  impressions: number;
  ctr: number;
  dailyBudget: number;
  cpcBid: number;
  applicants: number;
}

interface AdStats {
  activeCampaigns: number;
  totalClicks: number;
  totalImpressions: number;
  totalSpent: number;
  ctr: number;
  campaigns: CampaignDTO[];
}

interface JobPosting {
  jobId: number;
  title: string;
  status: string;
}

const statusConfig: Record<string, { label: string; className: string }> = {
  ACTIVE: { label: "진행중", className: "bg-success/10 text-success border-success/20" },
  PAUSED: { label: "일시중지", className: "bg-warning/10 text-warning border-warning/20" },
  ENDED: { label: "종료", className: "bg-muted text-muted-foreground border-border" },
};

// 오늘 날짜 (YYYY-MM-DD 형식)
function getTodayString() {
  const now = new Date();
  return now.toISOString().split("T")[0];
}

// 30일 후 날짜
function getDefaultEndDate() {
  const now = new Date();
  now.setDate(now.getDate() + 30);
  return now.toISOString().split("T")[0];
}

export function AdAnalyticsTab() {
  const { toast } = useToast();
  const { user } = useAuth(); // [Fix] Hook은 최상위에서 호출
  const [adStats, setAdStats] = useState<AdStats | null>(null);
  const [employerId, setEmployerId] = useState<number | null>(null); // [Added] 실제 employerId 저장
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // [Added] 컴포넌트 마운트 시 실제 Employer ID 가져오기
  useEffect(() => {
    async function fetchEmployerId() {
      try {
        const profile = await getEmployerProfile();
        if (profile && profile.employerId) {
          setEmployerId(profile.employerId);
        }
      } catch (err) {
        console.error("Failed to fetch employer profile:", err);
        // 에러 시 조치 (선택사항)
      }
    }
    fetchEmployerId();
  }, []);

  // 캠페인 생성 모달 상태
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [jobPostings, setJobPostings] = useState<JobPosting[]>([]);
  const [loadingJobs, setLoadingJobs] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // 폼 상태
  const [selectedJobId, setSelectedJobId] = useState<string>("");
  const [cpcBid, setCpcBid] = useState("500");
  const [dailyBudget, setDailyBudget] = useState("50000");
  const [startDate, setStartDate] = useState(getTodayString());
  const [endDate, setEndDate] = useState(getDefaultEndDate());

  // 캠페인 수정 모달 상태
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingCampaign, setEditingCampaign] = useState<CampaignDTO | null>(null);
  const [editCpcBid, setEditCpcBid] = useState("");
  const [editDailyBudget, setEditDailyBudget] = useState("");
  const [editSubmitting, setEditSubmitting] = useState(false);

  async function fetchAdStats() {
    try {
      setLoading(true);
      const data = await getAdStats();
      setAdStats(data);
    } catch (err) {
      console.error("광고 통계 조회 실패:", err);
      setError("광고 통계를 불러오는 데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchAdStats();
  }, []);

  // 모달 열 때 채용공고 목록 로드
  async function handleOpenModal() {
    setIsModalOpen(true);
    setLoadingJobs(true);
    try {
      const data = await getMyJobPostings();
      // 활성 상태(OPEN)인 채용공고만 필터링
      const jobs = (data.content || data.jobs || [])
        .filter((job: any) => job.status === "OPEN")
        .map((job: any) => ({
          jobId: job.jobId,
          title: job.title,
          status: job.status,
        }));
      setJobPostings(jobs);

      // 이미 광고가 있는 채용공고 제외
      const existingJobIds = adStats?.campaigns.map(c => c.jobId) || [];
      const availableJobs = jobs.filter((j: JobPosting) => !existingJobIds.includes(j.jobId));

      if (availableJobs.length > 0) {
        setSelectedJobId(String(availableJobs[0].jobId));
      }
    } catch (err) {
      console.error("채용공고 목록 조회 실패:", err);
      toast({
        title: "오류",
        description: "채용공고 목록을 불러오는 데 실패했습니다.",
        variant: "destructive",
      });
    } finally {
      setLoadingJobs(false);
    }
  }

  function handleCloseModal() {
    setIsModalOpen(false);
    setSelectedJobId("");
    setCpcBid("500");
    setDailyBudget("50000");
    setStartDate(getTodayString());
    setEndDate(getDefaultEndDate());
  }

  async function handleCreateCampaign() {
    if (!selectedJobId) {
      toast({
        title: "채용공고 선택",
        description: "광고할 채용공고를 선택해주세요.",
        variant: "destructive",
      });
      return;
    }

    const cpc = parseInt(cpcBid) || 0;
    const budget = parseInt(dailyBudget) || 0;

    if (cpc < 100) {
      toast({
        title: "입찰가 오류",
        description: "CPC 입찰가는 최소 100크레딧 이상이어야 합니다.",
        variant: "destructive",
      });
      return;
    }

    if (budget < 10000) {
      toast({
        title: "예산 오류",
        description: "일일 예산은 최소 10,000크레딧 이상이어야 합니다.",
        variant: "destructive",
      });
      return;
    }

    // [Fix] employerId 유효성 검사 (user.id 아님)
    if (!employerId) {
      toast({
        title: "기업 정보 로딩 중",
        description: "기업 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.",
        variant: "destructive",
      });
      return;
    }

    setSubmitting(true);
    try {
      await createAdCampaign({
        employerId: employerId, // [Fix] 실제 employerId 사용
        jobId: parseInt(selectedJobId),
        cpcBid: cpc,
        dailyBudget: budget,
        startDate,
        endDate,
      });

      toast({
        title: "캠페인 생성 완료",
        description: "광고 캠페인이 성공적으로 생성되었습니다.",
      });

      handleCloseModal();
      fetchAdStats(); // 목록 새로고침
    } catch (err: any) {
      console.error("캠페인 생성 실패:", err);
      const message = err?.response?.data?.message || err?.message || "캠페인 생성에 실패했습니다.";
      toast({
        title: "생성 실패",
        description: message,
        variant: "destructive",
      });
    } finally {
      setSubmitting(false);
    }
  }

  // 캠페인 상태 변경
  const [updatingCampaignId, setUpdatingCampaignId] = useState<number | null>(null);

  async function handleStatusChange(campaignId: number, newStatus: string) {
    setUpdatingCampaignId(campaignId);
    try {
      await updateAdCampaignStatus(campaignId, newStatus);
      toast({
        title: "상태 변경 완료",
        description: newStatus === "ACTIVE" ? "광고가 재개되었습니다."
          : newStatus === "PAUSED" ? "광고가 일시정지되었습니다."
            : "광고가 종료되었습니다.",
      });
      fetchAdStats(); // 목록 새로고침
    } catch (err: any) {
      console.error("상태 변경 실패:", err);
      const message = err?.response?.data?.message || err?.message || "상태 변경에 실패했습니다.";
      toast({
        title: "상태 변경 실패",
        description: message,
        variant: "destructive",
      });
    } finally {
      setUpdatingCampaignId(null);
    }
  }

  // 캠페인 수정 모달 열기
  function handleOpenEditModal(campaign: CampaignDTO) {
    setEditingCampaign(campaign);
    setEditCpcBid(String(campaign.cpcBid));
    setEditDailyBudget(String(campaign.dailyBudget));
    setIsEditModalOpen(true);
  }

  function handleCloseEditModal() {
    setIsEditModalOpen(false);
    setEditingCampaign(null);
    setEditCpcBid("");
    setEditDailyBudget("");
  }

  async function handleUpdateCampaign() {
    if (!editingCampaign) return;

    const cpc = parseInt(editCpcBid) || 0;
    const budget = parseInt(editDailyBudget) || 0;

    if (cpc < 100) {
      toast({
        title: "입찰가 오류",
        description: "CPC 입찰가는 최소 100원 이상이어야 합니다.",
        variant: "destructive",
      });
      return;
    }

    if (budget < 10000) {
      toast({
        title: "예산 오류",
        description: "일일 예산은 최소 10,000원 이상이어야 합니다.",
        variant: "destructive",
      });
      return;
    }

    setEditSubmitting(true);
    try {
      await updateAdCampaign(editingCampaign.campaignId, {
        cpcBid: cpc,
        dailyBudget: budget,
      });

      toast({
        title: "수정 완료",
        description: "광고 캠페인이 성공적으로 수정되었습니다.",
      });

      handleCloseEditModal();
      fetchAdStats(); // 목록 새로고침
    } catch (err: any) {
      console.error("캠페인 수정 실패:", err);
      const message = err?.response?.data?.message || err?.message || "캠페인 수정에 실패했습니다.";
      toast({
        title: "수정 실패",
        description: message,
        variant: "destructive",
      });
    } finally {
      setEditSubmitting(false);
    }
  }

  // 이미 광고가 있는 채용공고 ID 목록
  const existingJobIds = adStats?.campaigns.map(c => c.jobId) || [];
  const availableJobs = jobPostings.filter(j => !existingJobIds.includes(j.jobId));

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error || !adStats) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        {error || "광고 통계를 불러올 수 없습니다."}
      </div>
    );
  }

  const { totalClicks, totalImpressions, totalSpent, ctr, campaigns } = adStats;
  const totalBudget = campaigns.reduce((acc, c) => acc + c.dailyBudget * 30, 0); // 월간 예산 추정
  const avgCtr = totalImpressions > 0 ? ctr.toFixed(1) : "0.0";

  return (
    <div className="space-y-6">
      {/* 전체 통계 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">총 소진 크레딧</span>
              <DollarSign className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-foreground">
              {totalSpent >= 10000
                ? `${(totalSpent / 10000).toFixed(1)}만 크레딧`
                : `${totalSpent.toLocaleString()} 크레딧`}
            </p>
            {totalBudget > 0 && (
              <>
                <Progress value={(totalSpent / totalBudget) * 100} className="h-1.5 mt-2" />
                <p className="text-xs text-muted-foreground mt-1">
                  월간 예산 대비 {((totalSpent / totalBudget) * 100).toFixed(0)}% 사용
                </p>
              </>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">노출수</span>
              <Eye className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-foreground">
              {totalImpressions.toLocaleString()}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">클릭수</span>
              <MousePointer className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-foreground">
              {totalClicks.toLocaleString()}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">클릭률 (CTR)</span>
              <Target className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-foreground">{avgCtr}%</p>
          </CardContent>
        </Card>
      </div>

      {/* 캠페인 목록 */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-primary" />
            광고 캠페인
          </CardTitle>
          <Button size="sm" className="btn-gradient-primary" onClick={handleOpenModal}>
            <Plus className="h-4 w-4 mr-1" />
            새 캠페인
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {campaigns.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              등록된 광고 캠페인이 없습니다.
            </div>
          ) : (
            campaigns.map((campaign) => {
              const statusInfo = statusConfig[campaign.status] || statusConfig.ENDED;
              const spent = campaign.clicks * campaign.cpcBid;
              const monthlyBudget = campaign.dailyBudget * 30;
              const campaignCtr = campaign.impressions > 0
                ? ((campaign.clicks / campaign.impressions) * 100).toFixed(1)
                : "0.0";
              const isUpdating = updatingCampaignId === campaign.campaignId;

              return (
                <div
                  key={campaign.campaignId}
                  className="p-4 rounded-xl border border-border bg-card hover:shadow-card-hover transition-all"
                >
                  <div className="flex flex-col lg:flex-row gap-4">
                    {/* 캠페인 정보 */}
                    <div className="flex-1">
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <h3 className="font-semibold text-foreground">{campaign.jobTitle}</h3>
                          <Badge variant="outline" className={statusInfo.className}>
                            {statusInfo.label}
                          </Badge>
                        </div>
                        {/* 상태 변경 드롭다운 */}
                        {campaign.status !== "ENDED" && (
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="sm" className="h-8 w-8 p-0" disabled={isUpdating}>
                                {isUpdating ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <MoreHorizontal className="h-4 w-4" />
                                )}
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem onClick={() => handleOpenEditModal(campaign)}>
                                <Edit className="mr-2 h-4 w-4" />
                                광고 수정
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              {campaign.status === "ACTIVE" ? (
                                <DropdownMenuItem onClick={() => handleStatusChange(campaign.campaignId, "PAUSED")}>
                                  <Pause className="mr-2 h-4 w-4" />
                                  일시정지
                                </DropdownMenuItem>
                              ) : campaign.status === "PAUSED" ? (
                                <DropdownMenuItem onClick={() => handleStatusChange(campaign.campaignId, "ACTIVE")}>
                                  <Play className="mr-2 h-4 w-4" />
                                  재개하기
                                </DropdownMenuItem>
                              ) : null}
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                onClick={() => handleStatusChange(campaign.campaignId, "ENDED")}
                                className="text-destructive focus:text-destructive"
                              >
                                <Square className="mr-2 h-4 w-4" />
                                광고 종료
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        )}
                      </div>
                      <div className="flex flex-wrap gap-3 text-sm text-muted-foreground mb-3">
                        <span>CPC: {campaign.cpcBid.toLocaleString()} 크레딧</span>
                        <span>일일 예산: {campaign.dailyBudget.toLocaleString()} 크레딧</span>
                      </div>
                      {/* 예산 진행률 */}
                      <div className="space-y-1">
                        <div className="flex justify-between text-xs">
                          <span className="text-muted-foreground">예상 월간 예산 대비 소진</span>
                          <span className="font-medium text-foreground">
                            {(spent / 10000).toFixed(1)}만 크레딧 / {(monthlyBudget / 10000).toFixed(0)}만 크레딧
                          </span>
                        </div>
                        <Progress value={monthlyBudget > 0 ? (spent / monthlyBudget) * 100 : 0} className="h-2" />
                      </div>
                    </div>

                    {/* 성과 지표 */}
                    <div className="grid grid-cols-4 gap-3 lg:w-96">
                      <div className="text-center p-2 rounded-lg bg-muted/50">
                        <p className="text-lg font-bold text-foreground">{campaign.impressions.toLocaleString()}</p>
                        <p className="text-xs text-muted-foreground">노출</p>
                      </div>
                      <div className="text-center p-2 rounded-lg bg-muted/50">
                        <p className="text-lg font-bold text-foreground">{campaign.clicks.toLocaleString()}</p>
                        <p className="text-xs text-muted-foreground">클릭</p>
                      </div>
                      <div className="text-center p-2 rounded-lg bg-primary/5 border border-primary/20">
                        <p className="text-lg font-bold text-primary">{campaignCtr}%</p>
                        <p className="text-xs text-muted-foreground">CTR</p>
                      </div>
                      <div className="text-center p-2 rounded-lg bg-success/5 border border-success/20">
                        <p className="text-lg font-bold text-success">{campaign.applicants?.toLocaleString() || 0}</p>
                        <p className="text-xs text-muted-foreground">지원자</p>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </CardContent>
      </Card>

      {/* 새 캠페인 생성 모달 */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Megaphone className="h-5 w-5 text-primary" />
              새 광고 캠페인 만들기
            </DialogTitle>
            <DialogDescription>
              채용공고를 선택하고 광고 예산을 설정하세요.
            </DialogDescription>
          </DialogHeader>

          <div className="py-4 space-y-5">
            {/* 채용공고 선택 */}
            <div className="space-y-2">
              <Label htmlFor="job-select">광고할 채용공고</Label>
              {loadingJobs ? (
                <div className="flex items-center justify-center py-4">
                  <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                </div>
              ) : availableJobs.length === 0 ? (
                <div className="text-center py-4 text-sm text-muted-foreground border rounded-lg bg-muted/30">
                  {jobPostings.length === 0
                    ? "활성 상태인 채용공고가 없습니다."
                    : "모든 채용공고에 이미 광고가 등록되어 있습니다."}
                </div>
              ) : (
                <Select value={selectedJobId} onValueChange={setSelectedJobId}>
                  <SelectTrigger>
                    <SelectValue placeholder="채용공고를 선택하세요" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableJobs.map((job) => (
                      <SelectItem key={job.jobId} value={String(job.jobId)}>
                        {job.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>

            {/* CPC 입찰가 */}
            <div className="space-y-2">
              <Label htmlFor="cpc-bid">CPC 입찰가 (클릭당 비용)</Label>
              <div className="relative">
                <Input
                  id="cpc-bid"
                  type="number"
                  min="100"
                  step="50"
                  value={cpcBid}
                  onChange={(e) => setCpcBid(e.target.value)}
                  className="pr-8"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                  크레딧
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                입찰가가 높을수록 광고 노출 우선순위가 높아집니다. (최소 100크레딧)
              </p>
            </div>

            {/* 일일 예산 */}
            <div className="space-y-2">
              <Label htmlFor="daily-budget">일일 예산</Label>
              <div className="relative">
                <Input
                  id="daily-budget"
                  type="number"
                  min="10000"
                  step="5000"
                  value={dailyBudget}
                  onChange={(e) => setDailyBudget(e.target.value)}
                  className="pr-8"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                  크레딧
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                하루 최대 광고비 한도입니다. (최소 10,000 크레딧)
              </p>
            </div>

            {/* 캠페인 기간 */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="start-date" className="flex items-center gap-1">
                  <Calendar className="h-3.5 w-3.5" />
                  시작일
                </Label>
                <Input
                  id="start-date"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  min={getTodayString()}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="end-date" className="flex items-center gap-1">
                  <Calendar className="h-3.5 w-3.5" />
                  종료일
                </Label>
                <Input
                  id="end-date"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  min={startDate}
                />
              </div>
            </div>

            {/* 예상 비용 안내 */}
            <div className="p-3 rounded-lg bg-primary/5 border border-primary/20">
              <p className="text-sm font-medium text-foreground mb-1">예상 비용 (선택 기간)</p>
              <p className="text-xl font-bold text-primary">
                {(() => {
                  const start = new Date(startDate);
                  const end = new Date(endDate);
                  const days = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
                  const estimatedCost = (parseInt(dailyBudget) || 0) * (days > 0 ? days : 0);

                  return estimatedCost >= 10000
                    ? `${(estimatedCost / 10000).toFixed(1)}만 크레딧`
                    : `${estimatedCost.toLocaleString()} 크레딧`;
                })()}
              </p>
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center mt-2 text-xs text-muted-foreground gap-1">
                <span>
                  {(() => {
                    const start = new Date(startDate);
                    const end = new Date(endDate);
                    const days = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
                    return `기간: ${days > 0 ? days : 0}일`;
                  })()}
                </span>
                <span className="font-medium text-primary/80">
                  {(() => {
                    const start = new Date(startDate);
                    const end = new Date(endDate);
                    const days = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
                    const estimatedCost = (parseInt(dailyBudget) || 0) * (days > 0 ? days : 0);
                    const cpc = parseInt(cpcBid) || 0;
                    const maxClicks = cpc > 0 ? Math.floor(estimatedCost / cpc) : 0;
                    return `최대 약 ${maxClicks.toLocaleString()}회 클릭 예상`;
                  })()}
                </span>
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={handleCloseModal} disabled={submitting}>
              취소
            </Button>
            <Button
              onClick={handleCreateCampaign}
              disabled={submitting || availableJobs.length === 0 || !selectedJobId}
              className="btn-gradient-primary"
            >
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  생성 중...
                </>
              ) : (
                <>
                  <Megaphone className="mr-2 h-4 w-4" />
                  캠페인 시작
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 캠페인 수정 모달 */}
      <Dialog open={isEditModalOpen} onOpenChange={setIsEditModalOpen}>
        <DialogContent className="sm:max-w-[450px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Edit className="h-5 w-5 text-primary" />
              광고 캠페인 수정
            </DialogTitle>
            <DialogDescription>
              {editingCampaign?.jobTitle}의 광고 설정을 수정합니다.
            </DialogDescription>
          </DialogHeader>

          <div className="py-4 space-y-5">
            {/* CPC 입찰가 */}
            <div className="space-y-2">
              <Label htmlFor="edit-cpc-bid">CPC 입찰가 (클릭당 비용)</Label>
              <div className="relative">
                <Input
                  id="edit-cpc-bid"
                  type="number"
                  min="100"
                  step="50"
                  value={editCpcBid}
                  onChange={(e) => setEditCpcBid(e.target.value)}
                  className="pr-8"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                  원
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                입찰가가 높을수록 광고 노출 우선순위가 높아집니다. (최소 100원)
              </p>
            </div>

            {/* 일일 예산 */}
            <div className="space-y-2">
              <Label htmlFor="edit-daily-budget">일일 예산</Label>
              <div className="relative">
                <Input
                  id="edit-daily-budget"
                  type="number"
                  min="10000"
                  step="5000"
                  value={editDailyBudget}
                  onChange={(e) => setEditDailyBudget(e.target.value)}
                  className="pr-8"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                  원
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                하루 최대 광고비 한도입니다. (최소 10,000원)
              </p>
            </div>

            {/* 예상 비용 안내 */}
            <div className="p-3 rounded-lg bg-primary/5 border border-primary/20">
              <p className="text-sm font-medium text-foreground mb-1">예상 월간 최대 비용</p>
              <p className="text-xl font-bold text-primary">
                {((parseInt(editDailyBudget) || 0) * 30 / 10000).toFixed(1)}만원
              </p>
              <p className="text-xs text-muted-foreground mt-1">
                실제 비용은 클릭 발생 시에만 차감됩니다.
              </p>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={handleCloseEditModal} disabled={editSubmitting}>
              취소
            </Button>
            <Button
              onClick={handleUpdateCampaign}
              disabled={editSubmitting}
              className="btn-gradient-primary"
            >
              {editSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  수정 중...
                </>
              ) : (
                <>
                  <Edit className="mr-2 h-4 w-4" />
                  수정 완료
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
