import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";
import { Sparkles, ArrowRight, Target, Zap } from "lucide-react";

export function CompanyMarketingBanner() {
    return (
        <div className="w-full bg-gradient-to-r from-slate-900 to-slate-800 rounded-3xl p-8 md:p-12 text-white overflow-hidden relative shadow-xl">
            {/* Background Decorations */}
            <div className="absolute top-0 right-0 w-64 h-64 bg-primary/20 rounded-full blur-3xl -mr-16 -mt-16 pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-48 h-48 bg-blue-500/10 rounded-full blur-3xl -ml-10 -mb-10 pointer-events-none" />

            <div className="relative z-10 grid md:grid-cols-2 gap-8 items-center">
                <div className="space-y-6">
                    <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/20 text-primary text-sm font-medium border border-primary/20">
                        <Sparkles className="w-4 h-4" />
                        <span>기업 회원을 위한 특별한 혜택</span>
                    </div>

                    <h2 className="text-3xl md:text-4xl font-bold leading-tight">
                        FitMe의 <span className="text-primary">AI 인재 매칭</span>으로<br />
                        딱 맞는 개발자를 찾으세요
                    </h2>

                    <p className="text-slate-300 text-lg max-w-lg">
                        공고 등록 한 번으로 10만 명의 인재풀에서 <br className="hidden sm:block" />
                        귀사의 기술 스택과 완벽하게 일치하는 후보자를 추천해드립니다.
                    </p>

                    <div className="flex flex-wrap gap-4 pt-2">
                        <Button size="lg" className="font-bold text-base" asChild>
                            <Link to="/company/dashboard?tab=jobs">
                                채용 공고 등록하기 <ArrowRight className="ml-2 w-5 h-5" />
                            </Link>
                        </Button>
                        <Button size="lg" variant="outline" className="bg-transparent border-white/20 text-white hover:bg-white/10" asChild>
                            <Link to="/talents">
                                인재 풀 검색하기
                            </Link>
                        </Button>
                    </div>
                </div>

                {/* Right Side Stats/Visuals */}
                <div className="hidden md:block relative">
                    <div className="grid grid-cols-2 gap-4">
                        <div className="bg-white/5 backdrop-blur-sm border border-white/10 p-6 rounded-2xl">
                            <Target className="w-8 h-8 text-blue-400 mb-3" />
                            <div className="text-2xl font-bold text-white">98%</div>
                            <div className="text-sm text-slate-400">매칭 정확도</div>
                        </div>
                        <div className="bg-white/5 backdrop-blur-sm border border-white/10 p-6 rounded-2xl transform translate-y-8">
                            <Zap className="w-8 h-8 text-yellow-400 mb-3" />
                            <div className="text-2xl font-bold text-white">2일</div>
                            <div className="text-sm text-slate-400">평균 채용 기간</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
