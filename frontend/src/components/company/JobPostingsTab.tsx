import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { 
  Briefcase, 
  Eye, 
  Users, 
  MoreVertical, 
  Edit, 
  Trash2, 
  Pause, 
  Play,
  TrendingUp,
  Clock
} from "lucide-react";

const mockJobPostings = [
  {
    id: 1,
    title: "시니어 프론트엔드 개발자",
    status: "active",
    applicants: 23,
    views: 1247,
    createdAt: "2026-01-20",
    expiresAt: "2026-02-20",
    techStack: ["React", "TypeScript", "Next.js"],
    experienceLevel: "시니어 (5년 이상)",
    salary: "8,000만원 ~ 1억",
  },
  {
    id: 2,
    title: "백엔드 개발자",
    status: "active",
    applicants: 45,
    views: 2103,
    createdAt: "2026-01-15",
    expiresAt: "2026-02-15",
    techStack: ["Node.js", "PostgreSQL", "AWS"],
    experienceLevel: "미들 (3~5년)",
    salary: "6,000만원 ~ 8,000만원",
  },
  {
    id: 3,
    title: "데이터 엔지니어",
    status: "paused",
    applicants: 12,
    views: 567,
    createdAt: "2026-01-10",
    expiresAt: "2026-02-10",
    techStack: ["Python", "Spark", "Airflow"],
    experienceLevel: "주니어 (1~3년)",
    salary: "5,000만원 ~ 6,000만원",
  },
  {
    id: 4,
    title: "DevOps 엔지니어",
    status: "closed",
    applicants: 31,
    views: 892,
    createdAt: "2025-12-01",
    expiresAt: "2026-01-01",
    techStack: ["Kubernetes", "Docker", "Terraform"],
    experienceLevel: "시니어 (5년 이상)",
    salary: "9,000만원 ~ 1.2억",
  },
];

const statusConfig = {
  active: { label: "진행중", className: "bg-success/10 text-success border-success/20" },
  paused: { label: "일시중지", className: "bg-warning/10 text-warning border-warning/20" },
  closed: { label: "마감", className: "bg-muted text-muted-foreground border-border" },
};

export function JobPostingsTab() {
  return (
    <div className="space-y-4">
      {/* 통계 카드 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-primary/10">
                <Briefcase className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">전체 공고</p>
                <p className="text-2xl font-bold text-foreground">4</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-success/10">
                <Play className="h-5 w-5 text-success" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">진행중</p>
                <p className="text-2xl font-bold text-foreground">2</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-info/10">
                <Eye className="h-5 w-5 text-info" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">총 조회수</p>
                <p className="text-2xl font-bold text-foreground">4,809</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-accent/10">
                <Users className="h-5 w-5 text-accent" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">총 지원자</p>
                <p className="text-2xl font-bold text-foreground">111</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 채용공고 목록 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">채용공고 목록</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {mockJobPostings.map((job) => (
            <div
              key={job.id}
              className="p-4 rounded-xl border border-border bg-card hover:shadow-card-hover transition-all"
            >
              <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <h3 className="font-semibold text-foreground">{job.title}</h3>
                    <Badge variant="outline" className={statusConfig[job.status as keyof typeof statusConfig].className}>
                      {statusConfig[job.status as keyof typeof statusConfig].label}
                    </Badge>
                  </div>
                  <div className="flex flex-wrap gap-2 mb-2">
                    {job.techStack.map((tech) => (
                      <Badge key={tech} variant="secondary" className="text-xs">
                        {tech}
                      </Badge>
                    ))}
                  </div>
                  <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                    <span>{job.experienceLevel}</span>
                    <span>{job.salary}</span>
                    <span className="flex items-center gap-1">
                      <Clock className="h-3.5 w-3.5" />
                      마감: {job.expiresAt}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-6">
                  <div className="flex gap-6">
                    <div className="text-center">
                      <p className="text-2xl font-bold text-foreground">{job.views.toLocaleString()}</p>
                      <p className="text-xs text-muted-foreground">조회수</p>
                    </div>
                    <div className="text-center">
                      <p className="text-2xl font-bold text-primary">{job.applicants}</p>
                      <p className="text-xs text-muted-foreground">지원자</p>
                    </div>
                  </div>

                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem>
                        <Edit className="h-4 w-4 mr-2" />
                        수정하기
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        {job.status === "active" ? (
                          <>
                            <Pause className="h-4 w-4 mr-2" />
                            일시중지
                          </>
                        ) : (
                          <>
                            <Play className="h-4 w-4 mr-2" />
                            재개하기
                          </>
                        )}
                      </DropdownMenuItem>
                      <DropdownMenuItem className="text-destructive">
                        <Trash2 className="h-4 w-4 mr-2" />
                        삭제하기
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
