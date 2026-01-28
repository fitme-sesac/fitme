import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Button } from "@/components/ui/button";
import { FileText, Download, Sparkles, CheckCircle2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useNavigate } from "react-router-dom";

export default function Resume() {
    const { user } = useAuth();
    const navigate = useNavigate();

    const handleAction = () => {
        if (!user) {
            navigate("/auth");
        } else {
            // TODO: 실제 이력서 작성 페이지로 이동
            alert("이력서 작성 페이지 기능 준비 중입니다.");
        }
    };

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64 transition-all duration-300">
                <Header />
                <main>
                    {/* Hero Section */}
                    {/* 간격을 넓히기 위해 py-32로 상하 여백 대폭 증가 */}
                    <section className="relative py-32 px-6 overflow-hidden bg-gradient-to-b from-background to-[#5A639C]/5">
                        <div className="max-w-5xl mx-auto text-center relative z-10">
                            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight mb-8 leading-tight">
                                <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#5A639C] to-[#9B86BD]">
                                    이력서 작성부터 첨삭까지,
                                </span><br />
                                <span>FitMe에서 한 번에</span>
                            </h1>
                            <p className="text-lg md:text-xl text-muted-foreground mb-12 max-w-2xl mx-auto leading-relaxed">
                                직군별 맞춤 템플릿으로 쉽고 빠르게 작성하고,<br className="hidden md:block" />
                                AI 코칭과 정밀한 첨삭으로 더 완벽한 이력서를 완성해보세요.
                            </p>
                            <div className="flex flex-col sm:flex-row gap-5 justify-center">
                                <Button size="lg" className="h-14 px-10 text-lg font-semibold bg-gradient-to-r from-[#5A639C] to-[#9B86BD] hover:opacity-90 transition-opacity shadow-lg shadow-[#5A639C]/20 border-0" onClick={handleAction}>
                                    새 이력서 작성하기
                                </Button>
                                <Button size="lg" variant="outline" className="h-14 px-10 text-lg hover:bg-muted/50 border-input bg-background" onClick={handleAction}>
                                    샘플 이력서 보기
                                </Button>
                            </div>
                        </div>
                    </section>

                    {/* Features Section */}
                    {/* 섹션 간격을 넓히기 위해 py-24 -> py-32 */}
                    <section className="py-32 px-6 bg-white dark:bg-zinc-900 border-t border-border/50">
                        <div className="max-w-6xl mx-auto grid md:grid-cols-2 gap-16 md:gap-24 items-center">
                            <div className="space-y-10">
                                <div className="space-y-6">
                                    <h2 className="text-3xl md:text-4xl font-bold leading-tight">이력서 하나로<br />모든 채용 사이트 지원</h2>
                                    <p className="text-muted-foreground text-lg leading-relaxed">
                                        작성한 이력서는 PDF로 다운로드하여 다른 채용 플랫폼에도<br />
                                        자유롭게 제출할 수 있습니다.
                                    </p>
                                </div>
                                <div className="space-y-8">
                                    <div className="flex items-start gap-4">
                                        <div className="mt-1 bg-[#5A639C]/10 p-2 rounded-lg">
                                            <CheckCircle2 className="h-6 w-6 text-[#5A639C]" />
                                        </div>
                                        <div>
                                            <h3 className="font-semibold text-xl mb-2">직군별 맞춤 템플릿</h3>
                                            <p className="text-muted-foreground">개발, 디자인, 마케팅 등 직무에 최적화된 레이아웃을 제공합니다.</p>
                                        </div>
                                    </div>
                                    <div className="flex items-start gap-4">
                                        <div className="mt-1 bg-[#5A639C]/10 p-2 rounded-lg">
                                            <CheckCircle2 className="h-6 w-6 text-[#5A639C]" />
                                        </div>
                                        <div>
                                            <h3 className="font-semibold text-xl mb-2">AI 내용 분석</h3>
                                            <p className="text-muted-foreground">오타 수정부터 문맥 교정까지, AI가 더 나은 표현을 제안합니다.</p>
                                        </div>
                                    </div>
                                    <div className="flex items-start gap-4">
                                        <div className="mt-1 bg-[#5A639C]/10 p-2 rounded-lg">
                                            <CheckCircle2 className="h-6 w-6 text-[#5A639C]" />
                                        </div>
                                        <div>
                                            <h3 className="font-semibold text-xl mb-2">실시간 미리보기</h3>
                                            <p className="text-muted-foreground">작성한 내용이 채용 담당자에게 어떻게 보일지 바로 확인하세요.</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div className="relative">
                                <div className="absolute inset-0 bg-gradient-to-tr from-[#5A639C]/20 to-transparent rounded-2xl transform translate-x-6 translate-y-6 -z-10" />
                                <div className="bg-card border rounded-2xl shadow-xl p-8 aspect-[4/5] flex flex-col relative overflow-hidden">
                                    {/* Mock Resume UI */}
                                    <div className="absolute top-0 left-0 right-0 h-1.5 bg-[#5A639C]" />
                                    <div className="flex items-center gap-4 mb-8">
                                        <div className="w-16 h-16 bg-muted rounded-full" />
                                        <div className="space-y-2">
                                            <div className="w-32 h-6 bg-muted rounded" />
                                            <div className="w-24 h-4 bg-muted/50 rounded" />
                                        </div>
                                    </div>
                                    <div className="space-y-4 flex-1">
                                        <div className="w-full h-4 bg-muted/30 rounded" />
                                        <div className="w-2/3 h-4 bg-muted/30 rounded" />
                                        <div className="w-full h-32 bg-muted/10 rounded mt-4" />
                                        <div className="w-full h-32 bg-muted/10 rounded" />
                                    </div>
                                    <div className="absolute bottom-6 right-6">
                                        <div className="flex items-center gap-2 bg-[#5A639C] text-white px-4 py-2 rounded-full shadow-lg">
                                            <Sparkles className="h-4 w-4" />
                                            <span className="text-sm font-medium">AI 분석 중...</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* CTA Section */}
                    <section className="py-32 px-6 text-center bg-[#5A639C]/5">
                        <div className="max-w-3xl mx-auto space-y-8">
                            <h2 className="text-3xl md:text-4xl font-bold">지금 바로 시작해보세요</h2>
                            <p className="text-xl text-muted-foreground leading-relaxed">
                                서류 통과율 80% 이상의 검증된 이력서 양식으로<br />
                                커리어의 다음 단계를 준비하세요.
                            </p>
                            <Button size="lg" className="h-14 px-12 text-lg font-semibold bg-gradient-to-r from-[#5A639C] to-[#9B86BD] hover:opacity-90 transition-opacity text-white border-0" onClick={handleAction}>
                                무료로 이력서 작성하기
                            </Button>
                        </div>
                    </section>
                </main>
                <Footer />
            </div>
        </div>
    );
}
