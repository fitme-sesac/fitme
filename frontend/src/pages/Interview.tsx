import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";
import { Calendar, User, Clock, MapPin, Video, CheckCircle2 } from "lucide-react";
import { Link } from "react-router-dom";

export default function Interview() {
    const { user } = useAuth();

    // 비로그인 상태의 면접 페이지 (소개 화면)
    if (!user) {
        return (
            <div className="min-h-screen bg-background">
                <Sidebar />
                <div className="lg:pl-64 transition-all duration-300">
                    <Header />
                    <main className="container max-w-7xl mx-auto py-12 px-6">
                        <div className="text-center max-w-2xl mx-auto mb-16">
                            <h1 className="text-3xl md:text-4xl font-bold mb-4">면접 일정 관리</h1>
                            <p className="text-muted-foreground text-lg">
                                복잡한 면접 일정을 한눈에 확인하고,<br />
                                놓치지 않도록 미리 알림을 받아보세요.
                            </p>
                        </div>

                        <div className="grid md:grid-cols-3 gap-8 mb-16">
                            <Card className="border-none shadow-lg bg-gradient-to-br from-white to-slate-50 dark:from-zinc-900 dark:to-zinc-950">
                                <CardContent className="pt-8 pb-8 text-center space-y-4">
                                    <div className="w-14 h-14 bg-blue-100 dark:bg-blue-900/30 rounded-2xl flex items-center justify-center mx-auto text-blue-600 dark:text-blue-400">
                                        <Calendar className="h-7 w-7" />
                                    </div>
                                    <h3 className="text-xl font-bold">통합 일정 관리</h3>
                                    <p className="text-muted-foreground">여러 기업의 면접 일정을<br />하나의 캘린더에서 관리하세요.</p>
                                </CardContent>
                            </Card>
                            <Card className="border-none shadow-lg bg-gradient-to-br from-white to-slate-50 dark:from-zinc-900 dark:to-zinc-950">
                                <CardContent className="pt-8 pb-8 text-center space-y-4">
                                    <div className="w-14 h-14 bg-purple-100 dark:bg-purple-900/30 rounded-2xl flex items-center justify-center mx-auto text-purple-600 dark:text-purple-400">
                                        <Video className="h-7 w-7" />
                                    </div>
                                    <h3 className="text-xl font-bold">화상 면접 연동</h3>
                                    <p className="text-muted-foreground">화상 면접 링크를 바로 확인하고<br />원클릭으로 입장하세요.</p>
                                </CardContent>
                            </Card>
                            <Card className="border-none shadow-lg bg-gradient-to-br from-white to-slate-50 dark:from-zinc-900 dark:to-zinc-950">
                                <CardContent className="pt-8 pb-8 text-center space-y-4">
                                    <div className="w-14 h-14 bg-green-100 dark:bg-green-900/30 rounded-2xl flex items-center justify-center mx-auto text-green-600 dark:text-green-400">
                                        <CheckCircle2 className="h-7 w-7" />
                                    </div>
                                    <h3 className="text-xl font-bold">결과 확인</h3>
                                    <p className="text-muted-foreground">면접 진행 상태와 합격 여부를<br />실시간으로 확인하세요.</p>
                                </CardContent>
                            </Card>
                        </div>

                        <div className="text-center">
                            <Button asChild size="lg" className="h-12 px-8 bg-gradient-to-r from-[#5A639C] to-[#9B86BD] hover:opacity-90 transition-opacity shadow-lg shadow-[#5A639C]/20 border-0 text-white text-lg">
                                <Link to="/auth">로그인하고 시작하기</Link>
                            </Button>
                        </div>
                    </main>
                    <Footer />
                </div>
            </div>
        );
    }

    // 로그인 상태의 면접 페이지 (대시보드 스타일)
    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64 transition-all duration-300">
                <Header />
                <main className="container max-w-7xl mx-auto py-8 px-4 md:px-8">
                    {/* Dashboard Header */}
                    <div className="flex items-center justify-between mb-8">
                        <div>
                            <h1 className="text-2xl font-bold mb-1">면접 일정</h1>
                            <p className="text-muted-foreground">예정된 면접 일정을 확인하고 준비하세요.</p>
                        </div>
                        <Button className="bg-[#5A639C]">
                            <Calendar className="h-4 w-4 mr-2" />
                            캘린더 보기
                        </Button>
                    </div>

                    {/* Stats */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                        <Card>
                            <CardContent className="p-6 flex items-center gap-4">
                                <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center text-blue-600">
                                    <Calendar className="h-6 w-6" />
                                </div>
                                <div>
                                    <p className="text-sm text-muted-foreground">예정된 면접</p>
                                    <p className="text-2xl font-bold">0건</p>
                                </div>
                            </CardContent>
                        </Card>
                        <Card>
                            <CardContent className="p-6 flex items-center gap-4">
                                <div className="w-12 h-12 rounded-full bg-yellow-100 flex items-center justify-center text-yellow-600">
                                    <Clock className="h-6 w-6" />
                                </div>
                                <div>
                                    <p className="text-sm text-muted-foreground">대기 중인 결과</p>
                                    <p className="text-2xl font-bold">0건</p>
                                </div>
                            </CardContent>
                        </Card>
                        <Card>
                            <CardContent className="p-6 flex items-center gap-4">
                                <div className="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center text-green-600">
                                    <CheckCircle2 className="h-6 w-6" />
                                </div>
                                <div>
                                    <p className="text-sm text-muted-foreground">최근 합격</p>
                                    <p className="text-2xl font-bold">0건</p>
                                </div>
                            </CardContent>
                        </Card>
                    </div>

                    {/* Empty State / List */}
                    <Card className="min-h-[400px]">
                        <CardContent className="flex flex-col items-center justify-center h-full py-20 text-center">
                            <div className="w-20 h-20 bg-muted/30 rounded-full flex items-center justify-center mb-6">
                                <Calendar className="h-10 w-10 text-muted-foreground" />
                            </div>
                            <h2 className="text-xl font-semibold mb-2">예정된 면접이 없습니다</h2>
                            <p className="text-muted-foreground max-w-sm mb-6">
                                서류 합격 후 면접 일정이 잡히면<br />
                                이곳에서 상세 내용과 화상 면접 링크를 확인할 수 있습니다.
                            </p>
                            <Button variant="outline" asChild>
                                <Link to="/jobs">채용공고 둘러보기</Link>
                            </Button>
                        </CardContent>
                    </Card>
                </main>
                <Footer />
            </div>
        </div>
    );
}
