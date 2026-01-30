import { useParams, Link } from "react-router-dom";
import { 
  ArrowLeft, MapPin, Banknote, Clock, Building2, Users, 
  Briefcase, Globe, Mail, Phone, Heart, Share2, Loader2, Sparkles 
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { usePublicJob } from "@/hooks/useJobs";
import { useAuth } from "@/contexts/AuthContext";

export default function JobDetail() {
  const { jobId } = useParams();
  const { data: job, isLoading, error } = usePublicJob(jobId);
  const { isAuthenticated } = useAuth();

  // 스킬 배열 처리
  const skills = job?.stack 
    ? (Array.isArray(job.stack) ? job.stack : job.stack.split(",").map(s => s.trim()).filter(Boolean))
    : [];

  // 등록일 포맷팅
  const formatDate = (dateStr) => {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <Sidebar />
        <div className="lg:pl-64">
          <Header />
          <main className="container py-20">
            <div className="flex items-center justify-center">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <span className="ml-2 text-muted-foreground">채용공고를 불러오는 중...</span>
            </div>
          </main>
        </div>
      </div>
    );
  }

  if (error || !job) {
    return (
      <div className="min-h-screen bg-background">
        <Sidebar />
        <div className="lg:pl-64">
          <Header />
          <main className="container py-20">
            <div className="text-center">
              <h2 className="text-2xl font-bold mb-2">채용공고를 찾을 수 없습니다</h2>
              <p className="text-muted-foreground mb-4">
                요청하신 채용공고가 존재하지 않거나 삭제되었습니다.
              </p>
              <Link to="/jobs">
                <Button>
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  목록으로 돌아가기
                </Button>
              </Link>
            </div>
          </main>
          <Footer />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      
      <div className="lg:pl-64">
        <Header />
        
        <main className="container py-8">
          {/* 뒤로가기 */}
          <Link to="/jobs" className="inline-flex items-center text-muted-foreground hover:text-foreground mb-6">
            <ArrowLeft className="mr-2 h-4 w-4" />
            목록으로
          </Link>

          <div className="grid gap-8 lg:grid-cols-3">
            {/* 메인 콘텐츠 */}
            <div className="lg:col-span-2 space-y-6">
              {/* 헤더 */}
              <div className="rounded-2xl bg-card p-6 border">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-4">
                    {job.companyLogoUrl ? (
                      <img 
                        src={job.companyLogoUrl} 
                        alt={job.companyName}
                        className="h-16 w-16 rounded-xl object-cover"
                      />
                    ) : (
                      <div className="flex h-16 w-16 items-center justify-center rounded-xl bg-primary/10 text-2xl font-bold text-primary">
                        {job.companyName?.charAt(0) || "?"}
                      </div>
                    )}
                    <div>
                      <Link 
                        to={`/companies/${job.employerId}`}
                        className="text-muted-foreground hover:text-primary transition-colors"
                      >
                        {job.companyName}
                      </Link>
                      <h1 className="text-2xl font-bold mt-1">{job.title}</h1>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="ghost" size="icon">
                      <Heart className="h-5 w-5" />
                    </Button>
                    <Button variant="ghost" size="icon">
                      <Share2 className="h-5 w-5" />
                    </Button>
                  </div>
                </div>

                {/* 기본 정보 */}
                <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                  {job.location && (
                    <div className="flex items-center gap-1">
                      <MapPin className="h-4 w-4" />
                      <span>{job.location}</span>
                    </div>
                  )}
                  {job.salaryDisplay && (
                    <div className="flex items-center gap-1">
                      <Banknote className="h-4 w-4" />
                      <span>{job.salaryDisplay}</span>
                    </div>
                  )}
                  <div className="flex items-center gap-1">
                    <Clock className="h-4 w-4" />
                    <span>{formatDate(job.createdAt)}</span>
                  </div>
                </div>

                {/* AI 매칭 스코어 */}
                {job.matchInfo?.matchScore && (
                  <div className="mt-4 flex items-center gap-2 rounded-lg bg-primary/5 px-4 py-3">
                    <Sparkles className="h-5 w-5 text-primary" />
                    <span className="font-medium text-primary">
                      AI 매칭률 {job.matchInfo.matchScore}%
                    </span>
                    {job.matchInfo.matchedSkills?.length > 0 && (
                      <span className="text-sm text-muted-foreground">
                        · {job.matchInfo.matchedSkills.join(", ")} 일치
                      </span>
                    )}
                  </div>
                )}

                {/* 기술 스택 */}
                {skills.length > 0 && (
                  <div className="mt-4 flex flex-wrap gap-2">
                    {skills.map((skill) => (
                      <Badge key={skill} variant="secondary">
                        {skill}
                      </Badge>
                    ))}
                  </div>
                )}
              </div>

              {/* 상세 설명 */}
              <div className="rounded-2xl bg-card p-6 border">
                <h2 className="text-lg font-semibold mb-4">상세 내용</h2>
                <div className="prose prose-sm max-w-none">
                  {job.description ? (
                    <div dangerouslySetInnerHTML={{ __html: job.description.replace(/\n/g, '<br>') }} />
                  ) : (
                    <p className="text-muted-foreground">상세 내용이 없습니다.</p>
                  )}
                </div>

                {/* AI 요약 */}
                {job.summary && (
                  <>
                    <Separator className="my-6" />
                    <div className="rounded-lg bg-secondary/50 p-4">
                      <div className="flex items-center gap-2 mb-2">
                        <Sparkles className="h-4 w-4 text-primary" />
                        <span className="font-medium text-sm">AI 요약</span>
                      </div>
                      <p className="text-sm text-muted-foreground">{job.summary}</p>
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* 사이드바 */}
            <div className="space-y-6">
              {/* 지원하기 카드 */}
              <div className="rounded-2xl bg-card p-6 border sticky top-24">
                <h3 className="font-semibold mb-4">이 공고에 지원하기</h3>
                
                {isAuthenticated ? (
                  <Link to={`/jobs/${jobId}/apply`}>
                    <Button className="w-full btn-gradient-primary" size="lg">
                      지원하기
                    </Button>
                  </Link>
                ) : (
                  <div className="space-y-3">
                    <Link to="/auth">
                      <Button className="w-full" size="lg">
                        로그인하고 지원하기
                      </Button>
                    </Link>
                    <p className="text-xs text-center text-muted-foreground">
                      로그인하면 AI 매칭 분석도 확인할 수 있습니다
                    </p>
                  </div>
                )}

                <Separator className="my-4" />

                <div className="space-y-3 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">조회수</span>
                    <span>{job.viewCount?.toLocaleString() || 0}회</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">지원자</span>
                    <span>{job.applicationCount?.toLocaleString() || 0}명</span>
                  </div>
                </div>
              </div>

              {/* 기업 정보 카드 */}
              <div className="rounded-2xl bg-card p-6 border">
                <h3 className="font-semibold mb-4">기업 정보</h3>
                <div className="space-y-3 text-sm">
                  <Link 
                    to={`/companies/${job.employerId}`}
                    className="flex items-center gap-3 hover:bg-secondary/50 rounded-lg p-2 -mx-2 transition-colors"
                  >
                    {job.companyLogoUrl ? (
                      <img 
                        src={job.companyLogoUrl} 
                        alt={job.companyName}
                        className="h-10 w-10 rounded-lg object-cover"
                      />
                    ) : (
                      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 font-bold text-primary">
                        {job.companyName?.charAt(0) || "?"}
                      </div>
                    )}
                    <span className="font-medium">{job.companyName}</span>
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </main>
        
        <Footer />
      </div>
    </div>
  );
}
