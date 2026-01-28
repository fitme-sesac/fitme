import { ArrowRight, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";

export function CTASection() {
  return (
    <section className="py-12 lg:py-20">
      <div className="container">
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-[#5A639C] via-[#7776B3] to-[#9B86BD] p-8 lg:p-16">
          {/* 배경 장식 */}
          <div className="absolute inset-0 overflow-hidden">
            <div className="absolute -top-20 -right-20 h-60 w-60 rounded-full bg-white/10 blur-3xl" />
            <div className="absolute -bottom-20 -left-20 h-60 w-60 rounded-full bg-[#E2BBE9]/20 blur-3xl" />
          </div>

          <div className="relative text-center">
            <div className="mb-6 inline-flex items-center gap-2 rounded-full bg-white/20 px-4 py-2 text-sm font-medium text-white">
              <Sparkles className="h-4 w-4" />
              <span>15,000+ 개발자가 함께하고 있습니다</span>
            </div>

            <h2 className="mb-4 text-3xl font-bold text-white lg:text-5xl">
              지금 바로 시작하세요
            </h2>
            <p className="mx-auto mb-8 max-w-2xl text-lg text-white/80">
              3분이면 프로필 등록 완료. AI가 분석을 시작하고,
              <br className="hidden sm:block" />
              24시간 내에 첫 번째 기업 제안을 받아보세요.
            </p>

            <div className="flex flex-col sm:flex-row justify-center gap-4">
              <Button
                size="lg"
                className="h-14 bg-white px-8 text-[#5A639C] font-semibold hover:bg-white/90"
              >
                무료로 시작하기
                <ArrowRight className="ml-2 h-5 w-5" />
              </Button>
              <Button
                size="lg"
                className="h-14 bg-white px-8 text-[#5A639C] font-semibold hover:bg-white/90"
              >
                서비스 둘러보기
              </Button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

