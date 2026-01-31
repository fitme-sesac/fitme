import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";

export default function Companies() {
  const navigate = useNavigate();

  return (
    <div className="container mx-auto max-w-5xl py-10 px-4">
      <h1 className="text-3xl font-bold mb-3">기업</h1>
      <p className="text-muted-foreground mb-6">
        기업 관련 페이지 진입점입니다. 아래 메뉴로 이동하세요.
      </p>

      <div className="flex flex-wrap gap-3">
        <Button onClick={() => navigate("/companies/1")}>기업 상세(예시)</Button>
        <Button variant="outline" onClick={() => navigate("/company/dashboard")}>
          기업 대시보드
        </Button>
      </div>
    </div>
  );
}
