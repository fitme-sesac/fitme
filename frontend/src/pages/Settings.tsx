import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { Loader2, Bell, Lock, UserX, ChevronRight, AlertTriangle, Settings as SettingsIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { PaymentResultModal } from "@/components/payment/PaymentResultModal";

type SettingsTab = "notification" | "password" | "delete";

export default function Settings() {
    const [searchParams] = useSearchParams();
    const initialTab = (searchParams.get("tab") as SettingsTab) || "notification";
    const [activeTab, setActiveTab] = useState<SettingsTab>(initialTab);
    const { user, userRole } = useAuth() as any;

    type MenuSection = {
        title: string;
        items: { id: SettingsTab; label: string; icon: any; danger?: boolean }[];
    };

    const menuGroups: MenuSection[] = [
        {
            title: "서비스 설정",
            items: [
                { id: "notification", label: "알림 설정", icon: Bell },
            ]
        },
        {
            title: "계정 보안",
            items: [
                { id: "password", label: "비밀번호 설정", icon: Lock },
                { id: "delete", label: "회원 탈퇴", icon: UserX, danger: true },
            ]
        },
    ];

    const renderContent = () => {
        // 공통 헤더 스타일 (Support.tsx와 동일하게)
        const ContentHeader = ({ title, icon: Icon, danger }: { title: string, icon?: any, danger?: boolean }) => (
            <div className="mb-8 border-b pb-6">
                <div className="flex items-center gap-3 mb-4">
                    {Icon && (
                        <div className={cn("flex items-center justify-center w-12 h-12 rounded-xl", danger ? "bg-red-100 dark:bg-red-900/20" : "bg-sky-100 dark:bg-sky-900/20")}>
                            <Icon className={cn("h-6 w-6", danger ? "text-red-600 dark:text-red-500" : "text-sky-600")} />
                        </div>
                    )}
                    <div>
                        <h2 className={cn("text-3xl font-bold tracking-tight", danger ? "text-red-600 dark:text-red-500" : "text-foreground")}>{title}</h2>
                    </div>
                </div>
            </div>
        );

        switch (activeTab) {
            case "notification":
                return (
                    <div className="bg-card rounded-2xl shadow-sm border p-8 min-h-[600px]">
                        <ContentHeader title="알림 설정" icon={Bell} />
                        <div className="space-y-8 max-w-2xl mx-auto">
                            <div className="flex items-center justify-between space-x-4 p-4 rounded-xl border bg-muted/20">
                                <div className="space-y-1">
                                    <Label htmlFor="email-noti" className="text-base font-semibold">이메일 알림</Label>
                                    <p className="text-sm text-muted-foreground">
                                        주요 공지사항 및 업데이트 소식을 이메일로 받습니다.
                                    </p>
                                </div>
                                <Switch id="email-noti" defaultChecked />
                            </div>
                            <div className="flex items-center justify-between space-x-4 p-4 rounded-xl border bg-muted/20">
                                <div className="space-y-1">
                                    <Label htmlFor="push-noti" className="text-base font-semibold">마케팅 정보 수신</Label>
                                    <p className="text-sm text-muted-foreground">
                                        맞춤형 이벤트 및 혜택 정보를 받습니다.
                                    </p>
                                </div>
                                <Switch id="push-noti" />
                            </div>
                        </div>
                    </div>
                );
            case "password":
                return (
                    <div className="bg-card rounded-2xl shadow-sm border p-8 min-h-[600px]">
                        <ContentHeader title="비밀번호 설정" icon={Lock} />
                        <div className="max-w-md space-y-6 mx-auto">
                            <div className="space-y-2">
                                <Label htmlFor="current-password">현재 비밀번호</Label>
                                <Input id="current-password" type="password" placeholder="현재 비밀번호를 입력하세요" className="h-11" />
                            </div>
                            <div className="pt-2 space-y-2">
                                <Label htmlFor="new-password">새 비밀번호</Label>
                                <Input id="new-password" type="password" placeholder="새 비밀번호 (8자 이상)" className="h-11" />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="confirm-password">새 비밀번호 확인</Label>
                                <Input id="confirm-password" type="password" placeholder="새 비밀번호를 다시 입력하세요" className="h-11" />
                            </div>
                            <div className="pt-6">
                                <Button className="w-full h-11 text-lg font-medium text-white border-0 transition-all hover:scale-[1.02]" style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }}>
                                    비밀번호 변경
                                </Button>
                            </div>
                        </div>
                    </div>
                );
            case "delete":
                return (
                    <div className="bg-card rounded-2xl shadow-sm border p-8 min-h-[600px] border-red-100 dark:border-red-900/20">
                        <ContentHeader title="회원 탈퇴" icon={UserX} danger />

                        <div className="space-y-8 max-w-2xl mx-auto">
                            <div className="bg-red-50 dark:bg-red-900/10 p-6 rounded-xl flex items-start gap-4">
                                <AlertTriangle className="h-6 w-6 text-red-600 mt-0.5 shrink-0" />
                                <div className="space-y-3 text-sm text-red-800 dark:text-red-200">
                                    <p className="font-bold text-base">탈퇴 전 꼭 확인해주세요!</p>
                                    <ul className="list-disc pl-4 space-y-2 opacity-90 leading-relaxed">
                                        <li>탈퇴 시 모든 계정 정보와 활동 기록은 <strong>즉시 삭제되며 복구할 수 없습니다.</strong></li>
                                        <li>작성하신 커뮤니티 글과 댓글은 자동으로 삭제되지 않습니다.</li>
                                        <li>진행 중인 지원 내역이 있다면 탈퇴가 불가능할 수 있습니다.</li>
                                    </ul>
                                </div>
                            </div>

                            <div className="space-y-4 pt-4 border-t">
                                <div className="space-y-2">
                                    <Label htmlFor="delete-confirm">비밀번호 확인</Label>
                                    <Input id="delete-confirm" type="password" placeholder="본인 확인을 위해 비밀번호를 입력하세요" className="h-11" />
                                </div>
                                <div className="pt-4">
                                    <Button variant="destructive" className="w-full h-11 text-lg font-medium">
                                        회원 탈퇴하기
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </div>
                );
            default:
                return null;
        }
    };

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64 transition-all duration-300">
                <Header />
                <main className="container max-w-5xl mx-auto py-12 px-4 md:px-8">
                    {/* Page Title Area (Same as Support.tsx) */}
                    <div className="flex items-end justify-between mb-8 border-b pb-4">
                        <div className="flex items-center gap-3">
                            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-sky-500 text-white">
                                <SettingsIcon className="h-6 w-6" />
                            </div>
                            <h1 className="text-2xl font-bold">설정</h1>
                        </div>
                        <div className="text-sm text-muted-foreground pb-1">
                            계정 및 서비스 이용 환경을 설정합니다.
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
                        {/* Sidebar Menu - Left Column (Same styling as Support.tsx) */}
                        <div className="lg:col-span-1">
                            <div className="sticky top-24 space-y-8">
                                {menuGroups.map((group, groupIndex) => (
                                    <div key={groupIndex}>
                                        <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3 px-4">
                                            {group.title}
                                        </h3>
                                        <div className="space-y-1">
                                            {group.items.map((item) => {
                                                const isActive = activeTab === item.id;
                                                return (
                                                    <button
                                                        key={item.id}
                                                        onClick={() => setActiveTab(item.id)}
                                                        className={cn(
                                                            "w-full flex items-center justify-between px-4 py-3 text-sm font-medium rounded-xl transition-all duration-200",
                                                            isActive
                                                                ? item.danger
                                                                    ? "bg-red-50 text-red-600 shadow-sm border border-red-100" // Danger item active style
                                                                    : "bg-sky-500 text-white shadow-md shadow-sky-200/50" // Normal item active style
                                                                : item.danger
                                                                    ? "text-red-500 hover:bg-red-50" // Danger item inactive hover
                                                                    : "text-muted-foreground hover:bg-muted hover:text-foreground" // Normal item inactive hover
                                                        )}
                                                    >
                                                        <div className="flex items-center gap-3">
                                                            <item.icon className={cn("h-4 w-4", !isActive && item.danger && "text-red-500")} />
                                                            <span>{item.label}</span>
                                                        </div>
                                                        {isActive && !item.danger && <ChevronRight className="h-4 w-4 text-white/80" />}
                                                    </button>
                                                );
                                            })}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Right Content Area */}
                        <div className="lg:col-span-3">
                            {renderContent()}
                        </div>
                    </div>
                </main>
                <Footer />
            </div>
            <PaymentResultModal />
        </div>
    );
}
