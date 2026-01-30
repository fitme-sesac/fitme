import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Progress } from "@/components/ui/progress";
import { 
  Sparkles, 
  Bookmark, 
  MessageSquare, 
  MapPin,
  Briefcase,
  ArrowRight,
  RefreshCw,
  Filter
} from "lucide-react";
import { Link } from "react-router-dom";

const mockTalents = [
  {
    id: 1,
    name: "김개발",
    title: "시니어 프론트엔드 개발자",
    experience: "8년",
    skills: ["React", "TypeScript", "Next.js", "Vue.js", "Tailwind CSS"],
    matchScore: 98,
    location: "서울 강남구",
    salary: "1억 ~ 1.2억",
    avatar: null,
    isNew: true,
    lastActive: "방금 전",
  },
  {
    id: 2,
    name: "이백엔드",
    title: "백엔드 개발자",
    experience: "5년",
    skills: ["Node.js", "Python", "PostgreSQL", "AWS", "Docker"],
    matchScore: 95,
    location: "서울 서초구",
    salary: "8,000만 ~ 1억",
    avatar: null,
    isNew: true,
    lastActive: "10분 전",
  },
  {
    id: 3,
    name: "박풀스택",
    title: "풀스택 개발자",
    experience: "6년",
    skills: ["React", "Node.js", "TypeScript", "MongoDB", "GraphQL"],
    matchScore: 92,
    location: "경기 성남시",
    salary: "9,000만 ~ 1.1억",
    avatar: null,
    isNew: false,
    lastActive: "1시간 전",
  },
  {
    id: 4,
    name: "최데이터",
    title: "데이터 엔지니어",
    experience: "4년",
    skills: ["Python", "Spark", "Airflow", "BigQuery", "Kafka"],
    matchScore: 89,
    location: "서울 마포구",
    salary: "7,500만 ~ 9,000만",
    avatar: null,
    isNew: false,
    lastActive: "3시간 전",
  },
  {
    id: 5,
    name: "정클라우드",
    title: "DevOps 엔지니어",
    experience: "5년",
    skills: ["Kubernetes", "AWS", "Terraform", "CI/CD", "Linux"],
    matchScore: 87,
    location: "서울 영등포구",
    salary: "8,500만 ~ 1억",
    avatar: null,
    isNew: false,
    lastActive: "5시간 전",
  },
  {
    id: 6,
    name: "한디자인",
    title: "UI/UX 디자이너",
    experience: "7년",
    skills: ["Figma", "Sketch", "Prototyping", "Design System", "Framer"],
    matchScore: 85,
    location: "서울 강남구",
    salary: "7,000만 ~ 8,500만",
    avatar: null,
    isNew: true,
    lastActive: "30분 전",
  },
];

export function TalentRecommendationSection() {
  return (
    <section className="py-12 lg:py-16">
      <div className="container">
        {/* 섹션 헤더 */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
              <Sparkles className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-foreground">AI 추천 인재</h2>
              <p className="text-sm text-muted-foreground">
                귀사의 채용 공고와 가장 잘 맞는 인재입니다
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm">
              <Filter className="h-4 w-4 mr-1" />
              필터
            </Button>
            <Button variant="outline" size="sm">
              <RefreshCw className="h-4 w-4 mr-1" />
              새로고침
            </Button>
          </div>
        </div>

        {/* 인재 그리드 */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {mockTalents.map((talent) => (
            <Card key={talent.id} className="group hover:shadow-lg transition-all duration-300 border-border hover:border-primary/30">
              <CardContent className="p-5">
                {/* 상단: 프로필 */}
                <div className="flex items-start gap-4 mb-4">
                  <Avatar className="h-14 w-14">
                    <AvatarImage src={talent.avatar || undefined} />
                    <AvatarFallback className="bg-primary/10 text-primary text-lg font-semibold">
                      {talent.name.slice(0, 2)}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold text-foreground truncate">{talent.name}</h3>
                      {talent.isNew && (
                        <Badge variant="secondary" className="bg-accent text-accent-foreground text-[10px] px-1.5">
                          NEW
                        </Badge>
                      )}
                    </div>
                    <p className="text-sm text-muted-foreground truncate">{talent.title}</p>
                    <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <Briefcase className="h-3 w-3" />
                        경력 {talent.experience}
                      </span>
                      <span className="flex items-center gap-1">
                        <MapPin className="h-3 w-3" />
                        {talent.location}
                      </span>
                    </div>
                  </div>
                </div>

                {/* 매칭 점수 */}
                <div className="p-3 rounded-lg bg-primary/5 border border-primary/10 mb-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-medium text-muted-foreground">AI 매칭률</span>
                    <span className="text-lg font-bold text-primary">{talent.matchScore}%</span>
                  </div>
                  <Progress value={talent.matchScore} className="h-1.5" />
                </div>

                {/* 스킬 태그 */}
                <div className="flex flex-wrap gap-1.5 mb-4">
                  {talent.skills.slice(0, 4).map((skill) => (
                    <Badge key={skill} variant="secondary" className="text-xs">
                      {skill}
                    </Badge>
                  ))}
                  {talent.skills.length > 4 && (
                    <Badge variant="outline" className="text-xs text-muted-foreground">
                      +{talent.skills.length - 4}
                    </Badge>
                  )}
                </div>

                {/* 희망 연봉 */}
                <p className="text-sm text-muted-foreground mb-4">
                  희망 연봉: <span className="font-medium text-foreground">{talent.salary}</span>
                </p>

                {/* 액션 버튼 */}
                <div className="flex gap-2">
                  <Button size="sm" className="flex-1 btn-gradient-primary">
                    <MessageSquare className="h-4 w-4 mr-1" />
                    연락하기
                  </Button>
                  <Button variant="outline" size="sm">
                    <Bookmark className="h-4 w-4" />
                  </Button>
                </div>

                {/* 마지막 활동 */}
                <p className="text-[11px] text-muted-foreground text-center mt-3">
                  마지막 활동: {talent.lastActive}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* 더보기 */}
        <div className="flex justify-center mt-8">
          <Button asChild variant="outline" size="lg">
            <Link to="/talents">
              더 많은 인재 보기
              <ArrowRight className="h-4 w-4 ml-2" />
            </Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
