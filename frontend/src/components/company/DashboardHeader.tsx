import { Building2, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

export function DashboardHeader() {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div className="flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
          <Building2 className="h-6 w-6 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-foreground">기업 대시보드</h1>
          <p className="text-muted-foreground">채용공고와 지원자를 효율적으로 관리하세요</p>
        </div>
      </div>
      <Button className="btn-gradient-primary">
        <Plus className="h-4 w-4 mr-2" />
        새 채용공고 등록
      </Button>
    </div>
  );
}
