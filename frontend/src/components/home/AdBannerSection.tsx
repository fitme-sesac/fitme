import { useRef } from "react";
import { Button } from "@/components/ui/button";
import { ChevronLeft, ChevronRight, Sparkles } from "lucide-react";

const BANNERS = [
    {
        id: 1,
        company: "FitMe",
        title: "AI 기반 맞춤형 채용 플랫폼",
        desc: "Frontend Developer (3년 이상)",
        color: "from-blue-600 to-blue-400",
        image: ""
    },
    {
        id: 2,
        company: "TechHub",
        title: "기술의 중심에서 성장하세요",
        desc: "Backend Developer (Server)",
        color: "from-teal-500 to-teal-400",
        image: ""
    },
    {
        id: 3,
        company: "Kakao",
        title: "세상을 바꾸는 기술, 카카오",
        desc: "AI Research Engineer",
        color: "from-yellow-400 to-yellow-300",
        textColor: "text-black",
        image: ""
    }
];

export function AdBannerSection() {
    const scrollRef = useRef<HTMLDivElement>(null);

    const scroll = (direction: "left" | "right") => {
        if (scrollRef.current) {
            const scrollAmount = 400;
            scrollRef.current.scrollBy({
                left: direction === "right" ? scrollAmount : -scrollAmount,
                behavior: "smooth",
            });
        }
    };

    return (
        <div className="relative group mb-8">
            <div className="flex items-center justify-between mb-4 px-1">
                <h2 className="text-xl font-bold flex items-center gap-2">
                    <Sparkles className="h-5 w-5 text-primary" />
                    <span className="bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                        추천 기업
                    </span>
                </h2>
                <div className="flex gap-2">
                    <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8 rounded-full border-border bg-card/50 hover:bg-card"
                        onClick={() => scroll("left")}
                    >
                        <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8 rounded-full border-border bg-card/50 hover:bg-card"
                        onClick={() => scroll("right")}
                    >
                        <ChevronRight className="h-4 w-4" />
                    </Button>
                </div>
            </div>

            <div
                ref={scrollRef}
                className="flex gap-4 overflow-x-auto pb-4 scrollbar-hide snap-x snap-mandatory"
                style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
            >
                {BANNERS.map((banner) => (
                    <div
                        key={banner.id}
                        className={`flex-none w-[300px] md:w-[400px] h-[200px] rounded-2xl relative overflow-hidden snap-center cursor-pointer transition-all duration-300 hover:scale-[1.02] hover:shadow-lg`}
                    >
                        {/* Background Gradient */}
                        <div className={`absolute inset-0 bg-gradient-to-br ${banner.color} opacity-90`} />

                        {/* Content */}
                        <div className="absolute inset-0 p-6 flex flex-col justify-between z-10">
                            <div>
                                <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold bg-white/20 backdrop-blur-md ${banner.textColor || "text-white"} mb-3`}>
                                    {banner.company} Ad
                                </span>
                                <h3 className={`text-2xl font-bold leading-tight ${banner.textColor || "text-white"}`}>
                                    {banner.title}
                                </h3>
                            </div>
                            <div className="flex items-center justify-between">
                                <p className={`text-sm font-medium opacity-90 ${banner.textColor || "text-white"}`}>
                                    {banner.desc}
                                </p>
                                <div className="h-8 w-8 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center">
                                    <ChevronRight className={`h-4 w-4 ${banner.textColor || "text-white"}`} />
                                </div>
                            </div>
                        </div>

                        {/* Decorative Circle */}
                        <div className="absolute -bottom-10 -right-10 w-32 h-32 bg-white/10 rounded-full blur-2xl" />
                    </div>
                ))}
            </div>
        </div>
    );
}
