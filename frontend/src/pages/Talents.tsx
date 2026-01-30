import { useMemo } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { TalentCard } from "@/components/home/TalentCard";
import { Sparkles, ArrowRight, Loader2 } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useApplicants } from "@/hooks/useEmployers";

const mockTalents = [
  { id: 1, name: "김서연", title: "Senior Frontend Developer", summary: "대규모 서비스 성능 최적화와 접근성 개선에 관심이 많은 프론트엔드 개발자입니다.", experience: "5년", location: "서울", education: "컴퓨터공학과 학사", skills: ["React", "TypeScript", "Next.js", "GraphQL"], salary: "8,000만 ~ 1억 2,000만원", matchScore: 95, avatar: null, lastUpdated: "1일 전", isNew: true },
  { id: 2, name: "박민준", title: "AI/ML Engineer", summary: "딥러닝과 MLOps를 활용한 서비스 적용 경험이 있으며, 실험 파이프라인 구축에 강점이 있습니다.", experience: "4년", location: "판교", education: "인공지능학과 석사", skills: ["Python", "TensorFlow", "PyTorch", "Deep Learning", "MLOps"], salary: "9,000만 ~ 1억 3,000만원", matchScore: 88, avatar: null, lastUpdated: "2일 전", isNew: false },
  { id: 3, name: "이하은", title: "Product Designer", summary: "사용자 리서치와 디자인 시스템 구축을 통해 제품 경험을 개선해 온 디자이너입니다.", experience: "6년", location: "서울", education: "시각디자인학과 학사", skills: ["Figma", "UI/UX", "Prototyping", "Design System"], salary: "7,500만 ~ 1억원", matchScore: 82, avatar: null, lastUpdated: "3일 전", isNew: true },
  { id: 4, name: "최준호", title: "Backend Developer", summary: "MSA와 클라우드 네이티브 아키텍처 설계 경험이 있으며, 고가용성 서비스를 지향합니다.", experience: "5년", location: "서울", education: "소프트웨어학과 학사", skills: ["Java", "Spring", "Kubernetes", "AWS", "MSA"], salary: "8,500만 ~ 1억 2,000만원", matchScore: 90, avatar: null, lastUpdated: "1일 전", isNew: false },
  { id: 5, name: "정수진", title: "DevOps Engineer", summary: "CI/CD와 인프라 자동화로 배포 안정성과 개발 생산성을 높이는 데 기여해 왔습니다.", experience: "4년", location: "경기 성남", education: "컴퓨터공학과 학사", skills: ["Node.js", "PostgreSQL", "Docker", "Terraform", "CI/CD", "Monitoring"], salary: "8,000만 ~ 1억 1,000만원", matchScore: 85, avatar: null, lastUpdated: "2일 전", isNew: true },
  { id: 6, name: "한지우", title: "Data Engineer", summary: "대용량 데이터 파이프라인과 실시간 스트리밍 구축 경험이 있습니다.", experience: "5년", location: "판교", education: "통계학과 석사", skills: ["Python", "Spark", "Airflow", "Kafka", "BigQuery"], salary: "9,000만 ~ 1억 2,000만원", matchScore: 87, avatar: null, lastUpdated: "4일 전", isNew: false },
];

function matchRateDisplay(matchInfo: { overallMatchRate?: number; matchRate?: number } | null | undefined): number {
  if (!matchInfo) return 0;
  return matchInfo.overallMatchRate ?? matchInfo.matchRate ?? 0;
}

export default function Talents() {
  const { isCompany } = useAuth();
  const { data: applicantsData, isLoading: applicantsLoading } = useApplicants("");

  const talentCards = useMemo(() => {
    if (!isCompany || !applicantsData?.applicants?.length) return null;
    const list = [...applicantsData.applicants]
      .sort((a, b) => matchRateDisplay(b.matchInfo) - matchRateDisplay(a.matchInfo))
      .map((a) => ({
        id: a.applicationId ?? a.memberId ?? 0,
        name: a.name || "-",
        title: a.jobTitle || "-",
        summary: undefined,
        experience: "-",
        location: "-",
        education: undefined,
        skills: Array.isArray(a.matchInfo?.matchedStacks) ? a.matchInfo.matchedStacks : [],
        salary: "-",
        matchScore: matchRateDisplay(a.matchInfo),
        avatar: null,
        lastUpdated: a.appliedAt ? new Date(a.appliedAt).toLocaleDateString("ko-KR") : undefined,
        isNew: false,
      }));
    return list;
  }, [isCompany, applicantsData?.applicants]);

  const displayTalents = talentCards ?? mockTalents;
  const isEmployerView = isCompany && talentCards != null;

  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <div className="lg:pl-64 transition-all duration-300">
        <Header />
        <main className="container max-w-6xl mx-auto py-8 px-4 md:px-8 min-h-[calc(100vh-200px)]">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
                <Sparkles className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">AI 추천 인재</p>
                <h1 className="text-2xl font-bold text-foreground">
                  {isEmployerView ? "당신의 채용공고에 딱 맞는 인재들 (채용공고 vs 이력서 매칭)" : "당신의 회사에 딱 맞는 인재들"}
                </h1>
              </div>
            </div>
            {isCompany && (
              <Link to="/company/dashboard?tab=applicants" className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary hover:underline">
                지원자 현황 보기
                <ArrowRight className="h-4 w-4" />
              </Link>
            )}
          </div>

          {isCompany && applicantsLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {displayTalents.map((talent) => (
                <TalentCard
                  key={talent.id}
                  id={talent.id}
                  name={talent.name}
                  title={talent.title}
                  summary={talent.summary}
                  experience={talent.experience}
                  location={talent.location}
                  education={talent.education}
                  skills={talent.skills}
                  salary={talent.salary}
                  matchScore={talent.matchScore}
                  avatar={talent.avatar}
                  lastUpdated={talent.lastUpdated}
                  isNew={talent.isNew}
                />
              ))}
            </div>
          )}
        </main>
        <Footer />
      </div>
    </div>
  );
}
