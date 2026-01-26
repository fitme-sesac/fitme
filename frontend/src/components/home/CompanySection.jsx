import { ArrowRight, ChevronLeft, ChevronRight, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useState } from "react";
import { usePublicEmployers } from "@/hooks/useEmployers";
import { Link } from "react-router-dom";

// 백업용 목업 데이터 (API 실패 시 사용)
const fallbackCompanies = [
  { employerId: 1, name: "테크스타트", logoUrl: null, industry: "IT/소프트웨어", openJobCount: 12 },
  { employerId: 2, name: "디지털웨이브", logoUrl: null, industry: "AI/데이터", openJobCount: 8 },
  { employerId: 3, name: "클라우드팩토리", logoUrl: null, industry: "클라우드", openJobCount: 15 },
  { employerId: 4, name: "AI솔루션즈", logoUrl: null, industry: "AI/ML", openJobCount: 6 },
  { employerId: 5, name: "핀테크코리아", logoUrl: null, industry: "금융/핀테크", openJobCount: 10 },
  { employerId: 6, name: "모바일랩스", logoUrl: null, industry: "모바일", openJobCount: 7 },
  { employerId: 7, name: "커머스허브", logoUrl: null, industry: "이커머스", openJobCount: 9 },
  { employerId: 8, name: "헬스테크", logoUrl: null, industry: "헬스케어", openJobCount: 5 },
];

export function CompanySection() {
  const [scrollPosition, setScrollPosition] = useState(0);
  const { data, isLoading, error } = usePublicEmployers({ page: 0, size: 10 });

  // API 응답 또는 fallback 데이터 사용
  const companies = data?.employers || (error ? fallbackCompanies : []);
  const totalCompanies = data?.totalElements || (error ? fallbackCompanies.length : 0);

  const scroll = (direction) => {
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
              {totalCompanies > 0 
                ? `${totalCompanies.toLocaleString()}+ 기업이 FitMe에서 인재를 찾고 있습니다`
                : "최고의 기업들이 FitMe에서 인재를 찾고 있습니다"
              }
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

        {/* 로딩 상태 */}
        {isLoading && (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
            <span className="ml-2 text-muted-foreground">기업 정보를 불러오는 중...</span>
          </div>
        )}

        {/* 에러 상태 (fallback 데이터 표시) */}
        {error && !isLoading && (
          <div className="mb-4 rounded-lg bg-yellow-50 p-3 text-sm text-yellow-800">
            서버 연결에 실패했습니다. 샘플 데이터를 표시합니다.
          </div>
        )}

        {/* 기업 카드 슬라이더 */}
        {!isLoading && companies.length > 0 && (
          <div
            id="company-scroll"
            className="flex gap-4 overflow-x-auto pb-4 scrollbar-hide scroll-smooth snap-x snap-mandatory"
            style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
          >
            {companies.map((company) => (
              <Link key={company.employerId} to={`/companies/${company.employerId}`}>
                <div className="group flex-shrink-0 w-64 snap-start rounded-2xl bg-card p-5 card-hover border cursor-pointer">
                  <div className="mb-4 flex items-center gap-3">
                    {company.logoUrl ? (
                      <img 
                        src={company.logoUrl} 
                        alt={company.name}
                        className="h-14 w-14 rounded-xl object-cover"
                      />
                    ) : (
                      <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-xl font-bold text-primary">
                        {company.name?.charAt(0) || "?"}
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold truncate group-hover:text-primary transition-colors">
                        {company.name}
                      </h3>
                      <p className="text-sm text-muted-foreground">{company.industry || "기타"}</p>
                    </div>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-muted-foreground">
                      채용 중 <span className="font-semibold text-primary">{company.openJobCount || 0}개</span>
                    </span>
                    <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}

        {/* 데이터 없음 */}
        {!isLoading && !error && companies.length === 0 && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <p className="text-muted-foreground">등록된 기업이 없습니다.</p>
          </div>
        )}

        {/* 기업 등록 CTA */}
        <div className="mt-8 rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-info/10 p-6 lg:p-8">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div>
              <h3 className="text-lg font-semibold mb-1">기업 회원이신가요?</h3>
              <p className="text-muted-foreground">
                AI 기반 인재 매칭으로 최적의 개발자를 만나보세요
              </p>
            </div>
            <Link to="/auth?type=employer">
              <Button className="btn-gradient-primary whitespace-nowrap">
                기업 서비스 시작하기
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
