import { useMemo } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Progress } from "@/components/ui/progress";
import { 
  Sparkles, 
  Star, 
  MessageSquare, 
  TrendingUp,
  Briefcase,
  Award,
  ArrowRight,
  Loader2
} from "lucide-react";
import { useApplicants } from "@/hooks/useEmployers";

function matchRateDisplay(matchInfo: { overallMatchRate?: number; matchRate?: number } | null | undefined): number {
  if (!matchInfo) return 0;
  return matchInfo.overallMatchRate ?? matchInfo.matchRate ?? 0;
}

export function AIMatchingTab() {
  const { data, isLoading, error } = useApplicants("");
  const applicants = data?.applicants ?? [];

  const topByMatch = useMemo(() => {
    return [...applicants]
      .sort((a, b) => matchRateDisplay(b.matchInfo) - matchRateDisplay(a.matchInfo))
      .slice(0, 10);
  }, [applicants]);

  const highMatchCount = useMemo(
    () => applicants.filter((a) => matchRateDisplay(a.matchInfo) >= 90).length,
    [applicants]
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* AI 추천 헤더 */}
      <Card className="border-primary/20 bg-gradient-to-r from-primary/5 to-info/5">
        <CardContent className="p-6">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10">
              <Sparkles className="h-7 w-7 text-primary" />
            </div>
            <div className="flex-1">
              <h2 className="text-xl font-bold text-foreground">AI 인재 매칭</h2>
              <p className="text-muted-foreground">
                귀사의 채용공고(stack)와 구직자 이력서(re_stack·tech_stack) 기반으로 매칭률을 산출합니다
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 매칭 통계 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-primary">{applicants.length}</p>
            <p className="text-sm text-muted-foreground">추천 인재 (지원자)</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-success">{highMatchCount}</p>
            <p className="text-sm text-muted-foreground">높은 매칭 (90%+)</p>
          </CardContent>
        </Card>
      </div>

      {/* TOP 추천 인재 (매칭률순) */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <Star className="h-5 w-5 text-warning" />
            TOP 추천 인재 (채용공고 vs 이력서 매칭)
          </CardTitle>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/company/dashboard?tab=applicants">
              전체보기 <ArrowRight className="h-4 w-4 ml-1" />
            </Link>
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {error && (
            <p className="text-center py-4 text-muted-foreground">지원자 목록을 불러오지 못했습니다.</p>
          )}
          {!error && topByMatch.length === 0 && (
            <p className="text-center py-8 text-muted-foreground">아직 지원자가 없습니다. 채용공고를 등록해 보세요.</p>
          )}
          {topByMatch.map((applicant) => {
            const matchInfo = applicant.matchInfo;
            const rate = matchRateDisplay(matchInfo);
            const reasons: string[] = [];
            if (matchInfo?.stackMatchRate != null) reasons.push(`기술스택 ${matchInfo.stackMatchRate}% 일치`);
            if (matchInfo?.experienceMatchRate != null) reasons.push(`경력 매칭 ${matchInfo.experienceMatchRate}%`);
            if ((matchInfo?.matchedStacks?.length ?? 0) > 0) reasons.push(`보유 기술: ${(matchInfo.matchedStacks ?? []).slice(0, 3).join(", ")}`);

            return (
              <div
                key={applicant.applicationId}
                className="p-4 rounded-xl border border-border bg-card hover:shadow-card-hover transition-all"
              >
                <div className="flex flex-col lg:flex-row gap-4">
                  <div className="flex items-start gap-4 flex-1">
                    <Avatar className="h-14 w-14">
                      <AvatarFallback className="bg-primary/10 text-primary text-lg">
                        {(applicant.name || "?").slice(0, 2)}
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-semibold text-foreground">{applicant.name || "-"}</h3>
                      </div>
                      <p className="text-sm text-muted-foreground mb-2">
                        {applicant.jobTitle || "-"} · {applicant.email || "-"}
                      </p>
                      {(matchInfo?.matchedStacks?.length ?? 0) > 0 && (
                        <div className="flex flex-wrap gap-1.5 mb-3">
                          {(matchInfo?.matchedStacks ?? []).slice(0, 5).map((s, i) => (
                            <Badge key={i} variant="secondary" className="text-xs">
                              {s}
                            </Badge>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="lg:w-64 space-y-3">
                    <div className="p-3 rounded-lg bg-primary/5 border border-primary/20">
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-medium text-foreground">AI 매칭 점수</span>
                        <span className="text-2xl font-bold text-primary">{rate}%</span>
                      </div>
                      <Progress value={rate} className="h-2" />
                    </div>
                    <div className="space-y-1.5">
                      {reasons.slice(0, 3).map((reason, idx) => (
                        <div key={idx} className="flex items-center gap-2 text-xs text-muted-foreground">
                          <Award className="h-3 w-3 text-success shrink-0" />
                          {reason}
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="flex lg:flex-col gap-2 justify-end">
                    <Button size="sm" className="btn-gradient-primary" asChild>
                      <Link to="/company/dashboard?tab=applicants">
                        <MessageSquare className="h-4 w-4 mr-1" />
                        지원자 보기
                      </Link>
                    </Button>
                  </div>
                </div>
              </div>
            );
          })}
        </CardContent>
      </Card>
    </div>
  );
}
