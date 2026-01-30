import { useState, useEffect } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/contexts/AuthContext";
import { getMyProfileSummary } from "@/api/resumes";
import { getAiRecommendations, getRecentlyViewedJobs } from "@/api/recommendations";
import { getMyApplications } from "@/api/applications";
import { getMyScrapedJobs } from "@/api/jobs";
import { getMyPayments, cancelPayment } from "@/api/payment";
import { getMyLedgers } from "@/api/wallet";
import { CommunityManagement } from "@/components/mypage/CommunityManagement";
import {
  User,
  FileText,
  Briefcase,
  Heart,
  Bell,
  Settings,
  ChevronRight,
  Calendar,
  MapPin,
  Building2,
  Eye,
  CheckCircle2,
  Clock,
  XCircle,
  LogOut,
  Receipt,
  Coins,
  CreditCard,
  Loader2,
  RefreshCw
} from "lucide-react";
import { format } from "date-fns";
import { Link, useSearchParams } from "react-router-dom";
import { cn } from "@/lib/utils";

const PaymentHistoryTab = () => {
  const { userRole } = useAuth();
  const [payments, setPayments] = useState<any[]>([]);
  const [ledgers, setLedgers] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isCancelling, setIsCancelling] = useState<string | null>(null);

  const fetchHistory = async () => {
    setIsLoading(true);
    try {
      const [paymentsData, ledgersData] = await Promise.all([
        getMyPayments(userRole),
        getMyLedgers(userRole)
      ]);
      setPayments(paymentsData.content || []);
      setLedgers(ledgersData.content || []);
    } catch (error) {
      console.error("Failed to fetch history:", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  const handleRefund = async (paymentKey: string) => {
    if (!window.confirm("정말로 환불하시겠습니까? 환불 시 크레딧이 회수됩니다.")) return;

    setIsCancelling(paymentKey);
    try {
      await cancelPayment(paymentKey, "사용자 요청 환불");
      alert("환불 요청이 완료되었습니다.");
      fetchHistory(); // Refresh
    } catch (error: any) {
      console.error("Refund failed:", error);
      alert(`환불 실패: ${error.message || "알 수 없는 오류가 발생했습니다."}`);
    } finally {
      setIsCancelling(null);
    }
  };

  // Merge payments and ledgers for a unified chronological view
  const unifiedHistory = [
    ...payments.map(p => ({
      id: `pay-${p.paymentId}`,
      type: 'charge',
      amount: p.totalAmount,
      title: p.orderName,
      date: p.approvedAt ? new Date(p.approvedAt).toLocaleString() : '진행 중',
      isPlus: true,
      payMethod: p.method,
      status: p.status === 'DONE' ? '승인 완료' : p.status === 'CANCELED' ? '환불 완료' : '진행 중',
      paymentKey: p.paymentKey,
      canRefund: p.status === 'DONE' // Simple logic, backend should validate 7 days rule etc.
    })),
    ...ledgers.map(l => ({
      id: `ledger-${l.ledgerId}`,
      type: l.type === 'CREDIT' ? 'charge' : 'use',
      amount: l.amount,
      title: l.memo,
      date: new Date(l.occurredAt).toLocaleString(),
      isPlus: l.type === 'CREDIT',
      payMethod: '크레딧',
      status: l.type === 'CREDIT' ? '충전 완료' : '사용 완료',
      canRefund: false,
      paymentKey: undefined
    }))
  ].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg">결제 및 크레딧 내역</CardTitle>
        <Button variant="ghost" size="sm" onClick={fetchHistory} disabled={isLoading}>
          <RefreshCw className={cn("h-4 w-4 mr-2", isLoading && "animate-spin")} />
          새로고침
        </Button>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          {payments.length === 0 && ledgers.length === 0 && !isLoading ? (
            <div className="text-center py-8 text-muted-foreground">
              내역이 없습니다.
            </div>
          ) : (
            unifiedHistory.map((item) => (
              <div key={item.id} className="p-4 rounded-lg border hover:bg-muted/50 transition-colors">
                <div className="flex items-center justify-between">
                  <div className="flex items-start gap-4">
                    <div className={cn(
                      "mt-1 w-10 h-10 rounded-full flex items-center justify-center shrink-0",
                      item.isPlus ? "bg-emerald-100 text-emerald-600" : "bg-slate-100 text-slate-600"
                    )}>
                      {item.isPlus ? <Coins className="h-5 w-5" /> : <Receipt className="h-5 w-5" />}
                    </div>
                    <div>
                      <p className="font-bold text-base mb-1">{item.title}</p>
                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <span>{item.date}</span>
                        <span className="w-0.5 h-3 bg-slate-200"></span>
                        <span>{item.payMethod}</span>
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={cn(
                      "text-lg font-bold",
                      item.isPlus ? "text-emerald-600" : "text-slate-900"
                    )}>
                      {item.isPlus ? '+' : '-'}{Number(item.amount).toLocaleString()}
                    </p>
                    <div className="flex items-center justify-end gap-2 mt-1">
                      <p className="text-xs text-muted-foreground font-medium">
                        {item.status}
                      </p>
                      {item.canRefund && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-6 text-xs text-red-500 hover:text-red-600 hover:bg-red-50 border-red-200"
                          onClick={() => handleRefund(item.paymentKey)}
                          disabled={isCancelling === item.paymentKey}
                        >
                          {isCancelling === item.paymentKey ? "처리 중..." : "환불"}
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </CardContent>
    </Card>
  );
};

const getStatusIcon = (status: string) => {
  switch (status) {
    case "PENDING":
    case "서류심사중":
      return <Clock className="h-4 w-4 text-yellow-500" />;
    case "INTERVIEW":
    case "면접예정":
      return <Calendar className="h-4 w-4 text-blue-500" />;
    case "ACCEPTED":
    case "합격":
      return <CheckCircle2 className="h-4 w-4 text-green-500" />;
    case "REJECTED":
    case "불합격":
      return <XCircle className="h-4 w-4 text-red-500" />;
    default:
      return <Clock className="h-4 w-4" />;
  }
};

const getStatusBadgeVariant = (status: string) => {
  switch (status) {
    case "PENDING":
    case "서류심사중":
      return "secondary";
    case "INTERVIEW":
    case "면접예정":
      return "default";
    case "ACCEPTED":
    case "합격":
      return "default"; // green style if available
    case "REJECTED":
    case "불합격":
      return "destructive";
    default:
      return "secondary";
  }
};

const JobSeekerMyPage = () => {
  const { user, profile, signOut } = useAuth();
  const [profileSummary, setProfileSummary] = useState<any>(null);
  const [aiRecommendations, setAiRecommendations] = useState<any[]>([]);
  const [viewedJobs, setViewedJobs] = useState<any[]>([]);
  const [applications, setApplications] = useState<any[]>([]);
  const [savedJobs, setSavedJobs] = useState<any[]>([]);
  const [searchParams, setSearchParams] = useSearchParams();
  const initialTab = searchParams.get('tab') || 'applications';

  // Use state only if you need to control it internally too, or just derived from URL?
  // Radix UI Tabs `defaultValue` only sets it on mount. To control it dynamically, use `value` and `onValueChange`.
  const [activeTab, setActiveTab] = useState(initialTab);

  const [loading, setLoading] = useState(true);
  const [activeView, setActiveView] = useState<'profile' | 'community'>('profile');

  // Sync state with URL when it changes
  useEffect(() => {
    const tab = searchParams.get('tab');
    if (tab) setActiveTab(tab);
  }, [searchParams]);

  // Update URL when tab changes
  const handleTabChange = (val: string) => {
    setActiveTab(val);
    setSearchParams(prev => {
      prev.set('tab', val);
      return prev;
    });
  };


  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [profileData, recommendations, recentlyViewed, myApps, myScraps] = await Promise.all([
          getMyProfileSummary(),
          getAiRecommendations({ limit: 6 }),
          getRecentlyViewedJobs(6),
          getMyApplications(),
          getMyScrapedJobs()
        ]);

        setProfileSummary(profileData);
        setAiRecommendations(recommendations?.content || recommendations || []);
        setViewedJobs(recentlyViewed?.content || recentlyViewed || []);
        setApplications(myApps || []);
        setSavedJobs(myScraps || []);
      } catch (error) {
        console.error("Failed to fetch MyPage data:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (activeView === 'community') {
    return (
      <div className="min-h-screen bg-[#F8F9FA]">
        <Sidebar />
        <div className="lg:pl-64 flex flex-col min-h-screen">
          <Header />
          <main className="flex-1 p-6 lg:p-10">
            <div className="max-w-6xl mx-auto space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <div className="flex items-center gap-2 mb-6">
                <Button variant="ghost" onClick={() => setActiveView('profile')} className="p-0 h-auto hover:bg-transparent">
                  <ChevronRight className="h-6 w-6 rotate-180 mr-2" />
                </Button>
                <div>
                  <h1 className="text-2xl font-bold">커뮤니티 활동 관리</h1>
                  <span className="text-muted-foreground text-sm">내가 작성한 글과 댓글을 한눈에 확인하세요</span>
                </div>
              </div>
              <CommunityManagement onBack={() => setActiveView('profile')} />
            </div>
          </main>
          <Footer />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F8F9FA]">
      <Sidebar />

      <div className="lg:pl-64 flex flex-col min-h-screen">
        <Header />

        <main className="flex-1 p-6 lg:p-10">
          <div className="max-w-6xl mx-auto space-y-6">
            {/* 프로필 카드 */}
            <Card>
              <CardContent className="p-6">
                <div className="flex flex-col md:flex-row items-start md:items-center gap-6">
                  <Avatar className="h-24 w-24">
                    <AvatarImage src={user?.user_metadata?.avatar_url} />
                    <AvatarFallback className="text-2xl bg-primary text-primary-foreground">
                      {profile?.display_name?.charAt(0) || user?.email?.charAt(0)?.toUpperCase()}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex-1">
                    <h1 className="text-2xl font-bold">
                      {profile?.display_name || "사용자"}님, 안녕하세요!
                    </h1>
                    <p className="text-muted-foreground mt-1">{user?.email}</p>
                    <div className="flex flex-wrap gap-2 mt-3">
                      {/* 뱃지는 하드코딩된 예시, 실제 데이터가 있으면 교체 가능 */}
                      <Badge variant="outline">프론트엔드 개발자</Badge>
                      <Badge variant="outline">서울</Badge>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => setActiveView('community')}>
                      <Settings className="h-4 w-4 mr-2" />
                      커뮤니티 관리
                    </Button>
                    <Button size="sm" asChild>
                      <Link to="/resumes">
                        <FileText className="h-4 w-4 mr-2" />
                        이력서 관리
                      </Link>
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 통계 카드 */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <Card>
                <CardContent className="p-4 text-center">
                  <Briefcase className="h-8 w-8 mx-auto text-primary mb-2" />
                  <p className="text-2xl font-bold">{profileSummary?.applicationCount || 0}</p>
                  <p className="text-sm text-muted-foreground">지원한 공고</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <Eye className="h-8 w-8 mx-auto text-blue-500 mb-2" />
                  <p className="text-2xl font-bold">{profileSummary?.proposalCount || 0}</p> {/* proposalCount를 조회수로 대체하거나 실제 조회수 API 필요 */}
                  <p className="text-sm text-muted-foreground">받은 제안</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <Heart className="h-8 w-8 mx-auto text-red-500 mb-2" />
                  <p className="text-2xl font-bold">{profileSummary?.savedJobCount || 0}</p>
                  <p className="text-sm text-muted-foreground">관심 공고</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <Bell className="h-8 w-8 mx-auto text-yellow-500 mb-2" />
                  <p className="text-2xl font-bold">{profileSummary?.communityCount || 0}</p>
                  <p className="text-sm text-muted-foreground">커뮤니티 활동</p>
                </CardContent>
              </Card>
            </div>

            {/* 탭 콘텐츠 */}
            <Tabs value={activeTab} onValueChange={handleTabChange} className="w-full">
              <TabsList className="grid w-full grid-cols-4">
                <TabsTrigger value="applications">지원 현황</TabsTrigger>
                <TabsTrigger value="saved">관심 공고</TabsTrigger>
                <TabsTrigger value="recommendations">AI 추천</TabsTrigger>
                <TabsTrigger value="history">결제/크레딧</TabsTrigger>
              </TabsList>

              <TabsContent value="applications" className="mt-4">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">지원 현황</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {applications.length > 0 ? (
                        applications.map((app) => (
                          <div
                            key={app.applicationId || app.id}
                            className="flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50 transition-colors cursor-pointer"
                          >
                            <div className="flex items-center gap-4">
                              {getStatusIcon(app.status)}
                              <div>
                                <p className="font-medium">{app.job?.title || "채용공고"}</p>
                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                  <Building2 className="h-3 w-3" />
                                  <span>{app.job?.companyName || "회사명"}</span>
                                  <span>•</span>
                                  <span>지원일: {app.appliedAt ? format(new Date(app.appliedAt), 'yyyy-MM-dd') : "-"}</span>
                                </div>
                              </div>
                            </div>
                            <div className="flex items-center gap-3">
                              <Badge variant={getStatusBadgeVariant(app.status) as "default" | "secondary" | "destructive"}>
                                {app.status}
                              </Badge>
                              <ChevronRight className="h-4 w-4 text-muted-foreground" />
                            </div>
                          </div>
                        ))
                      ) : (
                        <div className="text-center py-8 text-muted-foreground">
                          지원 내역이 없습니다.
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="saved" className="mt-4">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">관심 공고</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {savedJobs.length > 0 ? (
                        savedJobs.map((item) => {
                          const job = item.job || item; // 구조에 따라 다를 수 있음
                          return (
                            <div
                              key={job.id}
                              className="flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50 transition-colors cursor-pointer"
                            >
                              <div>
                                <p className="font-medium">{job.title}</p>
                                <div className="flex items-center gap-2 text-sm text-muted-foreground mt-1">
                                  <Building2 className="h-3 w-3" />
                                  <span>{job.companyName}</span>
                                  <span>•</span>
                                  <MapPin className="h-3 w-3" />
                                  <span>{job.location}</span>
                                </div>
                                <p className="text-sm text-primary mt-1">{job.salary || "회사 내규에 따름"}</p>
                              </div>
                              <div className="text-right">
                                <Badge variant="outline">마감: {job.deadline ? format(new Date(job.deadline), 'yyyy-MM-dd') : "상시"}</Badge>
                                <Button size="sm" className="ml-3 btn-gradient-primary border-0" asChild>
                                  <Link to={`/jobs/${job.id}`}>
                                    지원하기
                                  </Link>
                                </Button>
                              </div>
                            </div>
                          );
                        })
                      ) : (
                        <div className="text-center py-8 text-muted-foreground">
                          관심 등록한 공고가 없습니다.
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="recommendations" className="mt-4">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">AI 맞춤 추천 공고</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {aiRecommendations.length > 0 ? (
                      <div className="space-y-4">
                        {aiRecommendations.map((job) => (
                          <div
                            key={job.id}
                            className="flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50 transition-colors cursor-pointer"
                          >
                            <div>
                              <p className="font-medium">{job.title}</p>
                              <div className="flex items-center gap-2 text-sm text-muted-foreground mt-1">
                                <Building2 className="h-3 w-3" />
                                <span>{job.companyName}</span>
                                <span>•</span>
                                <MapPin className="h-3 w-3" />
                                <span>{job.location}</span>
                              </div>
                            </div>
                            <Button size="sm" asChild>
                              <Link to={`/jobs/${job.id}`}>확인하기</Link>
                            </Button>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="flex flex-col items-center justify-center py-12 text-center">
                        <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center mb-4">
                          <User className="h-8 w-8 text-primary" />
                        </div>
                        <h3 className="text-lg font-medium mb-2">이력서를 완성해주세요</h3>
                        <p className="text-muted-foreground mb-4">
                          이력서 정보를 바탕으로 AI가 맞춤 공고를 추천해드립니다.
                        </p>
                        <Button asChild className="btn-gradient-primary border-0">
                          <Link to="/resumes/new">
                            <FileText className="h-4 w-4 mr-2" />
                            이력서 작성하기
                          </Link>
                        </Button>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="history" className="mt-4">
                <PaymentHistoryTab />
              </TabsContent>
            </Tabs>
          </div>
        </main>

        <Footer />
      </div>
    </div>
  );
};

export default JobSeekerMyPage;
