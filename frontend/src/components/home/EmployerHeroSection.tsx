import { Sparkles, Users, Zap, Target, Megaphone } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";
import {
  Carousel,
  CarouselContent,
  CarouselItem,
} from "@/components/ui/carousel";
import Autoplay from "embla-carousel-autoplay";
import { useRef } from "react";

const bannerSlides = [
  {
    id: 1,
    badge: "AI 기반 인재 매칭",
    badgeIcon: Sparkles,
    title: "AI가 추천하는",
    subtitle: "최적의 인재를 만나보세요",
    description: "채용 공고를 등록하고, FitMe AI가 분석한 최적의 인재를 추천받으세요.",
    gradient: "from-primary/5 via-background to-info/5",
    accentColor: "primary",
  },
  {
    id: 2,
    badge: "프리미엄 채용광고",
    badgeIcon: Megaphone,
    title: "상위 노출 광고로",
    subtitle: "지원률 3배 증가",
    description: "프리미엄 광고를 이용하면 더 많은 인재에게 도달할 수 있습니다.",
    gradient: "from-accent/5 via-background to-warning/5",
    accentColor: "accent",
  },
  {
    id: 3,
    badge: "빠른 채용 프로세스",
    badgeIcon: Zap,
    title: "평균 3.2일 만에",
    subtitle: "최적의 인재 채용 완료",
    description: "AI 매칭 시스템으로 채용 기간을 획기적으로 단축하세요.",
    gradient: "from-success/5 via-background to-primary/5",
    accentColor: "success",
  },
  {
    id: 4,
    badge: "15,000+ 인재풀",
    badgeIcon: Users,
    title: "검증된 인재풀에서",
    subtitle: "딱 맞는 인재를 찾으세요",
    description: "다양한 분야의 전문 인재들이 귀사의 연락을 기다리고 있습니다.",
    gradient: "from-info/5 via-background to-accent/5",
    accentColor: "info",
  },
  {
    id: 5,
    badge: "정밀 타겟팅",
    badgeIcon: Target,
    title: "94% 매칭 성공률",
    subtitle: "정확한 인재 추천",
    description: "기술 스택, 경력, 문화 적합도까지 분석하는 AI 매칭 시스템.",
    gradient: "from-warning/5 via-background to-success/5",
    accentColor: "warning",
  },
];

export function EmployerHeroSection() {
  const plugin = useRef(
    Autoplay({ delay: 4000, stopOnInteraction: false })
  );

  return (
    <section className="relative overflow-hidden">
      <Carousel
        plugins={[plugin.current]}
        opts={{ loop: true }}
        className="w-full"
      >
        <CarouselContent>
          {bannerSlides.map((slide) => (
            <CarouselItem key={slide.id}>
              <div className={`relative overflow-hidden bg-gradient-to-br ${slide.gradient} py-10 lg:py-14`}>
                {/* 배경 장식 */}
                <div className="absolute inset-0 overflow-hidden">
                  <div className={`absolute -top-40 -right-40 h-80 w-80 rounded-full bg-${slide.accentColor}/10 blur-3xl`} />
                  <div className={`absolute -bottom-40 -left-40 h-80 w-80 rounded-full bg-info/10 blur-3xl`} />
                </div>

                <div className="container relative">
                  <div className="flex flex-col items-center text-center space-y-5 animate-fade-in">
                    {/* 배지 */}
                    <div className={`inline-flex items-center gap-2 rounded-full bg-${slide.accentColor}/10 px-4 py-2 text-sm font-medium text-${slide.accentColor}`}>
                      <slide.badgeIcon className="h-4 w-4" />
                      <span>{slide.badge}</span>
                    </div>

                    {/* 제목 */}
                    <div className="space-y-2 max-w-2xl">
                      <h1 className="text-2xl font-extrabold tracking-tight lg:text-3xl xl:text-4xl">
                        <span className="text-gradient-hero">{slide.title}</span>
                        <br />
                        {slide.subtitle}
                      </h1>
                      <p className="text-base text-muted-foreground">
                        {slide.description}
                      </p>
                    </div>

                    {/* CTA */}
                    <div className="flex gap-3 pt-2">
                      <Button asChild size="lg" className="btn-gradient-hero">
                        <Link to="/company/dashboard">대시보드 바로가기</Link>
                      </Button>
                      <Button asChild variant="outline" size="lg">
                        <Link to="/jobs/new">채용공고 등록</Link>
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </CarouselItem>
          ))}
        </CarouselContent>
      </Carousel>

      {/* 슬라이드 인디케이터 */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
        {bannerSlides.map((_, idx) => (
          <div
            key={idx}
            className="w-2 h-2 rounded-full bg-primary/30 transition-colors"
          />
        ))}
      </div>
    </section>
  );
}
