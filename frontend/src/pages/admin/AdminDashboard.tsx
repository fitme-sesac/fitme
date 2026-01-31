import { useState, useEffect } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/contexts/AuthContext";
import { Navigate } from "react-router-dom";
import {
    Users,
    Briefcase,
    Building2,
    Shield,
    TrendingUp,
    AlertCircle,
    CheckCircle2,
    Clock,
    Loader2
} from "lucide-react";

/**
 * 관리자 대시보드
 * - 회원 관리
 * - 채용공고 관리
 * - 기업 관리
 * - 시스템 현황
 */
export default function AdminDashboard() {
    const { user, loading, isAdmin } = useAuth();
    const [activeTab, setActiveTab] = useState("overview");

    // 임시 통계 데이터
    const [stats, setStats] = useState({
        totalMembers: 0,
        totalJobs: 0,
        totalCompanies: 0,
        pendingApprovals: 0,
        todayApplications: 0,
        activeAds: 0
    });

    useEffect(() => {
        // TODO: 실제 API 연동
        setStats({
            totalMembers: 1234,
            totalJobs: 567,
            totalCompanies: 89,
            pendingApprovals: 12,
            todayApplications: 45,
            activeAds: 23
        });
    }, []);

    if (loading) {
        return (
            <div className="min-h-screen bg-background flex items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    // 관리자가 아니면 접근 불가
    if (!user || !isAdmin) {
        return <Navigate to="/" replace />;
    }

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64">
                <Header />
                <main className="p-6">
                    <div className="max-w-7xl mx-auto space-y-6">
                        {/* 페이지 헤더 */}
                        <div className="flex items-center justify-between">
                            <div>
                                <h1 className="text-2xl font-bold flex items-center gap-2">
                                    <Shield className="h-6 w-6 text-primary" />
                                    관리자 대시보드
                                </h1>
                                <p className="text-muted-foreground mt-1">
                                    FitMe 서비스 관리 및 모니터링
                                </p>
                            </div>
                            <Badge variant="outline" className="text-sm">
                                {user?.user_metadata?.display_name || "관리자"}
                            </Badge>
                        </div>

                        {/* 통계 카드 */}
                        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
                            <Card>
                                <CardContent className="p-4">
                                    <div className="flex items-center gap-3">
                                        <div className="h-10 w-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
                                            <Users className="h-5 w-5 text-blue-500" />
                                        </div>
                                        <div>
                                            <p className="text-2xl font-bold">{stats.totalMembers.toLocaleString()}</p>
                                            <p className="text-xs text-muted-foreground">전체 회원</p>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                            <Card>
                                <CardContent className="p-4">
                                    <div className="flex items-center gap-3">
                                        <div className="h-10 w-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                                            <Briefcase className="h-5 w-5 text-green-500" />
                                        </div>
                                        <div>
                                            <p className="text-2xl font-bold">{stats.totalJobs.toLocaleString()}</p>
                                            <p className="text-xs text-muted-foreground">채용공고</p>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                            <Card>
                                <CardContent className="p-4">
                                    <div className="flex items-center gap-3">
                                        <div className="h-10 w-10 rounded-lg bg-purple-500/10 flex items-center justify-center">
                                            <Building2 className="h-5 w-5 text-purple-500" />
                                        </div>
                                        <div>
                                            <p className="text-2xl font-bold">{stats.totalCompanies.toLocaleString()}</p>
                                            <p className="text-xs text-muted-foreground">기업</p>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                            <Card>
                                <CardContent className="p-4">
                                    <div className="flex items-center gap-3">
                                        <div className="h-10 w-10 rounded-lg bg-yellow-500/10 flex items-center justify-center">
                                            <Clock className="h-5 w-5 text-yellow-500" />
                                        </div>
                                        <div>
                                            <p className="text-2xl font-bold">{stats.pendingApprovals}</p>
                                            <p className="text-xs text-muted-foreground">승인 대기</p>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                            <Card>
                                <CardContent className="p-4">
                                    <div className="flex items-center gap-3">
                                        <div className="h-10 w-10 rounded-lg bg-emerald-500/10 flex items-center justify-center">
                                            <TrendingUp className="h-5 w-5 text-emerald-500" />
                                        </div>
                                        <div>
                                            <p className="text-2xl font-bold">{stats.todayApplications}</p>
                                            <p className="text-xs text-muted-foreground">오늘 지원</p>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                            <Card>
                                <CardContent className="p-4">
                                    <div className="flex items-center gap-3">
                                        <div className="h-10 w-10 rounded-lg bg-orange-500/10 flex items-center justify-center">
                                            <CheckCircle2 className="h-5 w-5 text-orange-500" />
                                        </div>
                                        <div>
                                            <p className="text-2xl font-bold">{stats.activeAds}</p>
                                            <p className="text-xs text-muted-foreground">진행중 광고</p>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        </div>

                        {/* 탭 콘텐츠 */}
                        <Tabs value={activeTab} onValueChange={setActiveTab}>
                            <TabsList className="grid w-full grid-cols-4 lg:w-auto lg:inline-flex">
                                <TabsTrigger value="overview">개요</TabsTrigger>
                                <TabsTrigger value="members">회원 관리</TabsTrigger>
                                <TabsTrigger value="jobs">공고 관리</TabsTrigger>
                                <TabsTrigger value="companies">기업 관리</TabsTrigger>
                            </TabsList>

                            <TabsContent value="overview" className="mt-6">
                                <div className="grid gap-6 md:grid-cols-2">
                                    {/* 최근 활동 */}
                                    <Card>
                                        <CardHeader>
                                            <CardTitle className="text-lg">최근 활동</CardTitle>
                                        </CardHeader>
                                        <CardContent>
                                            <div className="space-y-4">
                                                <div className="flex items-center gap-3 p-3 rounded-lg bg-muted/50">
                                                    <CheckCircle2 className="h-5 w-5 text-green-500" />
                                                    <div className="flex-1">
                                                        <p className="text-sm font-medium">새 기업 가입 승인</p>
                                                        <p className="text-xs text-muted-foreground">테크스타트업 (주)</p>
                                                    </div>
                                                    <span className="text-xs text-muted-foreground">5분 전</span>
                                                </div>
                                                <div className="flex items-center gap-3 p-3 rounded-lg bg-muted/50">
                                                    <Briefcase className="h-5 w-5 text-blue-500" />
                                                    <div className="flex-1">
                                                        <p className="text-sm font-medium">새 채용공고 등록</p>
                                                        <p className="text-xs text-muted-foreground">프론트엔드 개발자</p>
                                                    </div>
                                                    <span className="text-xs text-muted-foreground">15분 전</span>
                                                </div>
                                                <div className="flex items-center gap-3 p-3 rounded-lg bg-muted/50">
                                                    <AlertCircle className="h-5 w-5 text-yellow-500" />
                                                    <div className="flex-1">
                                                        <p className="text-sm font-medium">신고 접수</p>
                                                        <p className="text-xs text-muted-foreground">부적절한 채용공고</p>
                                                    </div>
                                                    <span className="text-xs text-muted-foreground">1시간 전</span>
                                                </div>
                                            </div>
                                        </CardContent>
                                    </Card>

                                    {/* 빠른 작업 */}
                                    <Card>
                                        <CardHeader>
                                            <CardTitle className="text-lg">빠른 작업</CardTitle>
                                        </CardHeader>
                                        <CardContent>
                                            <div className="grid grid-cols-2 gap-3">
                                                <Button variant="outline" className="h-auto py-4 flex-col gap-2">
                                                    <Users className="h-5 w-5" />
                                                    <span className="text-sm">회원 조회</span>
                                                </Button>
                                                <Button variant="outline" className="h-auto py-4 flex-col gap-2">
                                                    <Briefcase className="h-5 w-5" />
                                                    <span className="text-sm">공고 검토</span>
                                                </Button>
                                                <Button variant="outline" className="h-auto py-4 flex-col gap-2">
                                                    <Building2 className="h-5 w-5" />
                                                    <span className="text-sm">기업 승인</span>
                                                </Button>
                                                <Button variant="outline" className="h-auto py-4 flex-col gap-2">
                                                    <AlertCircle className="h-5 w-5" />
                                                    <span className="text-sm">신고 처리</span>
                                                </Button>
                                            </div>
                                        </CardContent>
                                    </Card>
                                </div>
                            </TabsContent>

                            <TabsContent value="members" className="mt-6">
                                <Card>
                                    <CardHeader>
                                        <CardTitle className="text-lg">회원 관리</CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <div className="text-center py-12 text-muted-foreground">
                                            <Users className="h-12 w-12 mx-auto mb-4 opacity-50" />
                                            <p>회원 관리 기능이 여기에 표시됩니다.</p>
                                            <p className="text-sm mt-2">API 연동 후 활성화됩니다.</p>
                                        </div>
                                    </CardContent>
                                </Card>
                            </TabsContent>

                            <TabsContent value="jobs" className="mt-6">
                                <Card>
                                    <CardHeader>
                                        <CardTitle className="text-lg">채용공고 관리</CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <div className="text-center py-12 text-muted-foreground">
                                            <Briefcase className="h-12 w-12 mx-auto mb-4 opacity-50" />
                                            <p>채용공고 관리 기능이 여기에 표시됩니다.</p>
                                            <p className="text-sm mt-2">API 연동 후 활성화됩니다.</p>
                                        </div>
                                    </CardContent>
                                </Card>
                            </TabsContent>

                            <TabsContent value="companies" className="mt-6">
                                <Card>
                                    <CardHeader>
                                        <CardTitle className="text-lg">기업 관리</CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <div className="text-center py-12 text-muted-foreground">
                                            <Building2 className="h-12 w-12 mx-auto mb-4 opacity-50" />
                                            <p>기업 관리 기능이 여기에 표시됩니다.</p>
                                            <p className="text-sm mt-2">API 연동 후 활성화됩니다.</p>
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
}
