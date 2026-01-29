import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Progress } from "@/components/ui/progress";
import { 
  Sparkles, 
  Star, 
  Bookmark, 
  MessageSquare, 
  TrendingUp,
  Code,
  Briefcase,
  Award,
  ArrowRight
} from "lucide-react";

const mockRecommendedTalents = [
  {
    id: 1,
    name: "홍길동",
    title: "시니어 풀스택 개발자",
    experience: "8년",
    skills: ["React", "Node.js", "TypeScript", "AWS", "PostgreSQL"],
    matchScore: 98,
    matchReasons: ["기술스택 95% 일치", "경력 조건 충족", "포트폴리오 우수"],
    salary: "1억 ~ 1.2억",
    location: "서울 강남구",
    isBookmarked: false,
    avatar: null,
  },
  {
    id: 2,
    name: "김개발",
    title: "프론트엔드 개발자",
    experience: "5년",
    skills: ["React", "Vue.js", "TypeScript", "Next.js"],
    matchScore: 94,
    matchReasons: ["프론트엔드 전문가", "React 심화 경험", "팀 리딩 경험"],
    salary: "8,000만원 ~ 9,500만원",
    location: "서울 서초구",
    isBookmarked: true,
    avatar: null,
  },
  {
    id: 3,
    name: "이백엔드",
    title: "백엔드 개발자",
    experience: "6년",
    skills: ["Java", "Spring Boot", "Kotlin", "MySQL", "Redis"],
    matchScore: 91,
    matchReasons: ["대규모 트래픽 처리 경험", "MSA 아키텍처 설계", "코드 리뷰 문화"],
    salary: "9,000만원 ~ 1.1억",
    location: "서울 성동구",
    isBookmarked: false,
    avatar: null,
  },
  {
    id: 4,
    name: "박데이터",
    title: "데이터 엔지니어",
    experience: "4년",
    skills: ["Python", "Spark", "Airflow", "BigQuery", "Kafka"],
    matchScore: 87,
    matchReasons: ["실시간 파이프라인 구축", "데이터 웨어하우스 설계", "ML 파이프라인 경험"],
    salary: "7,000만원 ~ 8,500만원",
    location: "경기 성남시",
    isBookmarked: false,
    avatar: null,
  },
];

export function AIMatchingTab() {
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
                귀사의 채용공고와 가장 잘 맞는 인재를 AI가 분석하여 추천합니다
              </p>
            </div>
            <Button variant="outline" className="hidden sm:flex">
              <TrendingUp className="h-4 w-4 mr-2" />
              매칭 조건 설정
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 매칭 통계 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-primary">127</p>
            <p className="text-sm text-muted-foreground">추천 인재</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-success">23</p>
            <p className="text-sm text-muted-foreground">높은 매칭 (90%+)</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-info">8</p>
            <p className="text-sm text-muted-foreground">북마크</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-accent">5</p>
            <p className="text-sm text-muted-foreground">연락 중</p>
          </CardContent>
        </Card>
      </div>

      {/* 추천 인재 목록 */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <Star className="h-5 w-5 text-warning" />
            TOP 추천 인재
          </CardTitle>
          <Button variant="ghost" size="sm">
            전체보기 <ArrowRight className="h-4 w-4 ml-1" />
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {mockRecommendedTalents.map((talent) => (
            <div
              key={talent.id}
              className="p-4 rounded-xl border border-border bg-card hover:shadow-card-hover transition-all"
            >
              <div className="flex flex-col lg:flex-row gap-4">
                {/* 프로필 */}
                <div className="flex items-start gap-4 flex-1">
                  <Avatar className="h-14 w-14">
                    <AvatarImage src={talent.avatar || undefined} />
                    <AvatarFallback className="bg-primary/10 text-primary text-lg">
                      {talent.name.slice(0, 2)}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold text-foreground">{talent.name}</h3>
                      {talent.isBookmarked && (
                        <Bookmark className="h-4 w-4 text-warning fill-warning" />
                      )}
                    </div>
                    <p className="text-sm text-muted-foreground mb-2">
                      {talent.title} • 경력 {talent.experience}
                    </p>
                    <div className="flex flex-wrap gap-1.5 mb-3">
                      {talent.skills.slice(0, 5).map((skill) => (
                        <Badge key={skill} variant="secondary" className="text-xs">
                          {skill}
                        </Badge>
                      ))}
                    </div>
                    <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <Briefcase className="h-3 w-3" />
                        {talent.salary}
                      </span>
                      <span>{talent.location}</span>
                    </div>
                  </div>
                </div>

                {/* 매칭 정보 */}
                <div className="lg:w-64 space-y-3">
                  <div className="p-3 rounded-lg bg-primary/5 border border-primary/20">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-medium text-foreground">AI 매칭 점수</span>
                      <span className="text-2xl font-bold text-primary">{talent.matchScore}%</span>
                    </div>
                    <Progress value={talent.matchScore} className="h-2" />
                  </div>
                  <div className="space-y-1.5">
                    {talent.matchReasons.map((reason, idx) => (
                      <div key={idx} className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Award className="h-3 w-3 text-success" />
                        {reason}
                      </div>
                    ))}
                  </div>
                </div>

                {/* 액션 버튼 */}
                <div className="flex lg:flex-col gap-2 justify-end">
                  <Button size="sm" className="btn-gradient-primary">
                    <MessageSquare className="h-4 w-4 mr-1" />
                    연락하기
                  </Button>
                  <Button variant="outline" size="sm">
                    <Bookmark className="h-4 w-4 mr-1" />
                    북마크
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
