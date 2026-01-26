import { ArrowRight, TrendingUp, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { JobCard } from "./JobCard";
import { usePublicJobs } from "@/hooks/useJobs";
import { Link } from "react-router-dom";

// 백업용 목업 데이터 (API 실패 시 사용)
const fallbackJobs = [
  {
    jobId: 1,
    companyName: "테크스타트",
    companyLogoUrl: null,
    title: "시니어 프론트엔드 개발자",
    location: "서울 강남",
    salaryDisplay: "6,000~8,000만",
    stack: "React,TypeScript,Next.js,TailwindCSS",
    createdAt: new Date().toISOString(),
    isAd: true,
    matchInfo: { matchScore: 95 },
  },
  {
    jobId: 2,
    companyName: "디지털웨이브",
    companyLogoUrl: null,
    title: "풀스택 개발자 (Python/React)",
    location: "서울 판교",
    salaryDisplay: "5,000~7,000만",
    stack: "Python,Django,React,PostgreSQL,AWS",
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    matchInfo: { matchScore: 92 },
  },
  {
    jobId: 3,
    companyName: "클라우드팩토리",
    companyLogoUrl: null,
    title: "DevOps 엔지니어",
    location: "서울 성수",
    salaryDisplay: "5,500~7,500만",
    stack: "Kubernetes,Docker,Terraform,CI/CD",
    createdAt: new Date(Date.now() - 172800000).toISOString(),
    matchInfo: { matchScore: 88 },
  },
  {
    jobId: 4,
    companyName: "AI솔루션즈",
    companyLogoUrl: null,
    title: "ML 엔지니어",
    location: "서울 역삼",
    salaryDisplay: "7,000~9,000만",
    stack: "PyTorch,TensorFlow,Python,MLOps",
    createdAt: new Date(Date.now() - 259200000).toISOString(),
  },
  {
    jobId: 5,
    companyName: "핀테크코리아",
    companyLogoUrl: null,
    title: "백엔드 개발자 (Java/Spring)",
    location: "서울 여의도",
    salaryDisplay: "5,500~7,000만",
    stack: "Java,Spring Boot,JPA,MySQL",
    createdAt: new Date(Date.now() - 259200000).toISOString(),
  },
  {
    jobId: 6,
    companyName: "모바일랩스",
    companyLogoUrl: null,
    title: "iOS 개발자",
    location: "서울 강남",
    salaryDisplay: "5,000~6,500만",
    stack: "Swift,SwiftUI,Combine,Core Data",
    createdAt: new Date(Date.now() - 345600000).toISOString(),
  },
];

export function JobListSection() {
  const { data, isLoading, error } = usePublicJobs({ page: 0, size: 6 });

  // API 응답 또는 fallback 데이터 사용
  const jobs = data?.jobs || (error ? fallbackJobs : []);

  return (
    <section className="py-12 lg:py-16">
      <div className="container">
        {/* 섹션 헤더 */}
        <div className="mb-8 flex items-end justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-primary">
              <TrendingUp className="h-5 w-5" />
              <span className="text-sm font-semibold">지금 뜨는 공고</span>
            </div>
            <h2 className="text-2xl font-bold lg:text-3xl">
              당신을 기다리는 기회
            </h2>
          </div>
          <Link to="/jobs">
            <Button variant="ghost" className="hidden sm:flex items-center gap-1 text-muted-foreground hover:text-primary">
              전체보기
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>

        {/* 로딩 상태 */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <span className="ml-2 text-muted-foreground">채용공고를 불러오는 중...</span>
          </div>
        )}

        {/* 에러 상태 (fallback 데이터 표시) */}
        {error && !isLoading && (
          <div className="mb-4 rounded-lg bg-yellow-50 p-3 text-sm text-yellow-800">
            서버 연결에 실패했습니다. 샘플 데이터를 표시합니다.
          </div>
        )}

        {/* 채용 카드 그리드 */}
        {!isLoading && jobs.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {jobs.map((job) => (
              <JobCard 
                key={job.jobId} 
                jobId={job.jobId}
                companyName={job.companyName}
                companyLogoUrl={job.companyLogoUrl}
                title={job.title}
                location={job.location}
                salaryDisplay={job.salaryDisplay}
                stack={job.stack}
                createdAt={job.createdAt}
                isAd={job.adBidCredit > 0}
                matchInfo={job.matchInfo}
              />
            ))}
          </div>
        )}

        {/* 데이터 없음 */}
        {!isLoading && !error && jobs.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <p className="text-lg text-muted-foreground">등록된 채용공고가 없습니다.</p>
            <p className="text-sm text-muted-foreground">곧 새로운 공고가 등록될 예정입니다.</p>
          </div>
        )}

        {/* 모바일 전체보기 버튼 */}
        <div className="mt-8 flex justify-center sm:hidden">
          <Link to="/jobs" className="w-full max-w-xs">
            <Button variant="outline" className="w-full">
              전체 공고 보기
              <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </Link>
        </div>
      </div>
    </section>
  );
}
