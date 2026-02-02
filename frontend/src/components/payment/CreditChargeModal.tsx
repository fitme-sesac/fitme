import { useState, useEffect } from "react";
import { loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { createPayment } from "@/api/payment";
import {
    Dialog,
    DialogContent,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Coins, CheckCircle2, ShieldCheck, X } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useSearchParams } from "react-router-dom";
import { cn } from "@/lib/utils";

interface CreditOption {
    id: string;
    credits: number;
    price: number;
    bonus?: number;
    isPopular?: boolean;
}

// TODO: 상품 정보는 백엔드 API(/api/v1/products)에서 조회하도록 개선 필요
// 현재는 백엔드의 Product 테이블과 동기화되어야 함
const CREDIT_OPTIONS: CreditOption[] = [
    { id: "1", credits: 1000, price: 11000 },
    { id: "2", credits: 5000, price: 55000, bonus: 250 },
    { id: "3", credits: 10000, price: 110000, bonus: 1000, isPopular: true },
    { id: "4", credits: 30000, price: 330000, bonus: 4500 },
    { id: "5", credits: 50000, price: 550000, bonus: 10000 },
    { id: "6", credits: 100000, price: 990000, bonus: 25000 },
];

export function CreditChargeModal({
    open,
    onOpenChange
}: {
    open: boolean;
    onOpenChange: (open: boolean) => void
}) {
    const { user, isCompany } = useAuth() as any;
    const [selectedId, setSelectedId] = useState<string>("3");
    const [searchParams, setSearchParams] = useSearchParams();
    const isSuccess = searchParams.get("payment_success") === "true";
    const successOrderId = searchParams.get("orderId");
    const successAmount = searchParams.get("amount");

    const [isCharging, setIsCharging] = useState(false);
    const [paymentMethod, setPaymentMethod] = useState("SIMPLE"); // SIMPLE, CARD, PHONE, TRANSFER, GIFT
    const [simplePayType, setSimplePayType] = useState<string | null>(null);
    const [agreed, setAgreed] = useState(false);

    const selectedOption = CREDIT_OPTIONS.find(opt => opt.id === selectedId) || CREDIT_OPTIONS[2];
    const totalCredits = selectedOption.credits + (selectedOption.bonus || 0);

    const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
    const customerKey = user?.id || user?.email?.replace(/[^a-zA-Z0-9]/g, "") || "ANONYMOUS";

    const handleCharge = async () => {
        if (!agreed) {
            alert("구매 조건 및 결제 진행 동의가 필요합니다.");
            return;
        }

        console.log("handleCharge triggered", { isCharging, price: selectedOption.price, customerKey });
        setIsCharging(true);

        try {
            const productCodeMapValue: Record<number, string> = {
                1000: "CREDIT_1000",
                5000: "CREDIT_5000",
                10000: "CREDIT_10000",
                30000: "CREDIT_30000",
                50000: "CREDIT_50000",
                100000: "CREDIT_100000",
            };

            const productCode = productCodeMapValue[selectedOption.credits] || "CREDIT_GENERIC";

            let backendOrder;
            try {
                backendOrder = await createPayment({
                    amount: selectedOption.price,
                    orderName: `${selectedOption.credits.toLocaleString()} 크레딧 충전`,
                    buyerType: isCompany ? "EMPLOYER" : "MEMBER",
                    productCode: productCode,
                    idempotencyKey: `ORDER-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
                });
            } catch (err: any) {
                console.error("Backend payment creation failed:", err);
                throw new Error(err.response?.data?.message || "서버 결제 요청 생성에 실패했습니다.");
            }

            const orderId = backendOrder.orderId;
            console.log("Backend Order Created:", orderId);

            // 프로덕션에서는 반드시 Toss Client Key가 설정되어 있어야 함
            if (!clientKey) {
                throw new Error("결제 설정이 올바르지 않습니다. 관리자에게 문의하세요.");
            }

            // Toss Payments 결제 요청
            const tossPayments = await loadTossPayments(clientKey);
            const payment = tossPayments.payment({ customerKey });

            // method mapping based on tabs/selection
            let method = "CARD";

            if (paymentMethod === "SIMPLE") {
                if (simplePayType === "kakao") {
                    method = "CARD";
                }
            } else if (paymentMethod === "PHONE") {
                method = "MOBILE_PHONE";
            } else if (paymentMethod === "TRANSFER") {
                method = "TRANSFER";
            } else if (paymentMethod === "GIFT") {
                method = "GIFT_CERTIFICATE";
            }

            await payment.requestPayment({
                method: method as any,
                amount: {
                    value: selectedOption.price,
                    currency: "KRW",
                },
                orderId: orderId,
                orderName: `${selectedOption.credits.toLocaleString()} 크레딧 충전`,
                customerEmail: user?.email,
                customerName: user?.user_metadata?.display_name || "사용자",
                successUrl: `${window.location.origin}/payment/success`,
                failUrl: `${window.location.origin}/payment/fail`,
                card: {
                    useEscrow: false,
                    flowMode: "DEFAULT",
                    useCardPoint: false,
                    useAppCardOnly: false,
                },
            });

        } catch (error: any) {
            console.error("Payment Error:", error);
            setIsCharging(false);
            alert(`결제 오류: ${error?.message || "결제 진행 중 오류가 발생했습니다."}`);
        }
    };

    const handleClose = () => {
        if (isSuccess) {
            const newParams = new URLSearchParams(searchParams);
            newParams.delete("payment_success");
            newParams.delete("orderId");
            newParams.delete("amount");
            setSearchParams(newParams, { replace: true });
        }
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={handleClose}>
            <DialogContent className={cn(
                "max-w-5xl p-0 overflow-hidden border-none bg-white rounded-2xl shadow-2xl transition-all [&>button]:hidden",
                isSuccess ? "max-w-md" : "w-[95vw] max-h-[90vh] h-[750px] flex flex-col"
            )}>
                {isSuccess ? (
                    <div className="p-10 text-center space-y-8 bg-white">
                        <div className="flex justify-center">
                            <div className="h-24 w-24 bg-emerald-50 rounded-full flex items-center justify-center animate-in zoom-in duration-500">
                                <CheckCircle2 className="h-12 w-12 text-emerald-500" />
                            </div>
                        </div>
                        <div className="space-y-2">
                            <h2 className="text-2xl font-bold text-slate-900">충전이 완료되었습니다!</h2>
                            <p className="text-slate-500">지금 바로 서비스를 이용해 보세요.</p>
                        </div>
                        <div className="pt-4">
                            <Button onClick={handleClose} className="w-full bg-blue-600 hover:bg-blue-700 text-lg py-6 rounded-xl">확인</Button>
                        </div>
                    </div>
                ) : (
                    <div className="flex flex-row h-full overflow-hidden">
                        {/* Left Panel - Fixed width, scrollable content if needed, basically a sidebar */}
                        <div className="hidden md:flex flex-col w-[280px] bg-slate-50 p-6 border-r border-slate-100 shrink-0">
                            {/* Blue Gradient Icon */}
                            <div className="flex justify-center mb-6">
                                <div className="h-20 w-20 rounded-3xl bg-gradient-to-br from-blue-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-blue-200">
                                    <Coins className="h-9 w-9 text-white" />
                                </div>
                            </div>

                            <h2 className="text-xl font-black text-center text-slate-800 mb-2">크레딧 충전하기</h2>
                            <div className="h-1 w-8 bg-blue-500 rounded-full mx-auto mb-8" />

                            <div className="space-y-1 mb-6">
                                <p className="text-xs font-bold text-slate-400">결제 알림 계정</p>
                                <p className="text-sm font-medium text-slate-600 truncate">{user?.email}</p>
                            </div>

                            <div className="space-y-4">
                                <div className="flex justify-between items-baseline">
                                    <span className="text-sm font-bold text-slate-500">충전 예정</span>
                                    <div className="text-right">
                                        <span className="text-2xl font-black text-blue-600">+{totalCredits.toLocaleString()}</span>
                                        <span className="text-[10px] text-blue-400 block font-bold">CREDITS</span>
                                    </div>
                                </div>
                                {selectedOption.bonus && (
                                    <div className="flex justify-between items-baseline text-xs font-bold text-emerald-500">
                                        <span>보너스 합산됨</span>
                                        <span>+{selectedOption.bonus.toLocaleString()}</span>
                                    </div>
                                )}
                            </div>

                            <div className="mt-auto">
                                <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
                                    <div className="flex items-center gap-2 text-[10px] font-bold text-slate-400 mb-2">
                                        <ShieldCheck className="h-3 w-3" />
                                        보안 결제 시스템 작동 중
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="font-bold text-slate-500 text-sm">결제 금액</span>
                                        <span className="text-lg font-black text-slate-900">{selectedOption.price.toLocaleString()}원</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Right Panel - Scrollable content area */}
                        <div className="flex-1 flex flex-col h-full bg-white min-w-0">
                            {/* Header */}
                            <div className="flex justify-between items-start p-6 pb-2 shrink-0">
                                <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                    <span className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 text-xs font-black text-white">1</span>
                                    충전 금액 선택
                                </h3>
                                <button onClick={() => onOpenChange(false)} className="text-slate-400 hover:text-slate-600">
                                    <X className="h-6 w-6" />
                                </button>
                            </div>

                            {/* Scrollable Content */}
                            <div className="flex-1 overflow-y-auto p-6 pt-2">
                                {/* Step 1: Grid */}
                                <div className="grid grid-cols-2 lg:grid-cols-3 gap-3 mb-8">
                                    {CREDIT_OPTIONS.map((opt) => (
                                        <div
                                            key={opt.id}
                                            onClick={() => setSelectedId(opt.id)}
                                            className={cn(
                                                "relative cursor-pointer rounded-xl border-2 p-4 transition-all duration-200 hover:shadow-md",
                                                selectedId === opt.id
                                                    ? "border-blue-500 bg-white ring-2 ring-blue-50/50"
                                                    : "border-slate-100 bg-white hover:border-slate-200"
                                            )}
                                        >
                                            {opt.isPopular && (
                                                <div className="absolute top-0 right-0 -mt-2 -mr-2 bg-blue-100 text-blue-600 text-[9px] font-bold px-2 py-0.5 rounded-full border border-blue-200">
                                                    인기
                                                </div>
                                            )}
                                            {selectedId === opt.id && (
                                                <div className="absolute top-2 right-2 text-blue-500">
                                                    <CheckCircle2 className="h-4 w-4 fill-blue-50" />
                                                </div>
                                            )}
                                            <div className="space-y-0.5">
                                                <p className="text-[9px] font-bold text-slate-400 uppercase tracking-wide">CREDITS</p>
                                                <p className="text-xl font-black text-slate-800">{opt.credits.toLocaleString()}</p>
                                                <p className="text-sm font-medium text-slate-500">{opt.price.toLocaleString()}원</p>
                                            </div>
                                            {opt.bonus && (
                                                <div className="mt-2 inline-flex items-center rounded-md bg-emerald-500 px-1.5 py-0.5 text-[9px] font-bold text-white">
                                                    +{opt.bonus.toLocaleString()} 보너스
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>

                                {/* Step 2: Payment Method */}
                                <div className="mb-6">
                                    <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 mb-4">
                                        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 text-xs font-black text-white">2</span>
                                        결제 수단 선택
                                    </h3>

                                    <Tabs defaultValue="SIMPLE" onValueChange={setPaymentMethod} className="w-full">
                                        <TabsList className="w-full grid grid-cols-5 p-1 bg-slate-100/80 rounded-xl mb-4 h-11">
                                            <TabsTrigger value="SIMPLE" className="rounded-lg font-bold text-[10px] md:text-xs lg:text-sm data-[state=active]:bg-white data-[state=active]:text-blue-600 data-[state=active]:shadow-sm">간편결제</TabsTrigger>
                                            <TabsTrigger value="CARD" className="rounded-lg font-bold text-[10px] md:text-xs lg:text-sm data-[state=active]:bg-white data-[state=active]:text-blue-600 data-[state=active]:shadow-sm">신용카드</TabsTrigger>
                                            <TabsTrigger value="PHONE" className="rounded-lg font-bold text-[10px] md:text-xs lg:text-sm data-[state=active]:bg-white data-[state=active]:text-blue-600 data-[state=active]:shadow-sm">휴대폰</TabsTrigger>
                                            <TabsTrigger value="TRANSFER" className="rounded-lg font-bold text-[10px] md:text-xs lg:text-sm data-[state=active]:bg-white data-[state=active]:text-blue-600 data-[state=active]:shadow-sm">계좌이체</TabsTrigger>
                                            <TabsTrigger value="GIFT" className="rounded-lg font-bold text-[10px] md:text-xs lg:text-sm data-[state=active]:bg-white data-[state=active]:text-blue-600 data-[state=active]:shadow-sm">상품권</TabsTrigger>
                                        </TabsList>

                                        <TabsContent value="SIMPLE" className="mt-0 space-y-4">
                                            <div className="bg-slate-50 p-6 rounded-xl border border-slate-100">
                                                <div className="grid grid-cols-3 gap-3">
                                                    <button
                                                        onClick={() => setSimplePayType("kakaopay")}
                                                        className={cn(
                                                            "flex items-center justify-center gap-2 p-4 rounded-xl border-2 transition-all bg-white hover:bg-yellow-50/50",
                                                            simplePayType === "kakaopay" ? "border-yellow-400 bg-yellow-50 ring-2 ring-yellow-100" : "border-slate-100"
                                                        )}
                                                    >
                                                        <span className="font-bold text-slate-800 text-sm md:text-base">kakaopay</span>
                                                    </button>
                                                    <button
                                                        onClick={() => setSimplePayType("toss")}
                                                        className={cn(
                                                            "flex items-center justify-center gap-2 p-4 rounded-xl border-2 transition-all bg-white hover:bg-blue-50/50",
                                                            simplePayType === "toss" ? "border-blue-400 bg-blue-50 ring-2 ring-blue-100" : "border-slate-100"
                                                        )}
                                                    >
                                                        <span className="font-bold text-slate-800 text-sm md:text-base">Toss</span>
                                                    </button>
                                                    <button
                                                        onClick={() => setSimplePayType("payco")}
                                                        className={cn(
                                                            "flex items-center justify-center gap-2 p-4 rounded-xl border-2 transition-all bg-white hover:bg-red-50/50",
                                                            simplePayType === "payco" ? "border-red-400 bg-red-50 ring-2 ring-red-100" : "border-slate-100"
                                                        )}
                                                    >
                                                        <span className="font-bold text-slate-800 text-sm md:text-base">PAYCO</span>
                                                    </button>
                                                </div>
                                            </div>
                                        </TabsContent>
                                        <TabsContent value="CARD">
                                            <div className="p-6 text-center bg-slate-50 rounded-xl border border-dashed border-slate-300">
                                                <p className="text-slate-500 text-xs">결제하기 버튼을 누르면 카드사 결제창이 열립니다.</p>
                                            </div>
                                        </TabsContent>
                                        {/* Other tabs placeholders */}
                                    </Tabs>
                                </div>

                                {/* Footer Section embedded in scroll area to avoid cutoff */}
                                <div className="mt-4 pt-4 border-t border-slate-100">
                                    <div className="flex items-start gap-3 mb-4">
                                        <Checkbox
                                            id="terms"
                                            checked={agreed}
                                            onCheckedChange={(c) => setAgreed(!!c)}
                                            className="mt-0.5"
                                        />
                                        <div className="space-y-0.5">
                                            <label
                                                htmlFor="terms"
                                                className="text-sm font-bold text-slate-800 leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                                            >
                                                구매 조건 확인 및 결제 진행 동의
                                            </label>
                                            <p className="text-xs text-slate-500 font-medium">
                                                충전된 크레딧은 채용 서비스 유료 기능 이용에 사용되며, 환불 규정은 이용약관을 따릅니다.
                                            </p>
                                        </div>
                                    </div>

                                    <Button
                                        onClick={handleCharge}
                                        disabled={isCharging}
                                        className={cn(
                                            "w-full h-12 text-base font-black rounded-xl transition-all shadow-lg shadow-blue-100",
                                            "bg-blue-600 hover:bg-blue-700 text-white"
                                        )}
                                    >
                                        {isCharging ? "결제 처리 중..." : "결제하기"}
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
}
