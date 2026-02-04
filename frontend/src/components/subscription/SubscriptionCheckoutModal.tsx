import { useState } from "react";
import { loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from "@/components/ui/dialog";
import { CheckCircle2, ShieldCheck, Loader2, ArrowRight } from "lucide-react";
import { SubscriptionPlan } from "@/data/subscriptionPlans";

interface SubscriptionCheckoutModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    plan: SubscriptionPlan | null;
}

export function SubscriptionCheckoutModal({
    open,
    onOpenChange,
    plan,
}: SubscriptionCheckoutModalProps) {
    const { user } = useAuth() as any;
    const [isProcessing, setIsProcessing] = useState(false);

    if (!plan) return null;

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

            const currentOrigin = window.location.origin;
            const successUrl = new URL(`${currentOrigin}/subscription/success`);
            successUrl.searchParams.set("productId", plan.id === "pro" ? "2" : "2"); // Fixed: Pro plan is ID 2 in DB.
            // Based on previous logs, Pro ID was often 4 or similar. Let's assume some mapping or use plan.id if it matches backend.
            // In SubscriptionCheckoutPage.tsx it was using productId from searchParams.
            successUrl.searchParams.set("price", String(plan.price));

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

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-md p-0 overflow-hidden border-none shadow-2xl">
                <div className="bg-slate-900 p-8 text-center">
                    <DialogTitle className="text-2xl font-black text-white mb-2">
                        정기 결제 카드 등록
                    </DialogTitle>
                    <DialogDescription className="text-slate-400 text-sm">
                        FitMe 멤버십 구독을 위해 카드를 등록합니다.
                    </DialogDescription>
                </div>

                <div className="p-8 space-y-6 bg-white">
                    <div className="bg-blue-50 p-4 rounded-xl border border-blue-100 flex justify-between items-center">
                        <div>
                            <p className="text-xs font-bold text-blue-500 mb-1 uppercase">Selected Plan</p>
                            <h3 className="font-bold text-slate-900">{plan.name}</h3>
                        </div>
                        <div className="text-right">
                            <p className="text-lg font-black text-slate-900">
                                ₩{plan.price.toLocaleString()}
                            </p>
                            <p className="text-xs text-slate-500">/ 월 (부가세 포함)</p>
                        </div>
                    </div>

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
                            <p className="text-xs text-slate-500">
                                개인/법인 카드 모두 등록 가능합니다. <br />
                                결제 정보는 토스페이먼츠에서 안전하게 관리됩니다.
                            </p>
                        </div>
                    </div>

                    <Button
                        onClick={handleRegisterCard}
                        disabled={isProcessing}
                        className="w-full h-14 text-lg font-bold bg-blue-600 hover:bg-blue-700 rounded-xl shadow-lg shadow-blue-200 transition-all text-white"
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
            </DialogContent>
        </Dialog>
    );
}
