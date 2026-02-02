import { ArrowRight, Sparkles, Users, Building2, Zap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";

export function HeroSection() {
  return (
    <section className="relative overflow-visible bg-gradient-to-br from-primary/5 via-background to-info/5 py-16 lg:py-24">
      {/* 배경 장식 */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 h-80 w-80 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute -bottom-40 -left-40 h-80 w-80 rounded-full bg-info/10 blur-3xl" />
      </div>

      <div className="container relative">
        <div className="grid gap-12 lg:grid-cols-2 lg:items-center">
          {/* 좌측 콘텐츠 */}
          <div className="space-y-8 animate-slide-up">
            {/* 배지 */}
            <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-2 text-sm font-medium text-primary">
              <Sparkles className="h-4 w-4" />
              <span>AI 기반 리버스 리크루팅</span>
            </div>

            {/* 제목 */}
            <div className="space-y-4">
              <h1 className="text-4xl font-extrabold tracking-tight lg:text-5xl xl:text-6xl text-slate-900">
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-sky-500 via-sky-400 to-teal-400">AI가 찾아주는</span>
                <br />
                나에게 딱 맞는 기회
              </h1>
              <p className="text-lg text-slate-500 lg:text-xl max-w-lg">
                기다리지 마세요. FitMe의 AI가 당신의 역량을 분석하고,
                가장 적합한 기업을 먼저 제안해드립니다.
              </p>
            </div>

            {/* CTA 버튼 */}
            <div className="flex flex-wrap gap-4">
              <Button asChild size="lg" className="h-14 px-8 text-base bg-gradient-to-r from-sky-500 via-sky-400 to-teal-400 hover:from-teal-400 hover:to-sky-500 text-white shadow-lg shadow-sky-200/50 border-none">
                <Link to="/auth?tab=signup">
                  지금 시작하기
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Link>
              </Button>
              <Button asChild size="lg" variant="outline" className="h-14 px-8 text-base border-2 hover:bg-slate-50 hover:text-sky-600 hover:border-sky-200">
                <Link to="/auth?tab=signup&type=company">기업 서비스 알아보기</Link>
              </Button>
            </div>

            {/* 통계 */}
            <div className="flex flex-wrap gap-8 pt-4">
              <div className="space-y-1">
                <p className="text-3xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-sky-500 to-teal-400">15,000+</p>
                <p className="text-sm text-slate-500">등록된 개발자</p>
              </div>
              <div className="space-y-1">
                <p className="text-3xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-sky-500 to-teal-400">2,500+</p>
                <p className="text-sm text-slate-500">파트너 기업</p>
              </div>
              <div className="space-y-1">
                <p className="text-3xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-sky-500 to-teal-400">98%</p>
                <p className="text-sm text-slate-500">매칭 만족도</p>
              </div>
            </div>
          </div>

          {/* 우측 일러스트 카드 */}
          <div className="relative hidden lg:block">
            <div className="relative">
              {/* 메인 카드 */}
              <div className="rounded-2xl bg-card p-6 shadow-lg card-hover">
                <div className="mb-4 flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
                    <Zap className="h-6 w-6 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-semibold">AI 매칭 완료</h3>
                    <p className="text-sm text-muted-foreground">3개의 새로운 제안</p>
                  </div>
                </div>

                {/* 매칭 결과 미리보기 */}
                <div className="space-y-3">
                  {[
                    { company: "테크스타트", match: 95, role: "프론트엔드 개발자" },
                    { company: "디지털웨이브", match: 92, role: "풀스택 개발자" },
                    { company: "클라우드팩토리", match: 88, role: "백엔드 개발자" },
                  ].map((item, i) => (
                    <div key={i} className="flex items-center justify-between rounded-xl bg-[#EAEFEF] p-3">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-sm font-bold text-primary">
                          {item.company[0]}
                        </div>
                        <div>
                          <p className="font-medium text-sm">{item.company}</p>
                          <p className="text-xs text-muted-foreground">{item.role}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-1 rounded-full bg-success/10 px-2 py-1 text-xs font-semibold text-success">
                        {item.match}% 적합
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 플로팅 카드들 */}
              <div className="absolute -top-4 -right-4 rounded-xl bg-card p-4 shadow-lg animate-float">
                <div className="flex items-center gap-2">
                  <Users className="h-5 w-5 text-primary" />
                  <span className="text-sm font-medium">342명 지원 중</span>
                </div>
              </div>

              <div className="absolute -bottom-4 -left-4 rounded-xl bg-card p-4 shadow-lg animate-float" style={{ animationDelay: "2s" }}>
                <div className="flex items-center gap-2">
                  <Building2 className="h-5 w-5 text-accent" />
                  <span className="text-sm font-medium">+127개 신규 공고</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
