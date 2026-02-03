import { useState, useEffect } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { getEmployerProfile, saveEmployerProfile } from "@/api/employers";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import {
    Building2,
    Upload,
    Users,
    Settings,
    CreditCard,
    Globe,
    Mail,
    Phone,
    MapPin,
    Crown,
    Shield,
    Bell,
    Eye
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";

import { useSearchParams } from "react-router-dom";
import { CompanyPaymentHistory } from "@/components/company/CompanyPaymentHistory";

// 임시 팀원 데이터
const mockTeamMembers = [
    { id: 1, name: "김대표", email: "ceo@company.com", role: "관리자", avatar: null },
    { id: 2, name: "이인사", email: "hr@company.com", role: "채용담당자", avatar: null },
    { id: 3, name: "박마케터", email: "marketing@company.com", role: "일반", avatar: null },
];

export default function CompanyManagement() {
    const { profile } = useAuth();
    const [searchParams, setSearchParams] = useSearchParams();
    const initialTab = searchParams.get("tab") || "profile";
    const [activeTab, setActiveTab] = useState(initialTab);

    // Sync URL when tab changes
    const handleTabChange = (val: string) => {
        setActiveTab(val);
        setSearchParams(prev => {
            prev.set('tab', val);
            return prev;
        });
    };

    const [profileLoading, setProfileLoading] = useState(true);
    const [profileSaving, setProfileSaving] = useState(false);
    const [profileLoadError, setProfileLoadError] = useState<string | null>(null);
    const [companyProfile, setCompanyProfile] = useState({
        name: "",
        industry: "",
        size: "",
        founded: "",
        website: "",
        email: "",
        phone: "",
        address: "",
        description: "",
        logo: null as string | null,
    });

    useEffect(() => {
        let cancelled = false;
        setProfileLoading(true);
        setProfileLoadError(null);
        getEmployerProfile()
            .then((res: any) => {
                if (cancelled || !res) return;
                setCompanyProfile({
                    name: res.name ?? "",
                    industry: res.industry ?? "",
                    size: res.employeeCount != null ? String(res.employeeCount) : "",
                    founded: res.foundedYear != null ? String(res.foundedYear) : "",
                    website: res.websiteUrl ?? "",
                    email: res.contactEmail ?? "",
                    phone: res.contactPhone ?? "",
                    address: res.location ?? "",
                    description: res.description ?? "",
                    logo: res.logoUrl ?? null,
                });
            })
            .catch(() => {
                if (!cancelled) {
                    setProfileLoadError("기업 정보를 불러오지 못했습니다. 네트워크와 서버를 확인해 주세요.");
                    toast.error("기업 정보를 불러오는데 실패했습니다.");
                }
            })
            .finally(() => {
                if (!cancelled) setProfileLoading(false);
            });
        return () => { cancelled = true; };
    }, []);

    const handleRetryProfile = () => {
        setProfileLoadError(null);
        setProfileLoading(true);
        getEmployerProfile()
            .then((res: any) => {
                if (res) setCompanyProfile({
                    name: res.name ?? "",
                    industry: res.industry ?? "",
                    size: res.employeeCount != null ? String(res.employeeCount) : "",
                    founded: res.foundedYear != null ? String(res.foundedYear) : "",
                    website: res.websiteUrl ?? "",
                    email: res.contactEmail ?? "",
                    phone: res.contactPhone ?? "",
                    address: res.location ?? "",
                    description: res.description ?? "",
                    logo: res.logoUrl ?? null,
                });
                setProfileLoadError(null);
            })
            .catch(() => setProfileLoadError("기업 정보를 불러오지 못했습니다."))
            .finally(() => setProfileLoading(false));
    };

    const handleSaveProfile = async () => {
        setProfileSaving(true);
        try {
            const sizeNum = companyProfile.size ? parseInt(companyProfile.size, 10) : undefined;
            const foundedNum = companyProfile.founded ? parseInt(companyProfile.founded, 10) : undefined;
            await saveEmployerProfile({
                name: companyProfile.name || undefined,
                industry: companyProfile.industry || undefined,
                employeeCount: Number.isNaN(sizeNum) ? undefined : sizeNum,
                foundedYear: Number.isNaN(foundedNum) ? undefined : foundedNum,
                websiteUrl: companyProfile.website || undefined,
                contactEmail: companyProfile.email || undefined,
                contactPhone: companyProfile.phone || undefined,
                location: companyProfile.address || undefined,
                description: companyProfile.description || undefined,
                logoUrl: companyProfile.logo || undefined,
            });
            toast.success("기업 정보가 저장되었습니다.");
        } catch (e: any) {
            toast.error(e?.response?.data?.error ?? "저장에 실패했습니다.");
        } finally {
            setProfileSaving(false);
        }
    };

    // 알림 설정 상태
    const [notifications, setNotifications] = useState({
        newApplicant: true,
        interviewReminder: true,
        offerResponse: true,
        weeklyReport: false,
        marketingEmail: false,
    });

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64 transition-all duration-300">
                <div className="p-6 lg:p-8">
                    {/* 헤더 */}
                    <div className="mb-8">
                        <h1 className="text-3xl font-bold text-foreground">기업 관리</h1>
                        <p className="text-muted-foreground mt-2">
                            기업 정보를 관리하고 팀원을 초대하세요
                        </p>
                    </div>

                    <Tabs value={activeTab} onValueChange={handleTabChange}>
                        <TabsList className="grid w-full grid-cols-4 lg:w-auto lg:inline-flex mb-6">
                            <TabsTrigger value="profile" className="gap-2">
                                <Building2 className="h-4 w-4" />
                                <span className="hidden sm:inline">기업 정보</span>
                            </TabsTrigger>
                            <TabsTrigger value="team" className="gap-2">
                                <Users className="h-4 w-4" />
                                <span className="hidden sm:inline">팀 관리</span>
                            </TabsTrigger>
                            <TabsTrigger value="settings" className="gap-2">
                                <Settings className="h-4 w-4" />
                                <span className="hidden sm:inline">설정</span>
                            </TabsTrigger>
                            <TabsTrigger value="billing" className="gap-2">
                                <CreditCard className="h-4 w-4" />
                                <span className="hidden sm:inline">결제</span>
                            </TabsTrigger>
                        </TabsList>

                        {/* 기업 정보 탭 */}
                        <TabsContent value="profile" className="space-y-6">
                            {profileLoading ? (
                                <div className="flex items-center justify-center py-16">
                                    <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                </div>
                            ) : profileLoadError ? (
                                <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
                                    <p className="text-destructive font-medium mb-2">{profileLoadError}</p>
                                    <p className="text-sm text-muted-foreground mb-4">백엔드 서버가 실행 중인지, 기업 회원으로 로그인했는지 확인해 주세요.</p>
                                    <Button variant="outline" onClick={handleRetryProfile}>
                                        다시 시도
                                    </Button>
                                </div>
                            ) : (
                            <div className="grid gap-6 lg:grid-cols-3">
                                {/* 로고 및 기본 정보 */}
                                <Card className="lg:col-span-1">
                                    <CardHeader>
                                        <CardTitle>기업 로고</CardTitle>
                                        <CardDescription>구직자에게 표시되는 로고입니다</CardDescription>
                                    </CardHeader>
                                    <CardContent className="flex flex-col items-center gap-4">
                                        <Avatar className="h-32 w-32">
                                            <AvatarImage src={companyProfile.logo || undefined} />
                                            <AvatarFallback className="bg-primary/10 text-primary text-3xl">
                                                <Building2 className="h-12 w-12" />
                                            </AvatarFallback>
                                        </Avatar>
                                        <Button variant="outline" className="gap-2">
                                            <Upload className="h-4 w-4" />
                                            로고 업로드
                                        </Button>
                                        <p className="text-xs text-muted-foreground text-center">
                                            권장 크기: 400x400px<br />
                                            지원 형식: PNG, JPG
                                        </p>
                                    </CardContent>
                                </Card>

                                {/* 상세 정보 */}
                                <Card className="lg:col-span-2">
                                    <CardHeader>
                                        <CardTitle>기업 정보</CardTitle>
                                        <CardDescription>기업의 기본 정보를 입력하세요</CardDescription>
                                    </CardHeader>
                                    <CardContent className="space-y-4">
                                        <div className="grid gap-4 sm:grid-cols-2">
                                            <div className="space-y-2">
                                                <Label htmlFor="companyName">기업명</Label>
                                                <Input
                                                    id="companyName"
                                                    value={companyProfile.name}
                                                    onChange={(e) => setCompanyProfile(prev => ({ ...prev, name: e.target.value }))}
                                                />
                                            </div>
                                            <div className="space-y-2">
                                                <Label htmlFor="industry">업종</Label>
                                                <Input
                                                    id="industry"
                                                    value={companyProfile.industry}
                                                    onChange={(e) => setCompanyProfile(prev => ({ ...prev, industry: e.target.value }))}
                                                />
                                            </div>
                                            <div className="space-y-2">
                                                <Label htmlFor="size">기업 규모</Label>
                                                <Input
                                                    id="size"
                                                    value={companyProfile.size}
                                                    onChange={(e) => setCompanyProfile(prev => ({ ...prev, size: e.target.value }))}
                                                />
                                            </div>
                                            <div className="space-y-2">
                                                <Label htmlFor="founded">설립년도</Label>
                                                <Input
                                                    id="founded"
                                                    value={companyProfile.founded}
                                                    onChange={(e) => setCompanyProfile(prev => ({ ...prev, founded: e.target.value }))}
                                                />
                                            </div>
                                        </div>

                                        <div className="space-y-2">
                                            <Label htmlFor="description">기업 소개</Label>
                                            <Textarea
                                                id="description"
                                                rows={4}
                                                value={companyProfile.description}
                                                onChange={(e) => setCompanyProfile(prev => ({ ...prev, description: e.target.value }))}
                                                placeholder="기업 소개를 입력하세요..."
                                            />
                                        </div>

                                        <div className="grid gap-4 sm:grid-cols-2">
                                            <div className="space-y-2">
                                                <Label htmlFor="website" className="flex items-center gap-2">
                                                    <Globe className="h-4 w-4" /> 웹사이트
                                                </Label>
                                                <Input
                                                    id="website"
                                                    value={companyProfile.website}
                                                    onChange={(e) => setCompanyProfile(prev => ({ ...prev, website: e.target.value }))}
                                                />
                                            </div>
                                            <div className="space-y-2">
                                                <Label htmlFor="email" className="flex items-center gap-2">
                                                    <Mail className="h-4 w-4" /> 이메일
                                                </Label>
                                                <Input
                                                    id="email"
                                                    value={companyProfile.email}
                                                    onChange={(e) => setCompanyProfile(prev => ({ ...prev, email: e.target.value }))}
                                                />
                                            </div>
                                            <div className="space-y-2">
                                                <Label htmlFor="phone" className="flex items-center gap-2">
                                                    <Phone className="h-4 w-4" /> 전화번호
                                                </Label>
                                                <Input
                                                    id="phone"
                                                    value={companyProfile.phone}
                                                    onChange={(e) => setCompanyProfile(prev => ({ ...prev, phone: e.target.value }))}
                                                />
                                            </div>
                                            <div className="space-y-2">
                                                <Label htmlFor="address" className="flex items-center gap-2">
                                                    <MapPin className="h-4 w-4" /> 주소
                                                </Label>
                                                <Input
                                                    id="address"
                                                    value={companyProfile.address}
                                                    onChange={(e) => setCompanyProfile(prev => ({ ...prev, address: e.target.value }))}
                                                />
                                            </div>
                                        </div>

                                        <div className="flex justify-end pt-4">
                                            <Button
                                                className="btn-gradient-primary"
                                                onClick={handleSaveProfile}
                                                disabled={profileLoading || profileSaving}
                                            >
                                                {profileSaving ? (
                                                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                                ) : null}
                                                저장하기
                                            </Button>
                                        </div>
                                    </CardContent>
                                </Card>
                            </div>
                            )}
                        </TabsContent>

                        {/* 팀 관리 탭 */}
                        <TabsContent value="team" className="space-y-6">
                            <Card>
                                <CardHeader className="flex flex-row items-center justify-between">
                                    <div>
                                        <CardTitle>팀원 관리</CardTitle>
                                        <CardDescription>기업 계정에 접근할 수 있는 팀원을 관리하세요</CardDescription>
                                    </div>
                                    <Button className="btn-gradient-primary gap-2">
                                        <Users className="h-4 w-4" />
                                        팀원 초대
                                    </Button>
                                </CardHeader>
                                <CardContent>
                                    <div className="space-y-4">
                                        {mockTeamMembers.map((member) => (
                                            <div key={member.id} className="flex items-center justify-between p-4 rounded-lg border">
                                                <div className="flex items-center gap-4">
                                                    <Avatar>
                                                        <AvatarImage src={member.avatar || undefined} />
                                                        <AvatarFallback className="bg-primary/10 text-primary">
                                                            {member.name.charAt(0)}
                                                        </AvatarFallback>
                                                    </Avatar>
                                                    <div>
                                                        <div className="flex items-center gap-2">
                                                            <span className="font-medium">{member.name}</span>
                                                            {member.role === "관리자" && (
                                                                <Crown className="h-4 w-4 text-yellow-500" />
                                                            )}
                                                        </div>
                                                        <p className="text-sm text-muted-foreground">{member.email}</p>
                                                    </div>
                                                </div>
                                                <div className="flex items-center gap-3">
                                                    <Badge variant={member.role === "관리자" ? "default" : "secondary"}>
                                                        {member.role}
                                                    </Badge>
                                                    <Button variant="ghost" size="sm">
                                                        <Settings className="h-4 w-4" />
                                                    </Button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader>
                                    <CardTitle>권한 설명</CardTitle>
                                    <CardDescription>각 역할의 권한을 확인하세요</CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="flex items-start gap-4 p-4 rounded-lg bg-yellow-500/10">
                                        <Crown className="h-5 w-5 text-yellow-500 mt-0.5" />
                                        <div>
                                            <p className="font-medium">관리자</p>
                                            <p className="text-sm text-muted-foreground">
                                                모든 기능에 접근 가능, 팀원 초대/삭제, 결제 관리
                                            </p>
                                        </div>
                                    </div>
                                    <div className="flex items-start gap-4 p-4 rounded-lg bg-blue-500/10">
                                        <Shield className="h-5 w-5 text-blue-500 mt-0.5" />
                                        <div>
                                            <p className="font-medium">채용담당자</p>
                                            <p className="text-sm text-muted-foreground">
                                                채용공고 관리, 지원자 관리, 면접 스케줄 관리
                                            </p>
                                        </div>
                                    </div>
                                    <div className="flex items-start gap-4 p-4 rounded-lg bg-muted">
                                        <Eye className="h-5 w-5 text-muted-foreground mt-0.5" />
                                        <div>
                                            <p className="font-medium">일반</p>
                                            <p className="text-sm text-muted-foreground">
                                                채용공고 및 지원자 조회만 가능
                                            </p>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        </TabsContent>

                        {/* 설정 탭 */}
                        <TabsContent value="settings" className="space-y-6">
                            <Card>
                                <CardHeader>
                                    <CardTitle className="flex items-center gap-2">
                                        <Bell className="h-5 w-5" />
                                        알림 설정
                                    </CardTitle>
                                    <CardDescription>이메일 및 알림 수신 설정을 관리하세요</CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-6">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="font-medium">새 지원자 알림</p>
                                            <p className="text-sm text-muted-foreground">새로운 지원자가 있을 때 알림을 받습니다</p>
                                        </div>
                                        <Switch
                                            checked={notifications.newApplicant}
                                            onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, newApplicant: checked }))}
                                        />
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="font-medium">면접 리마인더</p>
                                            <p className="text-sm text-muted-foreground">예정된 면접 1시간 전에 알림을 받습니다</p>
                                        </div>
                                        <Switch
                                            checked={notifications.interviewReminder}
                                            onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, interviewReminder: checked }))}
                                        />
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="font-medium">제안 응답 알림</p>
                                            <p className="text-sm text-muted-foreground">지원자가 채용 제안에 응답했을 때 알림을 받습니다</p>
                                        </div>
                                        <Switch
                                            checked={notifications.offerResponse}
                                            onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, offerResponse: checked }))}
                                        />
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="font-medium">주간 리포트</p>
                                            <p className="text-sm text-muted-foreground">매주 월요일 채용 현황 리포트를 받습니다</p>
                                        </div>
                                        <Switch
                                            checked={notifications.weeklyReport}
                                            onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, weeklyReport: checked }))}
                                        />
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="font-medium">마케팅 이메일</p>
                                            <p className="text-sm text-muted-foreground">새로운 기능 및 프로모션 안내를 받습니다</p>
                                        </div>
                                        <Switch
                                            checked={notifications.marketingEmail}
                                            onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, marketingEmail: checked }))}
                                        />
                                    </div>
                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader>
                                    <CardTitle className="text-destructive">위험 영역</CardTitle>
                                    <CardDescription>기업 계정 삭제 및 데이터 관리</CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="flex items-center justify-between p-4 rounded-lg border border-destructive/30 bg-destructive/5">
                                        <div>
                                            <p className="font-medium">기업 계정 삭제</p>
                                            <p className="text-sm text-muted-foreground">
                                                모든 데이터가 영구 삭제되며 복구할 수 없습니다
                                            </p>
                                        </div>
                                        <Button variant="destructive">계정 삭제</Button>
                                    </div>
                                </CardContent>
                            </Card>
                        </TabsContent>

                        {/* 결제 탭 */}
                        <TabsContent value="billing" className="space-y-6">
                            <Card>
                                <CardHeader>
                                    <CardTitle>현재 플랜</CardTitle>
                                    <CardDescription>현재 이용 중인 요금제입니다</CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <div className="flex items-center justify-between p-6 rounded-lg bg-gradient-to-r from-primary/10 to-accent/10 border">
                                        <div>
                                            <Badge className="mb-2">PRO</Badge>
                                            <h3 className="text-2xl font-bold">프로 플랜</h3>
                                            <p className="text-muted-foreground">월 99,000원 / 연 990,000원</p>
                                        </div>
                                        <div className="text-right">
                                            <p className="text-sm text-muted-foreground">다음 결제일</p>
                                            <p className="font-medium">2026년 2월 28일</p>
                                            <Button variant="outline" className="mt-2">플랜 변경</Button>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader>
                                    <CardTitle>사용량</CardTitle>
                                    <CardDescription>이번 달 사용량입니다</CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="space-y-2">
                                        <div className="flex justify-between text-sm">
                                            <span>채용공고</span>
                                            <span>8 / 10</span>
                                        </div>
                                        <div className="h-2 rounded-full bg-muted overflow-hidden">
                                            <div className="h-full bg-primary rounded-full" style={{ width: '80%' }} />
                                        </div>
                                    </div>
                                    <div className="space-y-2">
                                        <div className="flex justify-between text-sm">
                                            <span>AI 인재 추천</span>
                                            <span>45 / 100</span>
                                        </div>
                                        <div className="h-2 rounded-full bg-muted overflow-hidden">
                                            <div className="h-full bg-accent rounded-full" style={{ width: '45%' }} />
                                        </div>
                                    </div>
                                    <div className="space-y-2">
                                        <div className="flex justify-between text-sm">
                                            <span>광고 크레딧</span>
                                            <span>250,000원</span>
                                        </div>
                                        <div className="h-2 rounded-full bg-muted overflow-hidden">
                                            <div className="h-full bg-green-500 rounded-full" style={{ width: '50%' }} />
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader>
                                    <CardTitle>결제 수단</CardTitle>
                                    <CardDescription>등록된 결제 수단입니다</CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <div className="flex items-center justify-between p-4 rounded-lg border">
                                        <div className="flex items-center gap-4">
                                            <div className="h-10 w-16 rounded bg-gradient-to-r from-blue-600 to-blue-800 flex items-center justify-center text-white text-xs font-bold">
                                                VISA
                                            </div>
                                            <div>
                                                <p className="font-medium">•••• •••• •••• 4242</p>
                                                <p className="text-sm text-muted-foreground">만료: 12/28</p>
                                            </div>
                                        </div>
                                        <Button variant="outline" size="sm">변경</Button>
                                    </div>
                                    <Button variant="ghost" className="mt-4 w-full">+ 새 결제 수단 추가</Button>
                                </CardContent>
                            </Card>

                            <CompanyPaymentHistory />
                        </TabsContent>
                    </Tabs>
                </div>
            </div>
        </div>
    );
}
