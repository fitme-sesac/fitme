import { Sparkles, Target, Zap, Shield, TrendingUp, Clock } from "lucide-react";

const features = [
  {
    icon: Sparkles,
    title: "AI 역량 분석",
    description: "이력서와 포트폴리오를 AI가 분석하여 당신의 강점을 파악합니다",
  },
  {
    icon: Target,
    title: "정밀 매칭",
    description: "유사도 0.7 이상의 고품질 매칭만 제안하여 시간을 절약해드립니다",
  },
  {
    icon: Zap,
    title: "실시간 제안",
    description: "기업에서 먼저 당신에게 연락하는 리버스 리크루팅 경험",
  },
  {
    icon: Shield,
    title: "투명한 정보",
    description: "연봉, 기업문화, 면접후기까지 검증된 정보를 제공합니다",
  },
  {
    icon: TrendingUp,
    title: "커리어 코칭",
    description: "AI가 자기소개서 윤문과 예상 면접 질문을 제공합니다",
  },
  {
    icon: Clock,
    title: "지원 현황 트래킹",
    description: "서류부터 최종 합격까지 모든 단계를 실시간으로 관리합니다",
  },
];

export function FeatureSection() {
  return (
    <section className="py-12 lg:py-20">
      <div className="container">
        {/* 섹션 헤더 */}
        <div className="mb-12 text-center">
          <h2 className="mb-4 text-2xl font-bold lg:text-4xl">
            왜 <span className="text-gradient-primary">FitMe</span>인가요?
          </h2>
          <p className="mx-auto max-w-2xl text-muted-foreground lg:text-lg">
            기다리는 구직에서 벗어나세요. FitMe의 AI가 당신에게 맞는 기회를 찾아드립니다.
          </p>
        </div>

        {/* 기능 그리드 */}
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((feature) => {
            return (
              <div
                key={feature.title}
                className="group rounded-2xl bg-card p-6 card-hover border border-border hover:border-primary/50 transition-all duration-300"
              >
                <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 group-hover:bg-primary/20 transition-colors">
                  <feature.icon className="h-6 w-6 text-primary" />
                </div>
                <h3 className="mb-2 text-lg font-semibold group-hover:text-primary transition-colors">
                  {feature.title}
                </h3>
                <p className="text-muted-foreground">
                  {feature.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
