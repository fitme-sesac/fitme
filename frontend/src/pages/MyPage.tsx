import { Navigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import JobSeekerMyPage from "./JobSeekerMyPage";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { LogIn, Sparkles, Briefcase, FileText, User, MessageSquare } from "lucide-react";
import { Link } from "react-router-dom";
import { Loader2 } from "lucide-react";

/**
 * 마이페이지 라우팅 컴포넌트
 * - 비로그인: 로그인 유도 화면
 * - 구직자(CANDIDATE): JobSeekerMyPage
 * - 기업(EMPLOYER): /company/dashboard 리다이렉트
 * - 관리자(SERVICEADMIN/APPROVEADMIN/MASTER): /admin/dashboard 리다이렉트
 */
export default function MyPage() {
    const { user, loading, isCompany, isAdmin } = useAuth();

    // 로딩 중
    if (loading) {
        return (
            <div className="min-h-screen bg-background flex items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    // 비로그인 상태: 로그인 유도 화면
    if (!user) {
        return (
            <div className="min-h-screen bg-background">
                <Sidebar />
                <div className="lg:pl-64 transition-all duration-300">
                    <Header />
                    <main className="p-6">
                        <div className="max-w-2xl mx-auto">
                            <Card className="overflow-hidden">
                                <div className="bg-gradient-to-br from-primary/10 to-primary/5 p-8 text-center">
                                    <div className="mx-auto h-20 w-20 rounded-full bg-primary/10 flex items-center justify-center mb-6">
                                        <User className="h-10 w-10 text-primary" />
                                    </div>
                                    <h1 className="text-2xl font-bold mb-2">마이페이지</h1>
                                    <p className="text-muted-foreground">
                                        로그인하고 나만의 커리어를 관리하세요
                                    </p>
                                </div>
                                <CardContent className="p-6">
                                    {/* 기능 안내 */}
                                    <div className="grid gap-4 mb-6">
                                        <div className="flex items-center gap-3 p-3 rounded-lg bg-secondary/50">
                                            <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
                                                <Briefcase className="h-5 w-5 text-primary" />
                                            </div>
                                            <div>
                                                <p className="font-medium">지원 현황 관리</p>
                                                <p className="text-sm text-muted-foreground">내가 지원한 공고의 진행 상태를 확인하세요</p>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-3 p-3 rounded-lg bg-secondary/50">
                                            <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
                                                <FileText className="h-5 w-5 text-primary" />
                                            </div>
                                            <div>
                                                <p className="font-medium">이력서 관리</p>
                                                <p className="text-sm text-muted-foreground">이력서를 작성하고 AI 분석을 받아보세요</p>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-3 p-3 rounded-lg bg-secondary/50">
                                            <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
                                                <Sparkles className="h-5 w-5 text-primary" />
                                            </div>
                                            <div>
                                                <p className="font-medium">AI 맞춤 추천</p>
                                                <p className="text-sm text-muted-foreground">내 이력에 맞는 채용공고를 추천받으세요</p>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-3 p-3 rounded-lg bg-secondary/50">
                                            <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
                                                <MessageSquare className="h-5 w-5 text-primary" />
                                            </div>
                                            <div>
                                                <p className="font-medium">커뮤니티 활동</p>
                                                <p className="text-sm text-muted-foreground">내가 쓴 글과 댓글을 한눈에 모아보세요</p>
                                            </div>
                                        </div>
                                    </div>

                                    {/* 로그인 버튼 */}
                                    <div className="flex flex-col gap-3">
                                        <Button asChild className="w-full btn-gradient-primary" size="lg">
                                            <Link to="/auth">
                                                <LogIn className="h-5 w-5 mr-2" />
                                                로그인
                                            </Link>
                                        </Button>
                                        <p className="text-center text-sm text-muted-foreground">
                                            계정이 없으신가요?{" "}
                                            <Link to="/auth?tab=register" className="text-primary hover:underline">
                                                회원가입
                                            </Link>
                                        </p>
                                    </div>
                                </CardContent>
                            </Card>
                        </div>
                    </main>
                    <Footer />
                </div>
            </div>
        );
    }

    // 관리자: 관리자 대시보드로 리다이렉트
    if (isAdmin) {
        return <Navigate to="/admin/dashboard" replace />;
    }

    // 기업 회원: 기업 대시보드로 리다이렉트
    if (isCompany) {
        return <Navigate to="/company/dashboard" replace />;
    }

    // 구직자: JobSeekerMyPage 렌더링
    return <JobSeekerMyPage />;
}
