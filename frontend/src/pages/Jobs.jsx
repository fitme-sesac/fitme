import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Search, Filter, MapPin, Loader2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { JobCard } from "@/components/home/JobCard";
import { usePublicJobs, useFilterOptions } from "@/hooks/useJobs";

export default function Jobs() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get("keyword") || "");
  const [selectedStack, setSelectedStack] = useState(searchParams.get("stack") || "");
  const [selectedLocation, setSelectedLocation] = useState(searchParams.get("location") || "");
  const [page, setPage] = useState(0);

  const { data, isLoading, error } = usePublicJobs({
    page,
    size: 12,
    keyword: searchParams.get("keyword") || "",
    stack: searchParams.get("stack") || "",
    location: searchParams.get("location") || "",
  });

  const { data: filterOptions } = useFilterOptions();

  const handleSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (keyword) params.set("keyword", keyword);
    if (selectedStack) params.set("stack", selectedStack);
    if (selectedLocation) params.set("location", selectedLocation);
    setSearchParams(params);
    setPage(0);
  };

  const clearFilters = () => {
    setKeyword("");
    setSelectedStack("");
    setSelectedLocation("");
    setSearchParams({});
    setPage(0);
  };

  const jobs = data?.jobs || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      
      <div className="lg:pl-64">
        <Header />
        
        <main className="container py-8">
          {/* 검색 및 필터 */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold mb-6">채용공고</h1>
            
            <form onSubmit={handleSearch} className="space-y-4">
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="직무, 회사명, 기술스택으로 검색..."
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Button type="submit">검색</Button>
              </div>

              <div className="flex flex-wrap gap-2 items-center">
                <Filter className="h-4 w-4 text-muted-foreground" />
                
                <Select value={selectedStack} onValueChange={setSelectedStack}>
                  <SelectTrigger className="w-40">
                    <SelectValue placeholder="기술스택" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">전체</SelectItem>
                    {filterOptions?.stacks?.map((stack) => (
                      <SelectItem key={stack} value={stack}>
                        {stack}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                <Select value={selectedLocation} onValueChange={setSelectedLocation}>
                  <SelectTrigger className="w-40">
                    <SelectValue placeholder="지역" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">전체</SelectItem>
                    {filterOptions?.locations?.map((loc) => (
                      <SelectItem key={loc} value={loc}>
                        {loc}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                {(searchParams.get("keyword") || searchParams.get("stack") || searchParams.get("location")) && (
                  <Button variant="ghost" size="sm" onClick={clearFilters}>
                    필터 초기화
                  </Button>
                )}
              </div>
            </form>

            {/* 검색 결과 정보 */}
            {!isLoading && (
              <p className="mt-4 text-sm text-muted-foreground">
                총 {totalElements.toLocaleString()}개의 채용공고
              </p>
            )}
          </div>

          {/* 로딩 상태 */}
          {isLoading && (
            <div className="flex items-center justify-center py-20">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <span className="ml-2 text-muted-foreground">채용공고를 불러오는 중...</span>
            </div>
          )}

          {/* 에러 상태 */}
          {error && !isLoading && (
            <div className="rounded-lg bg-destructive/10 p-4 text-destructive">
              채용공고를 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.
            </div>
          )}

          {/* 채용공고 그리드 */}
          {!isLoading && jobs.length > 0 && (
            <>
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

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="mt-8 flex justify-center gap-2">
                  <Button
                    variant="outline"
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                  >
                    이전
                  </Button>
                  <span className="flex items-center px-4 text-sm text-muted-foreground">
                    {page + 1} / {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage(page + 1)}
                  >
                    다음
                  </Button>
                </div>
              )}
            </>
          )}

          {/* 데이터 없음 */}
          {!isLoading && !error && jobs.length === 0 && (
            <div className="flex flex-col items-center justify-center py-20 text-center">
              <MapPin className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-lg font-medium">검색 결과가 없습니다</p>
              <p className="text-muted-foreground mt-1">
                다른 검색어나 필터 조건을 시도해보세요.
              </p>
              <Button variant="outline" className="mt-4" onClick={clearFilters}>
                필터 초기화
              </Button>
            </div>
          )}
        </main>
        
        <Footer />
      </div>
    </div>
  );
}
