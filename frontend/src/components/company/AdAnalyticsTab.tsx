import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { 
  BarChart3, 
  TrendingUp, 
  TrendingDown,
  Eye, 
  MousePointer, 
  DollarSign,
  Target,
  Calendar,
  Plus,
  ArrowUpRight,
  ArrowDownRight
} from "lucide-react";

const mockCampaigns = [
  {
    id: 1,
    name: "시니어 프론트엔드 개발자 상위노출",
    status: "active",
    type: "premium",
    budget: 500000,
    spent: 320000,
    impressions: 12500,
    clicks: 375,
    ctr: 3.0,
    cpc: 853,
    applications: 23,
    startDate: "2026-01-15",
    endDate: "2026-02-15",
  },
  {
    id: 2,
    name: "백엔드 개발자 타겟 광고",
    status: "active",
    type: "targeted",
    budget: 300000,
    spent: 180000,
    impressions: 8200,
    clicks: 246,
    ctr: 3.0,
    cpc: 732,
    applications: 18,
    startDate: "2026-01-20",
    endDate: "2026-02-20",
  },
  {
    id: 3,
    name: "데이터 엔지니어 기본 노출",
    status: "paused",
    type: "basic",
    budget: 100000,
    spent: 45000,
    impressions: 3100,
    clicks: 62,
    ctr: 2.0,
    cpc: 726,
    applications: 5,
    startDate: "2026-01-10",
    endDate: "2026-02-10",
  },
];

const statusConfig = {
  active: { label: "진행중", className: "bg-success/10 text-success border-success/20" },
  paused: { label: "일시중지", className: "bg-warning/10 text-warning border-warning/20" },
  ended: { label: "종료", className: "bg-muted text-muted-foreground border-border" },
};

const typeConfig = {
  premium: { label: "프리미엄", className: "bg-accent/10 text-accent border-accent/20" },
  targeted: { label: "타겟 광고", className: "bg-info/10 text-info border-info/20" },
  basic: { label: "기본", className: "bg-muted text-muted-foreground border-border" },
};

export function AdAnalyticsTab() {
  const totalBudget = mockCampaigns.reduce((acc, c) => acc + c.budget, 0);
  const totalSpent = mockCampaigns.reduce((acc, c) => acc + c.spent, 0);
  const totalImpressions = mockCampaigns.reduce((acc, c) => acc + c.impressions, 0);
  const totalClicks = mockCampaigns.reduce((acc, c) => acc + c.clicks, 0);
  const totalApplications = mockCampaigns.reduce((acc, c) => acc + c.applications, 0);
  const avgCtr = ((totalClicks / totalImpressions) * 100).toFixed(1);

  return (
    <div className="space-y-6">
      {/* 전체 통계 */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">총 예산</span>
              <DollarSign className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-foreground">
              {(totalBudget / 10000).toFixed(0)}만원
            </p>
            <Progress value={(totalSpent / totalBudget) * 100} className="h-1.5 mt-2" />
            <p className="text-xs text-muted-foreground mt-1">
              {((totalSpent / totalBudget) * 100).toFixed(0)}% 사용
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">노출수</span>
              <Eye className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-foreground">
              {totalImpressions.toLocaleString()}
            </p>
            <div className="flex items-center gap-1 text-xs text-success mt-1">
              <ArrowUpRight className="h-3 w-3" />
              <span>+12.5% vs 지난주</span>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">클릭수</span>
              <MousePointer className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-foreground">
              {totalClicks.toLocaleString()}
            </p>
            <div className="flex items-center gap-1 text-xs text-success mt-1">
              <ArrowUpRight className="h-3 w-3" />
              <span>+8.3% vs 지난주</span>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">클릭률 (CTR)</span>
              <Target className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-foreground">{avgCtr}%</p>
            <div className="flex items-center gap-1 text-xs text-destructive mt-1">
              <ArrowDownRight className="h-3 w-3" />
              <span>-0.2% vs 지난주</span>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">광고 지원자</span>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </div>
            <p className="text-2xl font-bold text-primary">{totalApplications}</p>
            <div className="flex items-center gap-1 text-xs text-success mt-1">
              <ArrowUpRight className="h-3 w-3" />
              <span>+15% vs 지난주</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 캠페인 목록 */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-primary" />
            광고 캠페인
          </CardTitle>
          <Button size="sm" className="btn-gradient-primary">
            <Plus className="h-4 w-4 mr-1" />
            새 캠페인
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {mockCampaigns.map((campaign) => (
            <div
              key={campaign.id}
              className="p-4 rounded-xl border border-border bg-card hover:shadow-card-hover transition-all"
            >
              <div className="flex flex-col lg:flex-row gap-4">
                {/* 캠페인 정보 */}
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <h3 className="font-semibold text-foreground">{campaign.name}</h3>
                    <Badge variant="outline" className={statusConfig[campaign.status as keyof typeof statusConfig].className}>
                      {statusConfig[campaign.status as keyof typeof statusConfig].label}
                    </Badge>
                    <Badge variant="outline" className={typeConfig[campaign.type as keyof typeof typeConfig].className}>
                      {typeConfig[campaign.type as keyof typeof typeConfig].label}
                    </Badge>
                  </div>
                  <div className="flex flex-wrap gap-3 text-sm text-muted-foreground mb-3">
                    <span className="flex items-center gap-1">
                      <Calendar className="h-3.5 w-3.5" />
                      {campaign.startDate} ~ {campaign.endDate}
                    </span>
                  </div>
                  {/* 예산 진행률 */}
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs">
                      <span className="text-muted-foreground">예산 사용</span>
                      <span className="font-medium text-foreground">
                        {(campaign.spent / 10000).toFixed(1)}만원 / {(campaign.budget / 10000).toFixed(0)}만원
                      </span>
                    </div>
                    <Progress value={(campaign.spent / campaign.budget) * 100} className="h-2" />
                  </div>
                </div>

                {/* 성과 지표 */}
                <div className="grid grid-cols-4 gap-4 lg:w-96">
                  <div className="text-center p-2 rounded-lg bg-muted/50">
                    <p className="text-lg font-bold text-foreground">{campaign.impressions.toLocaleString()}</p>
                    <p className="text-xs text-muted-foreground">노출</p>
                  </div>
                  <div className="text-center p-2 rounded-lg bg-muted/50">
                    <p className="text-lg font-bold text-foreground">{campaign.clicks}</p>
                    <p className="text-xs text-muted-foreground">클릭</p>
                  </div>
                  <div className="text-center p-2 rounded-lg bg-muted/50">
                    <p className="text-lg font-bold text-foreground">{campaign.ctr}%</p>
                    <p className="text-xs text-muted-foreground">CTR</p>
                  </div>
                  <div className="text-center p-2 rounded-lg bg-primary/5 border border-primary/20">
                    <p className="text-lg font-bold text-primary">{campaign.applications}</p>
                    <p className="text-xs text-muted-foreground">지원</p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
