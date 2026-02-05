import { useState, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import { loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { useAuth } from "@/contexts/AuthContext";
import { usePublicEmployer } from "@/hooks/useEmployers.js";
import { Button } from "@/components/ui/button";
import { CheckCircle2, ShieldCheck, Loader2, ArrowRight } from "lucide-react";
import { cn } from "@/lib/utils";

// Define local interface for AuthContext to get TS help if needed, 
// though JS context usage is loose.
interface AuthContextType {
    user: any;
    isCompany: boolean;
}

export default function SubscriptionCheckoutPage() {
    const { user, isCompany } = useAuth() as AuthContextType;
    const [searchParams] = useSearchParams();

    // Product info from URL (or we could fetch it)
    const productId = searchParams.get("productId") || "1"; // Default to basic plan
    const productName = searchParams.get("productName") || "FitMe 기업 멤버십";
    const price = Number(searchParams.get("price")) || 0;

    const [isProcessing, setIsProcessing] = useState(false);

    // In a real app, we should get the employerId more reliably. 
    // Here we assume the user IS an employer and get their linked employer profile.
    // If multiple companies, we might need a selection step.
    // For now, let's try to fetch the first employer associated with this member?
    // Or use the one from URL if provided.

    // Strategy: We will proceed with "Card Registration" first. 
    // The actual subscription creation happens in Success Page using the AuthKey.
    // However, we need 'customerKey' here to initiate the request.
    const customerKey = user?.id ? `USER-${user.id}` : `ANONYMOUS-${Date.now()}`;

    const handleRegisterCard = async () => {
        setIsProcessing(true);
        try {
            const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;

            if (!clientKey) {
                alert("토스 클라이언트 키가 설정되지 않았습니다.");
                return;
            }

            // @ts-ignore
            const tossPayments = await loadTossPayments(clientKey);
            // @ts-ignore
            const payment = tossPayments.payment({ customerKey });

            // Generate a redirect URL
            const currentOrigin = window.location.origin;
            const successUrl = new URL(`${currentOrigin}/subscription/success`);
            successUrl.searchParams.set("productId", productId);
            successUrl.searchParams.set("price", String(price));

            // Request Billing Auth (Auto Pay Registration)
            await payment.requestBillingAuth({
                method: "CARD",
                successUrl: successUrl.toString(),
                failUrl: `${currentOrigin}/subscription/fail`,
                customerEmail: user?.email,
                customerName: user?.username || user?.user_metadata?.display_name || "사용자",
            });

        } catch (error: any) {
            console.error("Billing Auth Failed:", error);
            alert("카드 등록 창을 여는 중 오류가 발생했습니다.");
            setIsProcessing(false);
        }
    };

    if (!user) {
        return <div className="p-10 text-center">로그인이 필요합니다.</div>;
    }

    return (
        <div className="min-h-screen bg-slate-50 flex flex-col justify-center items-center p-4">
            <div className="max-w-md w-full bg-white rounded-2xl shadow-xl overflow-hidden">
                {/* Header */}
                <div className="bg-slate-900 p-8 text-center">
                    <h1 className="text-2xl font-black text-white mb-2">정기 결제 카드 등록</h1>
                    <p className="text-slate-400 text-sm">FitMe 멤버십 구독을 위해 카드를 등록합니다.</p>
                </div>

                {/* Content */}
                <div className="p-8 space-y-6">
                    {/* Plan Info */}
                    <div className="bg-blue-50 p-4 rounded-xl border border-blue-100 flex justify-between items-center">
                        <div>
                            <p className="text-xs font-bold text-blue-500 mb-1 uppercase">Selected Plan</p>
                            <h3 className="font-bold text-slate-900">{productName}</h3>
                        </div>
                        <div className="text-right">
                            <p className="text-lg font-black text-slate-900">{price.toLocaleString()}원</p>
                            <p className="text-xs text-slate-500">/ 월 (부가세 포함)</p>
                        </div>
                    </div>

                    {/* Features/Notice */}
                    <div className="space-y-3">
                        <div className="flex items-start gap-3">
                            <CheckCircle2 className="h-5 w-5 text-emerald-500 shrink-0 mt-0.5" />
                            <p className="text-sm text-slate-600">등록하신 카드로 매월 자동 결제됩니다.</p>
                        </div>
                        <div className="flex items-start gap-3">
                            <CheckCircle2 className="h-5 w-5 text-emerald-500 shrink-0 mt-0.5" />
                            <p className="text-sm text-slate-600">언제든지 구독을 해지하실 수 있습니다.</p>
                        </div>
                        <div className="flex items-start gap-3">
                            <ShieldCheck className="h-5 w-5 text-slate-400 shrink-0 mt-0.5" />
                            <p className="text-xs text-slate-500">개인/법인 카드 모두 등록 가능합니다. <br />결제 정보는 토스페이먼츠에서 안전하게 관리됩니다.</p>
                        </div>
                    </div>

                    {/* Action Button */}
                    <Button
                        onClick={handleRegisterCard}
                        disabled={isProcessing}
                        className="w-full h-14 text-lg font-bold bg-blue-600 hover:bg-blue-700 rounded-xl shadow-lg shadow-blue-200 transition-all"
                    >
                        {isProcessing ? (
                            <>
                                <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                                처리 중...
                            </>
                        ) : (
                            <>
                                카드 등록하고 구독 시작하기
                                <ArrowRight className="ml-2 h-5 w-5" />
                            </>
                        )}
                    </Button>

                    <p className="text-center text-xs text-slate-400">
                        버튼을 누르면 토스페이먼츠 카드 등록 창이 열립니다.
                    </p>
                </div>
            </div>
        </div>
    );
}
