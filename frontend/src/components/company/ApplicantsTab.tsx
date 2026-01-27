import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { 
  User, 
  Mail, 
  Phone, 
  Calendar, 
  FileText, 
  MessageSquare,
  CheckCircle,
  XCircle,
  Clock,
  Filter
} from "lucide-react";

const mockApplicants = [
  {
    id: 1,
    name: "김철수",
    email: "kim@example.com",
    phone: "010-1234-5678",
    jobTitle: "시니어 프론트엔드 개발자",
    status: "submitted",
    appliedAt: "2026-01-25",
    experience: "6년",
    matchScore: 92,
    avatar: null,
  },
  {
    id: 2,
    name: "이영희",
    email: "lee@example.com",
    phone: "010-2345-6789",
    jobTitle: "백엔드 개발자",
    status: "interview",
    appliedAt: "2026-01-24",
    experience: "4년",
    matchScore: 88,
    avatar: null,
  },
  {
    id: 3,
    name: "박민수",
    email: "park@example.com",
    phone: "010-3456-7890",
    jobTitle: "시니어 프론트엔드 개발자",
    status: "hired",
    appliedAt: "2026-01-20",
    experience: "7년",
    matchScore: 95,
    avatar: null,
  },
  {
    id: 4,
    name: "최수진",
    email: "choi@example.com",
    phone: "010-4567-8901",
    jobTitle: "데이터 엔지니어",
    status: "rejected",
    appliedAt: "2026-01-18",
    experience: "2년",
    matchScore: 65,
    avatar: null,
  },
  {
    id: 5,
    name: "정우성",
    email: "jung@example.com",
    phone: "010-5678-9012",
    jobTitle: "백엔드 개발자",
    status: "submitted",
    appliedAt: "2026-01-26",
    experience: "5년",
    matchScore: 85,
    avatar: null,
  },
];

const statusConfig = {
  submitted: { 
    label: "지원완료", 
    className: "bg-info/10 text-info border-info/20",
    icon: Clock 
  },
  interview: { 
    label: "면접예정", 
    className: "bg-warning/10 text-warning border-warning/20",
    icon: Calendar 
  },
  hired: { 
    label: "채용확정", 
    className: "bg-success/10 text-success border-success/20",
    icon: CheckCircle 
  },
  rejected: { 
    label: "불합격", 
    className: "bg-muted text-muted-foreground border-border",
    icon: XCircle 
  },
};

export function ApplicantsTab() {
  return (
    <div className="space-y-4">
      {/* 필터 */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4">
            <Select defaultValue="all">
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="채용공고 선택" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체 공고</SelectItem>
                <SelectItem value="1">시니어 프론트엔드 개발자</SelectItem>
                <SelectItem value="2">백엔드 개발자</SelectItem>
                <SelectItem value="3">데이터 엔지니어</SelectItem>
              </SelectContent>
            </Select>
            <Select defaultValue="all">
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="상태" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체 상태</SelectItem>
                <SelectItem value="submitted">지원완료</SelectItem>
                <SelectItem value="interview">면접예정</SelectItem>
                <SelectItem value="hired">채용확정</SelectItem>
                <SelectItem value="rejected">불합격</SelectItem>
              </SelectContent>
            </Select>
            <Select defaultValue="latest">
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="정렬" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="latest">최신순</SelectItem>
                <SelectItem value="match">매칭점수순</SelectItem>
                <SelectItem value="experience">경력순</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* 통계 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-foreground">111</p>
            <p className="text-sm text-muted-foreground">전체 지원자</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-info">68</p>
            <p className="text-sm text-muted-foreground">검토대기</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-warning">23</p>
            <p className="text-sm text-muted-foreground">면접예정</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-3xl font-bold text-success">8</p>
            <p className="text-sm text-muted-foreground">채용확정</p>
          </CardContent>
        </Card>
      </div>

      {/* 지원자 목록 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">지원자 목록</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {mockApplicants.map((applicant) => {
            const StatusIcon = statusConfig[applicant.status as keyof typeof statusConfig].icon;
            return (
              <div
                key={applicant.id}
                className="p-4 rounded-xl border border-border bg-card hover:shadow-card-hover transition-all"
              >
                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <Avatar className="h-12 w-12">
                      <AvatarImage src={applicant.avatar || undefined} />
                      <AvatarFallback className="bg-primary/10 text-primary">
                        {applicant.name.slice(0, 2)}
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-semibold text-foreground">{applicant.name}</h3>
                        <Badge variant="outline" className={statusConfig[applicant.status as keyof typeof statusConfig].className}>
                          <StatusIcon className="h-3 w-3 mr-1" />
                          {statusConfig[applicant.status as keyof typeof statusConfig].label}
                        </Badge>
                      </div>
                      <p className="text-sm text-muted-foreground mb-2">
                        {applicant.jobTitle} • 경력 {applicant.experience}
                      </p>
                      <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Mail className="h-3 w-3" />
                          {applicant.email}
                        </span>
                        <span className="flex items-center gap-1">
                          <Calendar className="h-3 w-3" />
                          {applicant.appliedAt}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-4">
                    {/* AI 매칭 점수 */}
                    <div className="text-center px-4 py-2 rounded-lg bg-primary/5 border border-primary/20">
                      <p className="text-2xl font-bold text-primary">{applicant.matchScore}%</p>
                      <p className="text-xs text-muted-foreground">AI 매칭</p>
                    </div>

                    <div className="flex gap-2">
                      <Button variant="outline" size="sm">
                        <FileText className="h-4 w-4 mr-1" />
                        이력서
                      </Button>
                      <Button variant="outline" size="sm">
                        <MessageSquare className="h-4 w-4 mr-1" />
                        메시지
                      </Button>
                    </div>
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
