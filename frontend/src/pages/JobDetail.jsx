import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { 
  ArrowLeft, MapPin, Banknote, Clock, Building2, Users, 
  Briefcase, Globe, Mail, Phone, Heart, Share2, Loader2, Sparkles, 
  ChevronDown, ChevronUp, CheckCircle, AlertCircle 
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { usePublicJob } from "@/hooks/useJobs";
import { useAuth } from "@/contexts/AuthContext";

const matchRateDisplay = (matchInfo) => matchInfo?.overallMatchRate ?? matchInfo?.matchRate ?? 0;
const matchLevelLabel = (level) => {
  if (level === "EXCELLENT") return "🎯 아주 잘 맞아요!";
  if (level === "GOOD") return "👍 잘 맞는 공고예요";
  if (level === "MODERATE") return "📚 도전해볼 만해요";
  if (level === "LOW") return "🌱 새로운 기회예요";
  return "";
};

export default function JobDetail() {
  const { jobId } = useParams();
  const { data: job, isLoading, error } = usePublicJob(jobId);
  const { isAuthenticated } = useAuth();
  const [showMatchDetail, setShowMatchDetail] = useState(false);

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

                {/* AI 매칭 스코어 (로그인 시 matchInfo 있음) */}
                {job.matchInfo && (
                  <div className="mt-4 rounded-xl border bg-card p-4 space-y-4">
                    <div className="flex items-center gap-2">
                      <Sparkles className="h-5 w-5 text-primary" />
                      <span className="font-semibold">나와의 매칭률</span>
                    </div>
                    <div className="flex flex-col sm:flex-row items-center gap-4">
                      <div
                        className="w-24 h-24 rounded-full border-4 flex flex-col items-center justify-center shrink-0 bg-muted/30"
                        style={{
                          borderColor:
                            job.matchInfo.matchLevel === "EXCELLENT" ? "#28a745" :
                            job.matchInfo.matchLevel === "GOOD" ? "#17a2b8" :
                            job.matchInfo.matchLevel === "MODERATE" ? "#ffc107" : "#6c757d",
                        }}
                      >
                        <span className="text-xl font-bold">{matchRateDisplay(job.matchInfo)}%</span>
                        <span className="text-xs text-muted-foreground">종합</span>
                      </div>
                      <div className="flex-1 text-center sm:text-left">
                        <Badge
                          variant={
                            job.matchInfo.matchLevel === "EXCELLENT" ? "default" :
                            job.matchInfo.matchLevel === "GOOD" ? "secondary" :
                            job.matchInfo.matchLevel === "MODERATE" ? "outline" : "outline"
                          }
                          className={
                            job.matchInfo.matchLevel === "EXCELLENT" ? "bg-green-600" :
                            job.matchInfo.matchLevel === "GOOD" ? "bg-sky-600" :
                            job.matchInfo.matchLevel === "MODERATE" ? "bg-amber-500 text-white" : ""
                          }
                        >
                          {matchLevelLabel(job.matchInfo.matchLevel)}
                        </Badge>
                        <div className="mt-3 grid grid-cols-3 gap-2 text-sm">
                          <div className="rounded-lg bg-muted/50 p-2 text-center">
                            <span className="font-semibold text-green-600">{job.matchInfo.stackMatchRate ?? 0}%</span>
                            <div className="text-muted-foreground text-xs">기술스택</div>
                          </div>
                          <div className="rounded-lg bg-muted/50 p-2 text-center">
                            <span className="font-semibold text-orange-500">{job.matchInfo.experienceMatchRate ?? 0}%</span>
                            <div className="text-muted-foreground text-xs">경력</div>
                          </div>
                          <div className="rounded-lg bg-muted/50 p-2 text-center">
                            <span className="font-semibold text-violet-600">{job.matchInfo.vectorMatchRate ?? 0}%</span>
                            <div className="text-muted-foreground text-xs">AI분석</div>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <div className="flex justify-between text-sm">
                        <span className="text-green-600 flex items-center gap-1">
                          <CheckCircle className="h-4 w-4" /> 보유 {job.matchInfo.matchedStacks?.length ?? 0}개
                        </span>
                        <span className="text-amber-600 flex items-center gap-1">
                          <AlertCircle className="h-4 w-4" /> 부족 {job.matchInfo.missingStacks?.length ?? 0}개
                        </span>
                      </div>
                      <div className="h-2 rounded-full bg-muted overflow-hidden">
                        <div
                          className="h-full rounded-full bg-green-500 transition-all"
                          style={{
                            width: job.matchInfo.requiredStacks?.length
                              ? `${((job.matchInfo.matchedStacks?.length ?? 0) / job.matchInfo.requiredStacks.length) * 100}%`
                              : "0%",
                          }}
                        />
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="w-full"
                      onClick={() => setShowMatchDetail((v) => !v)}
                    >
                      {showMatchDetail ? <ChevronUp className="h-4 w-4 mr-1" /> : <ChevronDown className="h-4 w-4 mr-1" />}
                      상세 보기
                    </Button>
                    {showMatchDetail && (
                      <div className="pt-3 border-t space-y-3">
                        <div>
                          <p className="text-xs font-semibold text-green-600 mb-1 flex items-center gap-1">
                            <CheckCircle className="h-3.5 w-3.5" /> 보유한 기술
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {(job.matchInfo.matchedStacks?.length ?? 0) > 0
                              ? job.matchInfo.matchedStacks.map((s, i) => (
                                  <Badge key={i} variant="secondary" className="bg-green-500/10 text-green-700 text-xs">
                                    {s}
                                  </Badge>
                                ))
                              : <span className="text-muted-foreground text-xs">없음</span>}
                          </div>
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-amber-600 mb-1 flex items-center gap-1">
                            <AlertCircle className="h-3.5 w-3.5" /> 부족한 기술
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {(job.matchInfo.missingStacks?.length ?? 0) > 0
                              ? job.matchInfo.missingStacks.map((s, i) => (
                                  <Badge key={i} variant="secondary" className="bg-amber-500/10 text-amber-700 text-xs">
                                    {s}
                                  </Badge>
                                ))
                              : <span className="text-muted-foreground text-xs">없음 🎉</span>}
                          </div>
                        </div>
                      </div>
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
