import { useEffect, useState } from "react";
import { ArrowRight, TrendingUp, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { JobCard } from "./JobCard";
import { getPublicJobs } from "@/api/jobs";
import { Link } from "react-router-dom";

// API 응답을 JobCard props 형식으로 변환
const mapJobToCardProps = (job: any) => ({
  id: job.jobId || job.id,
  company: job.employer?.name || job.companyName || "회사명",
  logo: (job.employer?.name || "C").charAt(0).toUpperCase(),
  logoUrl: job.employer?.logoUrl,
  title: job.title,
  location: job.location || "위치 미정",
  salary: job.salaryText || "협의",
  skills: job.stack || [],
  postedAt: getRelativeTime(job.createdAt),
  isAd: (job.adBidCredit || 0) > 0,
  matchScore: job.matchScore,
});

// 상대 시간 계산
const getRelativeTime = (dateStr: string) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return "오늘";
  if (diffDays === 1) return "1일 전";
  if (diffDays < 7) return `${diffDays}일 전`;
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전`;
  return `${Math.floor(diffDays / 30)}달 전`;
};

export function JobListSection() {
  const [jobs, setJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchJobs = async () => {
      try {
        const response = await getPublicJobs({ page: 0, size: 6 });
        const jobsData = Array.isArray(response)
          ? response
          : response?.jobs ?? response?.content ?? [];
        setJobs(jobsData.map(mapJobToCardProps));
      } catch (error) {
        console.error("Failed to fetch jobs:", error);
        setJobs([]);
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();
  }, []);

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
          <Button asChild variant="ghost" className="hidden sm:flex items-center gap-1 text-muted-foreground hover:text-primary">
            <Link to="/jobs">
              전체보기
              <ArrowRight className="h-4 w-4" />
            </Link>
          </Button>
        </div>

        {/* 로딩 상태 */}
        {loading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
          </div>
        ) : jobs.length === 0 ? (
          <div className="text-center py-12 text-muted-foreground">
            현재 등록된 채용공고가 없습니다.
          </div>
        ) : (
          /* 채용 카드 그리드 */
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {jobs.map((job) => (
              <JobCard key={job.id} {...job} />
            ))}
          </div>
        )}

        {/* 모바일 전체보기 버튼 */}
        <div className="mt-8 flex justify-center sm:hidden">
          <Button asChild variant="outline" className="w-full max-w-xs">
            <Link to="/jobs">
              전체 공고 보기
              <ArrowRight className="ml-2 h-4 w-4" />
            </Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
