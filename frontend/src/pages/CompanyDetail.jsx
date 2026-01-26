import { useParams, Link } from "react-router-dom";
import { 
  ArrowLeft, MapPin, Users, Building2, Globe, Calendar,
  Briefcase, Loader2 
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { JobCard } from "@/components/home/JobCard";
import { usePublicEmployer } from "@/hooks/useEmployers";
import { usePublicJobs } from "@/hooks/useJobs";

export default function CompanyDetail() {
  const { employerId } = useParams();
  const { data: company, isLoading, error } = usePublicEmployer(employerId);
  
  // 해당 기업의 채용공고 조회 (임시로 전체에서 필터링)
  const { data: jobsData } = usePublicJobs({ page: 0, size: 100 });
  
  // 기업 ID로 필터링 (실제로는 백엔드에서 employer_id로 필터해야 함)
  const companyJobs = jobsData?.jobs?.filter(job => job.employerId === parseInt(employerId)) || [];

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <Sidebar />
        <div className="lg:pl-64">
          <Header />
          <main className="container py-20">
            <div className="flex items-center justify-center">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <span className="ml-2 text-muted-foreground">기업 정보를 불러오는 중...</span>
            </div>
          </main>
        </div>
      </div>
    );
  }

  if (error || !company) {
    return (
      <div className="min-h-screen bg-background">
        <Sidebar />
        <div className="lg:pl-64">
          <Header />
          <main className="container py-20">
            <div className="text-center">
              <h2 className="text-2xl font-bold mb-2">기업을 찾을 수 없습니다</h2>
              <p className="text-muted-foreground mb-4">
                요청하신 기업이 존재하지 않거나 삭제되었습니다.
              </p>
              <Link to="/">
                <Button>
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  홈으로 돌아가기
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
          <Link to="/" className="inline-flex items-center text-muted-foreground hover:text-foreground mb-6">
            <ArrowLeft className="mr-2 h-4 w-4" />
            뒤로가기
          </Link>

          {/* 기업 헤더 */}
          <div className="rounded-2xl bg-card p-6 border mb-8">
            <div className="flex items-start gap-6">
              {company.logoUrl ? (
                <img 
                  src={company.logoUrl} 
                  alt={company.name}
                  className="h-24 w-24 rounded-2xl object-cover"
                />
              ) : (
                <div className="flex h-24 w-24 items-center justify-center rounded-2xl bg-primary/10 text-4xl font-bold text-primary">
                  {company.name?.charAt(0) || "?"}
                </div>
              )}
              
              <div className="flex-1">
                <h1 className="text-3xl font-bold mb-2">{company.name}</h1>
                
                <div className="flex flex-wrap gap-4 text-sm text-muted-foreground mb-4">
                  {company.industry && (
                    <div className="flex items-center gap-1">
                      <Building2 className="h-4 w-4" />
                      <span>{company.industry}</span>
                    </div>
                  )}
                  {company.location && (
                    <div className="flex items-center gap-1">
                      <MapPin className="h-4 w-4" />
                      <span>{company.location}</span>
                    </div>
                  )}
                  {company.employeeCount && (
                    <div className="flex items-center gap-1">
                      <Users className="h-4 w-4" />
                      <span>{company.employeeCount.toLocaleString()}명</span>
                    </div>
                  )}
                </div>

                <Badge variant="secondary" className="text-primary">
                  <Briefcase className="h-3 w-3 mr-1" />
                  채용 중 {company.openJobCount || 0}개
                </Badge>
              </div>
            </div>

            {/* 기업 소개 */}
            {company.description && (
              <div className="mt-6">
                <h3 className="font-semibold mb-2">기업 소개</h3>
                <p className="text-muted-foreground">{company.description}</p>
              </div>
            )}
          </div>

          {/* 채용 공고 */}
          <div>
            <h2 className="text-xl font-bold mb-4">
              채용 중인 공고 {company.openJobCount > 0 && `(${company.openJobCount})`}
            </h2>
            
            {companyJobs.length > 0 ? (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {companyJobs.map((job) => (
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
            ) : (
              <div className="rounded-2xl bg-secondary/30 p-8 text-center">
                <Briefcase className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
                <p className="text-muted-foreground">
                  현재 채용 중인 공고가 없습니다.
                </p>
              </div>
            )}
          </div>
        </main>
        
        <Footer />
      </div>
    </div>
  );
}
