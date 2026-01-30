import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { confirmPayment } from "@/api/payment";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { CheckCircle2, Loader2 } from "lucide-react";
import { Card } from "@/components/ui/card";

export default function PaymentSuccessPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { user, loading, refreshCredits } = useAuth() as any;
    const [isProcessing, setIsProcessing] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Toss Payments returns: paymentKey, orderId, amount
    const paymentKey = searchParams.get("paymentKey");
    const orderId = searchParams.get("orderId");
    const amount = Number(searchParams.get("amount") || 0);

    // 결제 확인 useEffect - 모든 hooks는 조건부 return 전에 선언해야 함
    useEffect(() => {
        // Auth 로딩 중이면 대기
        if (loading) return;

        // 인증되지 않은 경우 대기 (UI에서 처리)
        if (!user) return;

        const verifyPayment = async () => {
            if (!paymentKey || !orderId || !amount) {
                navigate("/settings?tab=history");
                return;
            }

            try {
                // Call real backend confirmation
                await confirmPayment({
                    paymentKey,
                    orderId,
                    amount
                });

                // Refresh credits on success
                await refreshCredits();

                // Redirect back to settings with success flag
                navigate(`/settings?tab=history&payment_success=true&orderId=${orderId}&amount=${amount}`, { replace: true });
            } catch (err: any) {
                console.error("Payment confirmation failed:", err);
                const errorMsg = err.response?.data?.message || "결제 확인 중 오류가 발생했습니다. 고객센터에 문의해주세요.";
                setError(errorMsg);
                setIsProcessing(false);
            }
        };

        verifyPayment();
    }, [loading, user, paymentKey, orderId, amount, navigate, refreshCredits]);

    // Auth 로딩 중이면 로딩 화면 표시
    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
                <Card className="w-full max-w-md p-8 text-center">
                    <Loader2 className="h-12 w-12 animate-spin text-blue-600 mx-auto mb-4" />
                    <p className="text-slate-500 font-medium">인증 정보를 확인하고 있습니다...</p>
                </Card>
            </div>
        );
    }

    // 인증되지 않은 경우 로그인 안내
    if (!user) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
                <Card className="w-full max-w-md p-8 text-center space-y-4">
                    <p className="text-slate-500">로그인이 필요합니다.</p>
                    <Button onClick={() => navigate("/auth")} className="w-full">
                        로그인하기
                    </Button>
                </Card>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
            <Card className="w-full max-w-md p-8 text-center space-y-6">
                {isProcessing ? (
                    <div className="py-12 flex flex-col items-center gap-4">
                        <Loader2 className="h-12 w-12 animate-spin text-blue-600" />
                        <p className="text-slate-500 font-medium">결제 정보를 확인하고 있습니다...</p>
                    </div>
                ) : error ? (
                    <div className="py-8 space-y-4">
                        <div className="flex justify-center">
                            <div className="h-16 w-16 bg-red-100 rounded-full flex items-center justify-center">
                                <span className="text-2xl text-red-600 font-bold">!</span>
                            </div>
                        </div>
                        <h2 className="text-xl font-bold text-slate-900">결제 처리 오류</h2>
                        <p className="text-slate-500 whitespace-pre-wrap">{error}</p>
                        <Button
                            variant="outline"
                            onClick={() => navigate("/settings?tab=history")}
                            className="w-full"
                        >
                            내역으로 이동
                        </Button>
                    </div>
                ) : (
                    <>
                        <div className="flex justify-center">
                            <div className="h-20 w-20 bg-emerald-100 rounded-full flex items-center justify-center animate-in zoom-in duration-300">
                                <CheckCircle2 className="h-10 w-10 text-emerald-600" />
                            </div>
                        </div>

                        <div className="space-y-2">
                            <h1 className="text-2xl font-bold text-slate-900">결제가 완료되었습니다!</h1>
                            <p className="text-slate-500">
                                주문번호: {orderId}<br />
                                결제금액: {Number(amount).toLocaleString()}원
                            </p>
                            <p className="text-sm font-bold text-blue-600 mt-2">
                                크레딧이 성공적으로 충전되었습니다.
                            </p>
                        </div>

                        <div className="pt-4">
                            <Button
                                onClick={() => navigate("/settings?tab=history")}
                                className="w-full h-11 bg-blue-600 hover:bg-blue-700 text-lg font-bold"
                            >
                                확인
                            </Button>
                        </div>
                    </>
                )}
            </Card>
        </div>
    );
}
