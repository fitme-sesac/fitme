import { ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { getPublicEmployers } from "@/api/employers";

const fallbackCompanies = [
  { id: 1, name: "테크스타트", logo: "T", category: "IT/소프트웨어", jobs: 12 },
  { id: 2, name: "디지털웨이브", logo: "D", category: "AI/데이터", jobs: 8 },
  { id: 3, name: "클라우드팩토리", logo: "C", category: "클라우드", jobs: 15 },
  { id: 4, name: "AI솔루션즈", logo: "A", category: "AI/ML", jobs: 6 },
  { id: 5, name: "핀테크코리아", logo: "F", category: "금융/핀테크", jobs: 10 },
  { id: 6, name: "모바일랩스", logo: "M", category: "모바일", jobs: 7 },
  { id: 7, name: "커머스허브", logo: "K", category: "이커머스", jobs: 9 },
  { id: 8, name: "헬스테크", logo: "H", category: "헬스케어", jobs: 5 },
];

export function CompanySection() {
  const [companies, setCompanies] = useState(fallbackCompanies);

  useEffect(() => {
    const fetchCompanies = async () => {
      try {
        const res = await getPublicEmployers({ page: 0, size: 12 });
        const list = res?.content ?? res?.employers ?? (Array.isArray(res) ? res : []);
        if (list.length > 0) {
          setCompanies(
            list.map((e: any) => ({
              id: e.employerId ?? e.id,
              name: e.name ?? "기업",
              logo: (e.name ?? "C").charAt(0).toUpperCase(),
              category: e.industry ?? e.category ?? "-",
              jobs: e.openJobCount ?? e.jobs ?? 0,
            }))
          );
        }
      } catch {
        // API 미제공 시 fallback 유지 (광고 크레딧 순 API 연동 전)
      }
    };
    fetchCompanies();
  }, []);
  const [scrollPosition, setScrollPosition] = useState(0);

  const scroll = (direction: "left" | "right") => {
    const container = document.getElementById("company-scroll");
    if (container) {
      const scrollAmount = direction === "left" ? -300 : 300;
      container.scrollBy({ left: scrollAmount, behavior: "smooth" });
      setScrollPosition(container.scrollLeft + scrollAmount);
    }
  };

  return (
    <section className="py-12 lg:py-16 bg-secondary/30">
      <div className="container">
        {/* 섹션 헤더 */}
        <div className="mb-8 flex items-end justify-between">
          <div className="space-y-2">
            <h2 className="text-2xl font-bold lg:text-3xl">
              함께하는 파트너 기업
            </h2>
            <p className="text-muted-foreground">
              광고 크레딧을 많이 사용한 우수 기업 · 2,500+ 기업이 FitMe에서 인재를 찾고 있습니다
            </p>
          </div>
          <div className="hidden sm:flex items-center gap-2">
            <Button
              variant="outline"
              size="icon"
              className="h-10 w-10 rounded-full"
              onClick={() => scroll("left")}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="icon"
              className="h-10 w-10 rounded-full"
              onClick={() => scroll("right")}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* 기업 카드 슬라이더 */}
        <div
          id="company-scroll"
          className="flex gap-4 overflow-x-auto pt-4 pb-4 scrollbar-hide scroll-smooth snap-x snap-mandatory overflow-y-visible"
          style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
        >
          {companies.map((company) => (
            <Link
              key={company.id}
              to={`/companies/${company.id}`}
              className="group flex-shrink-0 w-64 snap-start rounded-2xl bg-card p-5 card-hover border cursor-pointer block"
            >
              <div className="mb-4 flex items-center gap-3">
                <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-xl font-bold text-primary">
                  {company.logo}
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold truncate group-hover:text-primary transition-colors">
                    {company.name}
                  </h3>
                  <p className="text-sm text-muted-foreground">{company.category}</p>
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">
                  채용 중 <span className="font-semibold text-primary">{company.jobs}개</span>
                </span>
                <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
              </div>
            </Link>
          ))}
        </div>

        {/* 기업 등록 CTA */}
        <div className="mt-8 rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-info/10 p-6 lg:p-8">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div>
              <h3 className="text-lg font-semibold mb-1">기업 회원이신가요?</h3>
              <p className="text-muted-foreground">
                AI 기반 인재 매칭으로 최적의 개발자를 만나보세요
              </p>
            </div>
            <Button asChild className="btn-gradient-primary whitespace-nowrap">
              <Link to="/auth?tab=signup&type=company">
                기업 서비스 시작하기
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}
