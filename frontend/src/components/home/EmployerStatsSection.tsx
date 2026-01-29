import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { 
  Briefcase, 
  Users, 
  Eye, 
  TrendingUp,
  ArrowRight,
  Plus
} from "lucide-react";
import { Link } from "react-router-dom";

export function EmployerStatsSection() {
  return (
    <section className="py-8 border-b border-border bg-secondary/30">
      <div className="container">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="bg-card">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                  <Briefcase className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">3</p>
                  <p className="text-xs text-muted-foreground">진행 중인 채용</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-card">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-success/10">
                  <Users className="h-5 w-5 text-success" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">127</p>
                  <p className="text-xs text-muted-foreground">총 지원자</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-card">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-info/10">
                  <Eye className="h-5 w-5 text-info" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">2,456</p>
                  <p className="text-xs text-muted-foreground">공고 조회수</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-card">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-warning/10">
                  <TrendingUp className="h-5 w-5 text-warning" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">+23%</p>
                  <p className="text-xs text-muted-foreground">이번 주 증가</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* 빠른 액션 */}
        <div className="flex flex-wrap gap-3 mt-6 justify-center">
          <Button asChild className="btn-gradient-primary">
            <Link to="/jobs/new">
              <Plus className="h-4 w-4 mr-1" />
              새 채용공고 등록
            </Link>
          </Button>
          <Button asChild variant="outline">
            <Link to="/company/dashboard">
              대시보드로 이동
              <ArrowRight className="h-4 w-4 ml-1" />
            </Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
