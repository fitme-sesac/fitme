import { useRef, useState, useEffect } from "react";
import { Sparkles, Users, Zap, Target, Megaphone, Search, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
  type CarouselApi,
} from "@/components/ui/carousel";
import Autoplay from "embla-carousel-autoplay";

const bannerSlides = [
  {
    id: 1,
    badge: "AI 기반 인재 매칭",
    badgeIcon: Sparkles,
    title: "AI가 추천하는",
    subtitle: "최적의 인재를 만나보세요",
    description: "FitMe AI가 분석한 상위 1% 인재를 실시간으로 추천받고 채용 성공률을 높이세요.",
    gradient: "from-blue-50 via-white to-blue-50",
    badgeClass: "bg-white border-blue-200 text-blue-600",
    titleClass: "text-slate-900",
    subtitleClass: "text-blue-600",
    indicatorClass: "bg-blue-600",
    ctaText: "인재 보러가기",
    ctaLink: "/talents",
  },
  {
    id: 2,
    badge: "프리미엄 채용광고",
    badgeIcon: Megaphone,
    title: "상위 노출 광고로",
    subtitle: "지원율을 3배 높이세요",
    description: "프리미엄 채용관과 상단 노출 광고로 더 많은 핵심 인재에게 기업을 홍보하세요.",
    gradient: "from-emerald-50 via-white to-emerald-50",
    badgeClass: "bg-white border-emerald-200 text-emerald-600",
    titleClass: "text-slate-900",
    subtitleClass: "text-emerald-600",
    indicatorClass: "bg-emerald-600",
    ctaText: "광고 상품 안내",
    ctaLink: "/products",
  },
  {
    id: 3,
    badge: "빠른 채용 프로세스",
    badgeIcon: Zap,
    title: "평균 3.2일 만에",
    subtitle: "최적의 인재 채용 완료",
    description: "복잡한 서류 검토 없이 AI 매칭 시스템으로 채용 기간을 획기적으로 단축하세요.",
    gradient: "from-amber-50 via-white to-amber-50",
    badgeClass: "bg-white border-amber-200 text-amber-600",
    titleClass: "text-slate-900",
    subtitleClass: "text-amber-600",
    indicatorClass: "bg-amber-500",
    ctaText: "공고 등록하기",
    ctaLink: "/employer/jobs/create",
  },
  {
    id: 4,
    badge: "15,000+ 인재풀",
    badgeIcon: Users,
    title: "검증된 개발 인재풀",
    subtitle: "딱 맞는 인재를 찾으세요",
    description: "다양한 기술 스택을 보유한 검증된 시니어 개발자들이 귀사의 제안을 기다리고 있습니다.",
    gradient: "from-red-50 via-white to-red-50",
    badgeClass: "bg-white border-red-200 text-red-600",
    titleClass: "text-slate-900",
    subtitleClass: "text-red-600",
    indicatorClass: "bg-red-600",
    ctaText: "인재 검색하기",
    ctaLink: "/talents",
  },
  {
    id: 5,
    badge: "정밀 타켓팅",
    badgeIcon: Target,
    title: "94% 매칭 성공률",
    subtitle: "신뢰할 수 있는 데이터",
    description: "기술 스택뿐만 아니라 직무 적합도와 문화 적합도까지 정밀하게 분석합니다.",
    gradient: "from-purple-50 via-white to-purple-50",
    badgeClass: "bg-white border-purple-200 text-purple-600",
    titleClass: "text-slate-900",
    subtitleClass: "text-purple-600",
    indicatorClass: "bg-purple-600",
    ctaText: "매칭 서비스 시작",
    ctaLink: "/company/dashboard",
  },
];

export function EmployerHeroSection() {
  const plugin = useRef(
    Autoplay({ delay: 5000, stopOnInteraction: false })
  );
  const [api, setApi] = useState<CarouselApi>();
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    if (!api) {
      return;
    }

    setCurrent(api.selectedScrollSnap());

    api.on("select", () => {
      setCurrent(api.selectedScrollSnap());
    });
  }, [api]);

  return (
    <section className="relative overflow-hidden mb-8 group">
      <Carousel
        plugins={[plugin.current]}
        setApi={setApi}
        opts={{ loop: true }}
        className="w-full"
      >
        <CarouselContent>
          {bannerSlides.map((slide) => (
            <CarouselItem key={slide.id}>
              <div className={`relative overflow-hidden bg-gradient-to-br ${slide.gradient} py-12 lg:py-20 border-b border-border/50`}>
                {/* 배경 장식 */}
                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                  <div className={`absolute -top-40 -right-40 h-80 w-80 rounded-full opacity-20 blur-3xl ${slide.indicatorClass.replace('bg-', 'bg-')}`} />
                  <div className={`absolute -bottom-40 -left-40 h-80 w-80 rounded-full opacity-20 blur-3xl ${slide.indicatorClass.replace('bg-', 'bg-')}`} />
                </div>

                <div className="container relative">
                  <div className="flex flex-col items-center text-center space-y-6 animate-fade-in">
                    {/* 배지 */}
                    <div className={`inline-flex items-center gap-2 rounded-full shadow-sm border px-4 py-2 text-sm font-bold ${slide.badgeClass}`}>
                      <slide.badgeIcon className="h-4 w-4" />
                      <span>{slide.badge}</span>
                    </div>

                    {/* 제목 */}
                    <div className="space-y-3 max-w-2xl">
                      <h1 className="text-3xl lg:text-5xl font-extrabold tracking-tight leading-tight">
                        <span className={`${slide.titleClass}`}>{slide.title}</span>
                        <br className="hidden sm:block" />
                        <span className={`${slide.subtitleClass} mt-2 inline-block`}>{slide.subtitle}</span>
                      </h1>
                      <p className="text-lg text-slate-700 font-medium max-w-xl mx-auto leading-relaxed mt-4">
                        {slide.description}
                      </p>
                    </div>

                    {/* CTA - Brand Gradient Applied */}
                    <div className="flex flex-wrap justify-center gap-3 pt-6">
                      <Button asChild size="lg" className="h-12 px-8 text-base font-bold border-none shadow-lg text-white shadow-blue-200/50 transition-all hover:scale-[1.02]" style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }}>
                        <Link to={slide.ctaLink}>
                          {slide.ctaText}
                        </Link>
                      </Button>
                      <Button asChild variant="outline" size="lg" className="h-12 px-8 text-base font-medium bg-white hover:bg-slate-50 border-slate-200">
                        <Link to="/talents">
                          <Search className="mr-2 h-4 w-4" />
                          인재 검색
                        </Link>
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </CarouselItem>
          ))}
        </CarouselContent>

        {/* 좌우 이동 버튼 */}
        <CarouselPrevious className="hidden lg:flex left-4 lg:left-8 bg-white/80 border-0 shadow-md hover:bg-white text-slate-900" />
        <CarouselNext className="hidden lg:flex right-4 lg:right-8 bg-white/80 border-0 shadow-md hover:bg-white text-slate-900" />
      </Carousel>

      {/* 슬라이드 인디케이터 (네비게이션 닷) */}
      <div className="absolute bottom-6 left-1/2 -translate-x-1/2 flex gap-3 z-10 px-4 py-2 rounded-full bg-white/40 backdrop-blur-md border border-white/20 shadow-sm hover:bg-white/60 transition-colors">
        {bannerSlides.map((slide, idx) => (
          <button
            key={slide.id}
            onClick={() => api?.scrollTo(idx)}
            className={`transition-all duration-300 rounded-full ${current === idx
              ? `${slide.indicatorClass} w-8 h-2.5 shadow-sm`
              : `bg-slate-400 hover:bg-slate-600 w-2.5 h-2.5`
              }`}
            aria-label={`Go to slide ${idx + 1}`}
          />
        ))}
      </div>
    </section>
  );
}
