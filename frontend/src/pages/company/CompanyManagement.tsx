import { useState, useEffect, useRef, useCallback } from "react";
import { Header } from "@/components/layout/Header";
import { Sidebar } from "@/components/layout/Sidebar";
import { getEmployerProfile, saveEmployerProfile, uploadEmployerLogo, deleteEmployerLogo } from "@/api/employers";
import { toast } from "sonner";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Slider } from "@/components/ui/slider";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
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
    Eye,
    X,
    ZoomIn,
    ZoomOut,
    RotateCw,
    Loader2
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import Cropper from "react-easy-crop";
import type { Area, Point } from "react-easy-crop";
import { useEmployerProfile } from "@/hooks/useEmployers";
import { useEmployerSubscriptions } from "@/features/payment/hooks/useSubscription";
import { cancelSubscription, resumeSubscription, cancelScheduledProductChange, updateBillingInfo } from "@/api/subscription";
import { loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { format } from "date-fns";
import { ko } from "date-fns/locale";

import { useSearchParams, useNavigate } from "react-router-dom"; // Add useNavigate if needed, but looks like it's not used yet
import { CompanyPaymentHistory } from "@/components/company/CompanyPaymentHistory";

// 크롭된 이미지를 Blob으로 변환하는 유틸리티 함수
const createImage = (url: string): Promise<HTMLImageElement> =>
    new Promise((resolve, reject) => {
        const image = new Image();
        image.addEventListener("load", () => resolve(image));
        image.addEventListener("error", (error) => reject(error));
        image.setAttribute("crossOrigin", "anonymous");
        image.src = url;
    });

const getCroppedImg = async (
    imageSrc: string,
    pixelCrop: Area,
    rotation = 0
): Promise<Blob> => {
    const image = await createImage(imageSrc);
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");

    if (!ctx) {
        throw new Error("No 2d context");
    }

    const rotRad = (rotation * Math.PI) / 180;

    // 회전된 이미지의 바운딩 박스 계산
    const { width: bBoxWidth, height: bBoxHeight } = {
        width: Math.abs(Math.cos(rotRad) * image.width) + Math.abs(Math.sin(rotRad) * image.height),
        height: Math.abs(Math.sin(rotRad) * image.width) + Math.abs(Math.cos(rotRad) * image.height),
    };

    canvas.width = bBoxWidth;
    canvas.height = bBoxHeight;

    ctx.translate(bBoxWidth / 2, bBoxHeight / 2);
    ctx.rotate(rotRad);
    ctx.translate(-image.width / 2, -image.height / 2);
    ctx.drawImage(image, 0, 0);

    // 크롭 영역 추출
    const data = ctx.getImageData(pixelCrop.x, pixelCrop.y, pixelCrop.width, pixelCrop.height);

    canvas.width = pixelCrop.width;
    canvas.height = pixelCrop.height;
    ctx.putImageData(data, 0, 0);

    return new Promise((resolve, reject) => {
        canvas.toBlob(
            (blob) => {
                if (blob) {
                    resolve(blob);
                } else {
                    reject(new Error("Canvas is empty"));
                }
            },
            "image/png",
            1
        );
    });
};

// Toss Payments 카드사 코드 매핑
const cardCompanyMap: { [key: string]: string } = {
    "3K": "기업비씨",
    "46": "광주",
    "71": "롯데",
    "30": "산업",
    "31": "비씨",
    "51": "삼성",
    "38": "새마을",
    "41": "신한",
    "62": "신협",
    "36": "씨티",
    "33": "우리",
    "37": "우체국",
    "39": "저축",
    "35": "전북",
    "42": "제주",
    "15": "카카오뱅크",
    "3A": "케이뱅크",
    "24": "토스뱅크",
    "21": "하나",
    "61": "현대",
    "11": "KB국민",
    "91": "NH농협",
    "34": "수협"
};

// 임시 팀원 데이터
const mockTeamMembers = [
    { id: 1, name: "김대표", email: "ceo@company.com", role: "관리자", avatar: null },
    { id: 2, name: "이인사", email: "hr@company.com", role: "채용담당자", avatar: null },
    { id: 3, name: "박마케터", email: "marketing@company.com", role: "일반", avatar: null },
];

export default function CompanyManagement() {
    const { profile } = useAuth();
    const navigate = useNavigate();
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
    const [isProcessingSubscription, setIsProcessingSubscription] = useState(false);
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


    // 로고 업로드 및 크롭 상태
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [logoUploading, setLogoUploading] = useState(false);
    const [cropDialogOpen, setCropDialogOpen] = useState(false);
    const [imageSrc, setImageSrc] = useState<string | null>(null);
    const [crop, setCrop] = useState<Point>({ x: 0, y: 0 });
    const [zoom, setZoom] = useState(1);
    const [rotation, setRotation] = useState(0);
    const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area | null>(null);

    const handleFileSelect = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // 파일 크기 검증 (5MB 제한)
        if (file.size > 5 * 1024 * 1024) {
            toast.error("파일 크기는 5MB 이하여야 합니다.");
            return;
        }

        // 파일 형식 검증
        const allowedTypes = ["image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp"];
        if (!allowedTypes.includes(file.type)) {
            toast.error("png, jpg, gif, webp 형식만 업로드 가능합니다.");
            return;
        }

        // 이미지 읽어서 크롭 다이얼로그 열기
        const reader = new FileReader();
        reader.onloadend = () => {
            setImageSrc(reader.result as string);
            setCrop({ x: 0, y: 0 });
            setZoom(1);
            setRotation(0);
            setCropDialogOpen(true);
        };
        reader.readAsDataURL(file);

        // 파일 입력 초기화
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    };

    const onCropComplete = useCallback((_croppedArea: Area, croppedAreaPixels: Area) => {
        setCroppedAreaPixels(croppedAreaPixels);
    }, []);

    const handleCropCancel = () => {
        setCropDialogOpen(false);
        setImageSrc(null);
        setCroppedAreaPixels(null);
    };

    const handleCropConfirm = async () => {
        if (!imageSrc || !croppedAreaPixels) return;

        setLogoUploading(true);
        try {
            // 크롭된 이미지를 Blob으로 변환
            const croppedBlob = await getCroppedImg(imageSrc, croppedAreaPixels, rotation);

            // File 객체로 변환
            const croppedFile = new File([croppedBlob], "logo.png", { type: "image/png" });

            // 서버에 업로드
            const result = await uploadEmployerLogo(croppedFile);
            setCompanyProfile(prev => ({ ...prev, logo: result.logoUrl }));
            toast.success("로고가 업로드되었습니다.");

            // 다이얼로그 닫기
            setCropDialogOpen(false);
            setImageSrc(null);
            setCroppedAreaPixels(null);
        } catch (err: any) {
            toast.error(err?.response?.data?.error || "로고 업로드에 실패했습니다.");
        } finally {
            setLogoUploading(false);
        }
    };

    const handleRemoveLogo = async () => {
        setLogoUploading(true);
        try {
            await deleteEmployerLogo();
            setCompanyProfile(prev => ({ ...prev, logo: null }));
            toast.success("로고가 삭제되었습니다.");
        } catch (err: any) {
            toast.error(err?.response?.data?.error || "로고 삭제에 실패했습니다.");
        } finally {
            setLogoUploading(false);
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

    // --- Subscription Data Integration ---
    const { data: employerData, isLoading: isLoadingProfile } = useEmployerProfile();
    const employerId = employerData?.employerId;
    const { data: subscriptions, isLoading: isLoadingSubs } = useEmployerSubscriptions(employerId);

    const currentSubscription = subscriptions?.find(s => s.status === "ACTIVE");
    const nextBillingDate = currentSubscription?.nextBillingAt
        ? format(new Date(currentSubscription.nextBillingAt), "yyyy년 M월 d일", { locale: ko })
        : "-";

    const planName = currentSubscription?.product?.name || "무료 플랜"; // Default to Free if no active
    const planPrice = currentSubscription?.product?.priceAmount || 0;
    const isFree = !currentSubscription;

    const cardCompany = currentSubscription?.cardCompany;
    const cardNumber = currentSubscription?.cardNumber; // Expected to be masked or partial

    // 결제 수단 변경 핸들러
    const handleChangePaymentMethod = async () => {
        try {
            const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
            if (!clientKey) {
                alert("토스 클라이언트 키가 설정되지 않았습니다.");
                return;
            }

            // @ts-ignore
            const tossPayments = await loadTossPayments(clientKey);
            // @ts-ignore
            const payment = tossPayments.payment({
                customerKey: profile?.id ? `USER-${profile.id}` : `ANONYMOUS-${Date.now()}`
            });

            // 현재 URL에 action 파라미터를 추가하여 리다이렉트
            const currentOrigin = window.location.origin;
            const successUrl = new URL(`${currentOrigin}/companies`);
            successUrl.searchParams.set("tab", "billing");
            successUrl.searchParams.set("action", "update_billing");

            await payment.requestBillingAuth({
                method: "CARD",
                successUrl: successUrl.toString(),
                failUrl: window.location.href, // 실패 시 현재 페이지 유지
                customerEmail: profile?.email,
                customerName: profile?.username || "사용자",
            });
        } catch (error) {
            console.error("Billing Auth Request Failed:", error);
            toast.error("결제 수단 변경 요청 중 오류가 발생했습니다.");
        }
    };

    // 결제 수단 변경 콜백 처리 (URL 파라미터 확인)
    useEffect(() => {
        const action = searchParams.get("action");
        const authKey = searchParams.get("authKey");
        const customerKey = searchParams.get("customerKey");

        if (action === "update_billing" && authKey && customerKey && currentSubscription) {
            const updateBilling = async () => {
                const toastId = toast.loading("결제 수단을 변경하고 있습니다...");
                try {
                    await updateBillingInfo(currentSubscription.subscriptionId, { authKey, customerKey });
                    toast.success("결제 수단이 성공적으로 변경되었습니다.", { id: toastId });

                    // 파라미터 제거 및 리로드
                    navigate("/companies?tab=billing", { replace: true });
                    window.location.reload();
                } catch (error) {
                    console.error(error);
                    toast.error("결제 수단 변경에 실패했습니다.", { id: toastId });
                    navigate("/companies?tab=billing", { replace: true });
                }
            };
            updateBilling();
        }
    }, [searchParams, currentSubscription, navigate]);

    return (
        <div className="flex min-h-screen bg-background">
            <Sidebar />

            <div className="flex-1 flex flex-col lg:ml-64">
                <Header />

                <main className="flex-1 p-6 lg:p-8">
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
                                            <div className="relative">
                                                <Avatar className="h-32 w-32">
                                                    <AvatarImage src={companyProfile.logo || undefined} />
                                                    <AvatarFallback className="bg-primary/10 text-primary text-3xl">
                                                        <Building2 className="h-12 w-12" />
                                                    </AvatarFallback>
                                                </Avatar>
                                                {logoUploading && (
                                                    <div className="absolute inset-0 flex items-center justify-center bg-background/80 rounded-full">
                                                        <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                                    </div>
                                                )}
                                            </div>

                                            {/* 숨겨진 파일 입력 */}
                                            <input
                                                type="file"
                                                ref={fileInputRef}
                                                onChange={handleFileChange}
                                                accept="image/png,image/jpeg,image/jpg,image/gif,image/webp"
                                                className="hidden"
                                            />

                                            <div className="flex gap-2 justify-center">
                                                <Button
                                                    variant="outline"
                                                    className="gap-2"
                                                    onClick={handleFileSelect}
                                                    disabled={logoUploading}
                                                >
                                                    <Upload className="h-4 w-4" />
                                                    로고 업로드
                                                </Button>
                                                {companyProfile.logo && (
                                                    <Button
                                                        variant="ghost"
                                                        size="icon"
                                                        onClick={handleRemoveLogo}
                                                        className="text-destructive hover:text-destructive"
                                                        disabled={logoUploading}
                                                    >
                                                        <X className="h-4 w-4" />
                                                    </Button>
                                                )}
                                            </div>
                                            <p className="text-xs text-muted-foreground text-center">
                                                권장 크기: 400x400px<br />
                                                지원 형식: PNG, JPG, GIF, WEBP<br />
                                                최대 크기: 5MB
                                            </p>
                                        </CardContent>
                                    </Card>

                                    {/* 로고 크롭 다이얼로그 */}
                                    <Dialog open={cropDialogOpen} onOpenChange={setCropDialogOpen}>
                                        <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-hidden">
                                            <DialogHeader>
                                                <DialogTitle>로고 이미지 편집</DialogTitle>
                                                <DialogDescription>
                                                    이미지를 드래그하여 위치를 조정하고, 슬라이더로 크기와 회전을 조절하세요
                                                </DialogDescription>
                                            </DialogHeader>

                                            <div className="space-y-4">
                                                {/* 크롭 영역 */}
                                                <div className="relative h-[300px] bg-muted rounded-lg overflow-hidden">
                                                    {imageSrc && (
                                                        <Cropper
                                                            image={imageSrc}
                                                            crop={crop}
                                                            zoom={zoom}
                                                            rotation={rotation}
                                                            aspect={1}
                                                            cropShape="round"
                                                            showGrid={false}
                                                            onCropChange={setCrop}
                                                            onCropComplete={onCropComplete}
                                                            onZoomChange={setZoom}
                                                        />
                                                    )}
                                                </div>

                                                {/* 컨트롤 패널 */}
                                                <div className="space-y-4 px-2">
                                                    {/* 줌 컨트롤 */}
                                                    <div className="space-y-2">
                                                        <div className="flex items-center justify-between">
                                                            <Label className="flex items-center gap-2">
                                                                <ZoomIn className="h-4 w-4" />
                                                                크기 조절
                                                            </Label>
                                                            <span className="text-sm text-muted-foreground">
                                                                {Math.round(zoom * 100)}%
                                                            </span>
                                                        </div>
                                                        <div className="flex items-center gap-3">
                                                            <ZoomOut className="h-4 w-4 text-muted-foreground" />
                                                            <Slider
                                                                value={[zoom]}
                                                                min={1}
                                                                max={3}
                                                                step={0.1}
                                                                onValueChange={(value) => setZoom(value[0])}
                                                                className="flex-1"
                                                            />
                                                            <ZoomIn className="h-4 w-4 text-muted-foreground" />
                                                        </div>
                                                    </div>

                                                    {/* 회전 컨트롤 */}
                                                    <div className="space-y-2">
                                                        <div className="flex items-center justify-between">
                                                            <Label className="flex items-center gap-2">
                                                                <RotateCw className="h-4 w-4" />
                                                                회전
                                                            </Label>
                                                            <span className="text-sm text-muted-foreground">
                                                                {rotation}°
                                                            </span>
                                                        </div>
                                                        <Slider
                                                            value={[rotation]}
                                                            min={0}
                                                            max={360}
                                                            step={1}
                                                            onValueChange={(value) => setRotation(value[0])}
                                                        />
                                                    </div>
                                                </div>
                                            </div>

                                            <DialogFooter className="gap-2 sm:gap-0">
                                                <Button
                                                    variant="outline"
                                                    onClick={handleCropCancel}
                                                    disabled={logoUploading}
                                                >
                                                    취소
                                                </Button>
                                                <Button
                                                    onClick={handleCropConfirm}
                                                    disabled={logoUploading}
                                                    className="gap-2"
                                                >
                                                    {logoUploading && <Loader2 className="h-4 w-4 animate-spin" />}
                                                    적용하기
                                                </Button>
                                            </DialogFooter>
                                        </DialogContent>
                                    </Dialog>

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
                                    {isLoadingProfile || isLoadingSubs ? (
                                        <div className="flex justify-center py-8">
                                            <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                        </div>
                                    ) : (
                                        <div className="flex items-center justify-between p-6 rounded-lg bg-gradient-to-r from-primary/10 to-accent/10 border">
                                            <div>
                                                <Badge className="mb-2">{isFree ? "Free" : "PRO"}</Badge>
                                                <h3 className="text-2xl font-bold">{planName}</h3>
                                                <p className="text-muted-foreground">
                                                    {isFree ? "무료" : `월 ${Number(planPrice).toLocaleString()}원`}
                                                </p>
                                            </div>
                                            <div className="text-right">
                                                {isFree ? (
                                                    <div className="text-right">
                                                        <p className="text-sm text-muted-foreground">구독 중인 플랜이 없습니다.</p>
                                                        <Button variant="default" className="mt-2" onClick={() => navigate('/subscription')}>
                                                            구독하러 가기
                                                        </Button>
                                                    </div>
                                                ) : (
                                                    <>
                                                        {currentSubscription?.endedAt ? (
                                                            <>
                                                                <div className="mb-2">
                                                                    <Badge variant="destructive" className="mb-1">해지 예약됨</Badge>
                                                                    <p className="text-sm text-destructive font-medium">
                                                                        {format(new Date(currentSubscription.endedAt), "yyyy년 MM월 dd일")} 종료 예정
                                                                    </p>
                                                                </div>
                                                                <Button
                                                                    variant="outline"
                                                                    className="mt-2 border-primary text-primary hover:bg-primary/5"
                                                                    disabled={isProcessingSubscription}
                                                                    onClick={async () => {
                                                                        if (!confirm("해지 예약을 취소하고 구독을 유지하시겠습니까?")) return;
                                                                        if (!currentSubscription?.subscriptionId) return;

                                                                        setIsProcessingSubscription(true);
                                                                        try {
                                                                            await resumeSubscription(currentSubscription.subscriptionId);
                                                                            alert("구독이 정상적으로 재개되었습니다.");
                                                                            window.location.reload();
                                                                        } catch (e) {
                                                                            console.error(e);
                                                                            alert("처리 중 오류가 발생했습니다.");
                                                                        } finally {
                                                                            setIsProcessingSubscription(false);
                                                                        }
                                                                    }}
                                                                >
                                                                    {isProcessingSubscription ? <Loader2 className="h-4 w-4 animate-spin" /> : "해지 취소 (구독 유지)"}
                                                                </Button>
                                                            </>
                                                        ) : currentSubscription?.nextProduct ? (
                                                            <>
                                                                <div className="mb-2">
                                                                    <Badge className="mb-1 bg-blue-100 text-blue-700 hover:bg-blue-200 border-blue-200">
                                                                        변경 예약됨
                                                                    </Badge>
                                                                    <p className="text-sm font-medium text-blue-700">
                                                                        {format(new Date(currentSubscription.nextBillingAt), "yyyy년 MM월 dd일")}부터<br />
                                                                        {currentSubscription.nextProduct.name} 플랜 적용
                                                                    </p>
                                                                </div>
                                                                <div className="flex justify-end gap-2 mt-2">
                                                                    <Button
                                                                        variant="outline"
                                                                        onClick={() => navigate('/subscription')}
                                                                    >
                                                                        플랜 변경
                                                                    </Button>
                                                                    <Button
                                                                        variant="outline"
                                                                        className="text-muted-foreground hover:text-foreground"
                                                                        disabled={isProcessingSubscription}
                                                                        onClick={async () => {
                                                                            if (!confirm("플랜 변경 예약을 취소하시겠습니까?")) return;
                                                                            if (!currentSubscription?.subscriptionId) return;

                                                                            setIsProcessingSubscription(true);
                                                                            try {
                                                                                await cancelScheduledProductChange(currentSubscription.subscriptionId);
                                                                                alert("플랜 변경 예약이 취소되었습니다.");
                                                                                window.location.reload();
                                                                            } catch (e) {
                                                                                console.error(e);
                                                                                alert("처리 중 오류가 발생했습니다.");
                                                                            } finally {
                                                                                setIsProcessingSubscription(false);
                                                                            }
                                                                        }}
                                                                    >
                                                                        {isProcessingSubscription ? <Loader2 className="h-4 w-4 animate-spin" /> : "변경 예약 취소"}
                                                                    </Button>
                                                                    <Button
                                                                        variant="ghost"
                                                                        className="text-muted-foreground hover:text-destructive"
                                                                        disabled={isProcessingSubscription}
                                                                        onClick={async () => {
                                                                            if (!confirm("정말로 구독 취소를 진행하시겠습니까?")) return;
                                                                            if (!currentSubscription?.subscriptionId) return;

                                                                            setIsProcessingSubscription(true);
                                                                            try {
                                                                                await cancelSubscription(currentSubscription.subscriptionId);
                                                                                alert("해지 예약이 완료되었습니다.\n다음 결제일에 구독이 종료됩니다.");
                                                                                window.location.reload();
                                                                            } catch (e) {
                                                                                console.error(e);
                                                                                alert("처리 중 오류가 발생했습니다.");
                                                                            } finally {
                                                                                setIsProcessingSubscription(false);
                                                                            }
                                                                        }}
                                                                    >
                                                                        {isProcessingSubscription ? <Loader2 className="h-4 w-4 animate-spin" /> : "구독 취소"}
                                                                    </Button>
                                                                </div>
                                                            </>
                                                        ) : (
                                                            <>
                                                                <p className="text-sm text-muted-foreground">다음 결제일</p>
                                                                <p className="font-medium">{nextBillingDate}</p>
                                                                <div className="flex justify-end gap-2 mt-2">
                                                                    <Button
                                                                        variant="outline"
                                                                        onClick={() => navigate('/subscription')}
                                                                    >
                                                                        플랜 변경
                                                                    </Button>
                                                                    <Button
                                                                        variant="ghost"
                                                                        className="text-muted-foreground hover:text-destructive"
                                                                        disabled={isProcessingSubscription}
                                                                        onClick={async () => {
                                                                            if (!confirm("정말로 구독을 취소하시겠습니까?\n취소하더라도 다음 결제일까지는 혜택이 유지되며, 이후 자동 결제가 중단됩니다.")) return;
                                                                            if (!currentSubscription?.subscriptionId) return;

                                                                            setIsProcessingSubscription(true);
                                                                            try {
                                                                                await cancelSubscription(currentSubscription.subscriptionId);
                                                                                alert("해지 예약이 완료되었습니다.\n다음 결제일에 구독이 종료됩니다.");
                                                                                window.location.reload();
                                                                            } catch (e) {
                                                                                console.error(e);
                                                                                alert("처리 중 오류가 발생했습니다.");
                                                                            } finally {
                                                                                setIsProcessingSubscription(false);
                                                                            }
                                                                        }}
                                                                    >
                                                                        {isProcessingSubscription ? <Loader2 className="h-4 w-4 animate-spin" /> : "구독 취소"}
                                                                    </Button>
                                                                </div>
                                                            </>
                                                        )}
                                                    </>
                                                )}

                                            </div>
                                        </div>
                                    )}
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
                                    {!isFree && cardCompany ? (
                                        <div className="flex items-center justify-between p-4 rounded-lg border">
                                            <div className="flex items-center gap-4">
                                                <div className="h-10 w-16 rounded bg-gradient-to-r from-blue-600 to-blue-800 flex items-center justify-center text-white text-xs font-bold">
                                                    {cardCompany && cardCompanyMap[cardCompany] ? cardCompanyMap[cardCompany] : cardCompany}
                                                </div>
                                                <div>
                                                    <p className="font-medium">{cardNumber || "•••• •••• •••• ••••"}</p>
                                                    <p className="text-sm text-muted-foreground">구독 결제 카드</p>
                                                </div>
                                            </div>
                                            <Button variant="outline" size="sm" onClick={handleChangePaymentMethod}>변경</Button>
                                        </div>
                                    ) : (
                                        <div className="text-center py-6 text-muted-foreground">
                                            <p className="mb-4">등록된 결제 수단이 없습니다.</p>
                                        </div>
                                    )}
                                    {/* <Button variant="ghost" className="mt-4 w-full">+ 새 결제 수단 추가</Button> */}
                                </CardContent>
                            </Card>

                            <CompanyPaymentHistory />
                        </TabsContent>
                    </Tabs>
                </main>
            </div>
        </div>
    );
}
