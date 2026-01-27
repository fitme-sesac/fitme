import { useEffect, useState } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/contexts/AuthContext";
import { getMyApplications } from "@/api/applications";
import { getMyScrapedJobs } from "@/api/jobs";
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
  Loader2
} from "lucide-react";
import { APPLICATION_STATUS_LABELS } from "@/types";

// 상태 타입 매핑 (API 응답 -> UI 타입)
const getStatusType = (status: string) => {
  switch (status) {
    case "SUBMITTED":
    case "VIEWED":
      return "pending";
    case "INTERVIEW":
      return "interview";
    case "HIRED":
      return "accepted";
    case "REJECTED":
    case "CANCELED":
      return "rejected";
    default:
      return "pending";
  }
};

const getStatusLabel = (status: string) => {
  return APPLICATION_STATUS_LABELS[status as keyof typeof APPLICATION_STATUS_LABELS] || status;
};

const getStatusIcon = (statusType: string) => {
  switch (statusType) {
    case "pending":
      return <Clock className="h-4 w-4 text-yellow-500" />;
    case "interview":
      return <Calendar className="h-4 w-4 text-blue-500" />;
    case "accepted":
      return <CheckCircle2 className="h-4 w-4 text-green-500" />;
    case "rejected":
      return <XCircle className="h-4 w-4 text-red-500" />;
    default:
      return <Clock className="h-4 w-4" />;
  }
};

const getStatusBadgeVariant = (statusType: string) => {
  switch (statusType) {
    case "pending":
      return "secondary";
    case "interview":
      return "default";
    case "accepted":
      return "default";
    case "rejected":
      return "destructive";
    default:
      return "secondary";
  }
};

const JobSeekerMyPage = () => {
  const { user, profile } = useAuth();
  const [applications, setApplications] = useState<any[]>([]);
  const [savedJobs, setSavedJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    applied: 0,
    profileViews: 0,
    saved: 0,
    notifications: 0
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 지원 현황 조회
        const appsResponse = await getMyApplications();
        const appsData = Array.isArray(appsResponse) ? appsResponse : appsResponse?.content || [];
        setApplications(appsData);

        // 스크랩 공고 조회
        const scrapsResponse = await getMyScrapedJobs();
        const scrapsData = Array.isArray(scrapsResponse) ? scrapsResponse : scrapsResponse?.content || [];
        setSavedJobs(scrapsData);

        // 통계 업데이트
        setStats({
          applied: appsData.length,
          profileViews: 0, // API에서 제공하면 업데이트
          saved: scrapsData.length,
          notifications: 0 // API에서 제공하면 업데이트
        });
      } catch (error) {
        console.error("Failed to fetch data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Sidebar />

      <div className="lg:pl-64">
        <Header />

        <main className="p-6">
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
                      {profile?.display_name || user?.user_metadata?.display_name || "사용자"}님, 안녕하세요!
                    </h1>
                    <p className="text-muted-foreground mt-1">{user?.email}</p>
                    <div className="flex flex-wrap gap-2 mt-3">
                      <Badge variant="outline">구직자</Badge>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Button variant="outline" size="sm">
                      <Settings className="h-4 w-4 mr-2" />
                      설정
                    </Button>
                    <Button size="sm">
                      <FileText className="h-4 w-4 mr-2" />
                      이력서 관리
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
                  <p className="text-2xl font-bold">{stats.applied}</p>
                  <p className="text-sm text-muted-foreground">지원한 공고</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <Eye className="h-8 w-8 mx-auto text-blue-500 mb-2" />
                  <p className="text-2xl font-bold">{stats.profileViews}</p>
                  <p className="text-sm text-muted-foreground">프로필 조회</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <Heart className="h-8 w-8 mx-auto text-red-500 mb-2" />
                  <p className="text-2xl font-bold">{stats.saved}</p>
                  <p className="text-sm text-muted-foreground">관심 공고</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4 text-center">
                  <Bell className="h-8 w-8 mx-auto text-yellow-500 mb-2" />
                  <p className="text-2xl font-bold">{stats.notifications}</p>
                  <p className="text-sm text-muted-foreground">새 알림</p>
                </CardContent>
              </Card>
            </div>

            {/* 탭 콘텐츠 */}
            <Tabs defaultValue="applications" className="w-full">
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="applications">지원 현황</TabsTrigger>
                <TabsTrigger value="saved">관심 공고</TabsTrigger>
                <TabsTrigger value="recommendations">AI 추천</TabsTrigger>
              </TabsList>

              <TabsContent value="applications" className="mt-4">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">지원 현황</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {applications.length === 0 ? (
                      <div className="text-center py-8 text-muted-foreground">
                        아직 지원한 공고가 없습니다.
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {applications.map((app: any) => {
                          const statusType = getStatusType(app.status);
                          return (
                            <div
                              key={app.applicationId || app.id}
                              className="flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50 transition-colors cursor-pointer"
                            >
                              <div className="flex items-center gap-4">
                                {getStatusIcon(statusType)}
                                <div>
                                  <p className="font-medium">{app.job?.title || app.position}</p>
                                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    <Building2 className="h-3 w-3" />
                                    <span>{app.job?.employer?.name || app.company}</span>
                                    <span>•</span>
                                    <span>지원일: {new Date(app.appliedAt).toLocaleDateString()}</span>
                                  </div>
                                </div>
                              </div>
                              <div className="flex items-center gap-3">
                                <Badge variant={getStatusBadgeVariant(statusType) as "default" | "secondary" | "destructive"}>
                                  {getStatusLabel(app.status)}
                                </Badge>
                                <ChevronRight className="h-4 w-4 text-muted-foreground" />
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="saved" className="mt-4">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">관심 공고</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {savedJobs.length === 0 ? (
                      <div className="text-center py-8 text-muted-foreground">
                        스크랩한 공고가 없습니다.
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {savedJobs.map((scrap: any) => {
                          const job = scrap.job || scrap;
                          return (
                            <div
                              key={scrap.scrapId || job.jobId || job.id}
                              className="flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50 transition-colors cursor-pointer"
                            >
                              <div>
                                <p className="font-medium">{job.title || job.position}</p>
                                <div className="flex items-center gap-2 text-sm text-muted-foreground mt-1">
                                  <Building2 className="h-3 w-3" />
                                  <span>{job.employer?.name || job.company}</span>
                                  <span>•</span>
                                  <MapPin className="h-3 w-3" />
                                  <span>{job.location}</span>
                                </div>
                                <p className="text-sm text-primary mt-1">{job.salaryText || job.salary}</p>
                              </div>
                              <div className="text-right">
                                {job.deadline && (
                                  <Badge variant="outline">마감: {job.deadline}</Badge>
                                )}
                                <Button size="sm" className="ml-3">
                                  지원하기
                                </Button>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="recommendations" className="mt-4">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">AI 맞춤 추천 공고</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="flex flex-col items-center justify-center py-12 text-center">
                      <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center mb-4">
                        <User className="h-8 w-8 text-primary" />
                      </div>
                      <h3 className="text-lg font-medium mb-2">이력서를 완성해주세요</h3>
                      <p className="text-muted-foreground mb-4">
                        이력서 정보를 바탕으로 AI가 맞춤 공고를 추천해드립니다.
                      </p>
                      <Button>
                        <FileText className="h-4 w-4 mr-2" />
                        이력서 작성하기
                      </Button>
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

export default JobSeekerMyPage;
