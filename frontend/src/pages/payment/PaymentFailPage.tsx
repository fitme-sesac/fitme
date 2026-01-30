import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { XCircle } from "lucide-react";
import { Card } from "@/components/ui/card";

export default function PaymentFailPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const code = searchParams.get("code");
    const message = searchParams.get("message");

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
            <Card className="w-full max-w-md p-8 text-center space-y-6">
                <div className="flex justify-center">
                    <div className="h-20 w-20 bg-red-100 rounded-full flex items-center justify-center">
                        <XCircle className="h-10 w-10 text-red-600" />
                    </div>
                </div>

                <div className="space-y-2">
                    <h1 className="text-2xl font-bold text-slate-900">결제에 실패했습니다</h1>
                    <p className="text-red-500 font-medium">
                        {message || "알 수 없는 오류가 발생했습니다."}
                    </p>
                    {code && <p className="text-xs text-slate-400">에러 코드: {code}</p>}
                </div>

                <div className="pt-4 flex gap-3">
                    <Button
                        variant="outline"
                        onClick={() => navigate(-1)}
                        className="flex-1 h-11"
                    >
                        이전으로
                    </Button>
                    <Button
                        onClick={() => navigate("/settings?tab=history")}
                        className="flex-1 h-11 bg-slate-900 text-white hover:bg-slate-800"
                    >
                        홈으로
                    </Button>
                </div>
            </Card>
        </div>
    );
}
