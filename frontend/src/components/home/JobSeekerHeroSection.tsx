import { useRef, useState, useEffect } from "react";
import { Sparkles, Building2, Zap, Rocket, Search, Utensils } from "lucide-react";
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
        badge: "Toss",
        badgeIcon: Rocket,
        title: "금융의 모든 순간을",
        subtitle: "새롭게 정의합니다",
        description: "2,000만 사용자가 선택한 금융 슈퍼앱 토스에서, 금융의 불편함을 해결할 동료를 찾습니다.",
        gradient: "from-blue-50 via-white to-blue-50",
        badgeClass: "bg-white border-blue-200 text-blue-600",
        titleClass: "text-slate-900",
        subtitleClass: "text-blue-600",
        indicatorClass: "bg-blue-600",
        ctaText: "토스 채용 확인하기",
        ctaLink: "/jobs/toss",
    },
    {
        id: 2,
        badge: "NAVER",
        badgeIcon: Search,
        title: "기술로 세상을",
        subtitle: "연결하는 네이버",
        description: "글로벌 테크 기업 네이버에서 AI, 검색, 쇼핑 등 다양한 영역의 혁신을 주도할 인재를 모십니다.",
        gradient: "from-green-50 via-white to-green-50",
        badgeClass: "bg-white border-green-200 text-green-600",
        titleClass: "text-slate-900",
        subtitleClass: "text-green-600",
        indicatorClass: "bg-green-600",
        ctaText: "네이버 공고 보기",
        ctaLink: "/jobs/naver",
    },
    {
        id: 3,
        badge: "Kakao",
        badgeIcon: Sparkles,
        title: "사람과 세상",
        subtitle: "그 이상을 연결하다",
        description: "전 국민의 일상을 책임지는 카카오와 함께, 더 나은 세상을 만들어갈 크루를 기다립니다.",
        gradient: "from-yellow-50 via-white to-yellow-50",
        badgeClass: "bg-white border-amber-200 text-amber-600",
        titleClass: "text-slate-900",
        subtitleClass: "text-amber-600",
        indicatorClass: "bg-amber-500",
        ctaText: "카카오 영입 안내",
        ctaLink: "/jobs/kakao",
    },
    {
        id: 4,
        badge: "Coupang",
        badgeIcon: Zap,
        title: "커머스의 미래",
        subtitle: "우리가 만듭니다",
        description: "압도적인 기술력과 데이터로 고객의 삶을 혁신하는 쿠팡의 여정에 합류하세요.",
        gradient: "from-red-50 via-white to-red-50",
        badgeClass: "bg-white border-red-200 text-red-600",
        titleClass: "text-slate-900",
        subtitleClass: "text-red-600",
        indicatorClass: "bg-red-600",
        ctaText: "쿠팡 채용 바로가기",
        ctaLink: "/jobs/coupang",
    },
    {
        id: 5,
        badge: "Woowa Bros",
        badgeIcon: Utensils,
        title: "문 앞으로 배달되는",
        subtitle: "일상의 행복",
        description: "배달의민족을 통해 푸드테크의 혁신을 만들어가는 우아한형제들과 함께하세요.",
        gradient: "from-teal-50 via-white to-teal-50",
        badgeClass: "bg-white border-teal-200 text-teal-600",
        titleClass: "text-slate-900",
        subtitleClass: "text-teal-600",
        indicatorClass: "bg-teal-600",
        ctaText: "우아한형제들 채용",
        ctaLink: "/jobs/woowa",
    },
];

export function JobSeekerHeroSection() {
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
                                {/* 배경 장식 - 더 진하게 변경 */}
                                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                                    <div className={`absolute -top-40 -right-40 h-80 w-80 rounded-full opacity-20 blur-3xl ${slide.indicatorClass.replace('bg-', 'bg-')}`} />
                                    <div className={`absolute -bottom-40 -left-40 h-80 w-80 rounded-full opacity-20 blur-3xl ${slide.indicatorClass.replace('bg-', 'bg-')}`} />
                                </div>

                                <div className="container relative">
                                    <div className="flex flex-col items-center text-center space-y-6 animate-fade-in">
                                        {/* 배지 - 가독성 개선 */}
                                        <div className={`inline-flex items-center gap-2 rounded-full shadow-sm border px-4 py-2 text-sm font-bold ${slide.badgeClass}`}>
                                            <slide.badgeIcon className="h-4 w-4" />
                                            <span>{slide.badge}</span>
                                        </div>

                                        {/* 제목 - 솔리드 컬러와 브랜드 컬러 사용 */}
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

                                        {/* CTA - Brand Gradient Applied HARDCODED */}
                                        <div className="flex flex-wrap justify-center gap-3 pt-6">
                                            <Button asChild size="lg" className="h-12 px-8 text-base font-bold border-none shadow-lg bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 text-white shadow-blue-200/50">
                                                <Link to={slide.ctaLink}>
                                                    {slide.ctaText}
                                                </Link>
                                            </Button>
                                            <Button asChild variant="outline" size="lg" className="h-12 px-8 text-base font-medium bg-white hover:bg-slate-50 border-slate-200">
                                                <Link to="/jobs">
                                                    <Search className="mr-2 h-4 w-4" />
                                                    공고 검색
                                                </Link>
                                            </Button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </CarouselItem>
                    ))}
                </CarouselContent>

                {/* 좌우 이동 버튼 (호버 시 표시) */}
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
