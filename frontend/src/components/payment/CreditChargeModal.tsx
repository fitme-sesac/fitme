import { useState, useEffect, useRef } from "react";
import { loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { createPayment } from "@/api/payment";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Coins, CreditCard, Smartphone, Banknote, ShieldCheck, CheckCircle2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useNavigate, useSearchParams } from "react-router-dom";
import { cn } from "@/lib/utils";

interface CreditOption {
    id: string;
    credits: number;
    price: number;
    bonus?: number;
}

const CREDIT_OPTIONS: CreditOption[] = [
    { id: "1", credits: 1000, price: 11000 },
    { id: "2", credits: 5000, price: 55000, bonus: 250 },
    { id: "3", credits: 10000, price: 110000, bonus: 1000 },
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

    const selectedOption = CREDIT_OPTIONS.find(opt => opt.id === selectedId) || CREDIT_OPTIONS[2];
    const totalCredits = selectedOption.credits + (selectedOption.bonus || 0);

    const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
    const customerKey = user?.id || user?.email?.replace(/[^a-zA-Z0-9]/g, "") || "ANONYMOUS";

    const handleCharge = async () => {
        console.log("handleCharge (Standard-Card) triggered", { isCharging, price: selectedOption.price, customerKey });
        setIsCharging(true);

        try {
            // ... (Backend order creation logic)
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

            // 2. Dummy Simulation
            if (!clientKey || clientKey === "test") {
                window.location.href = `${window.location.origin}/payment/success?paymentKey=mock_${Date.now()}&orderId=${orderId}&amount=${selectedOption.price}`;
                return;
            }

            // 3. Request Payment using Standard Flow (v2 Final Correct Syntax)
            const tossPayments = await loadTossPayments(clientKey);
            const payment = tossPayments.payment({ customerKey });

            await payment.requestPayment({
                method: "CARD",
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
            // Remove success params from URL on close
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
                "max-w-4xl p-0 overflow-hidden border-none bg-white rounded-xl shadow-2xl",
                isSuccess && "max-w-md" // Success view is more compact
            )}>
                {isSuccess ? (
                    <div className="p-10 text-center space-y-8 bg-white rounded-xl">
                        <div className="flex justify-center">
                            <div className="h-24 w-24 bg-emerald-50 rounded-full flex items-center justify-center animate-in zoom-in duration-500 ring-8 ring-emerald-50/50">
                                <CheckCircle2 className="h-12 w-12 text-emerald-500" />
                            </div>
                        </div>

                        <div className="space-y-3">
                            <h2 className="text-3xl font-black text-slate-900 tracking-tight">충전이 완료되었습니다!</h2>
                            <p className="text-slate-500 font-medium leading-relaxed">
                                선택하신 상품이 성공적으로 반영되었습니다.<br />
                                지금 바로 FitMe 서비스를 이용해 보세요.
                            </p>
                        </div>

                        <div className="bg-slate-50 rounded-2xl p-6 text-left space-y-4 border border-slate-100 shadow-inner">
                            <div className="flex justify-between items-center pb-3 border-b border-slate-200/50">
                                <span className="text-sm font-bold text-slate-400">주문 번호</span>
                                <span className="text-slate-700 font-mono text-[11px] font-bold bg-white px-2 py-1 rounded-md border">{successOrderId}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-sm font-bold text-slate-400">최종 결제 금액</span>
                                <span className="text-2xl font-black text-blue-600">{Number(successAmount).toLocaleString()}원</span>
                            </div>
                        </div>

                        <div className="pt-4">
                            <Button
                                onClick={handleClose}
                                className="w-full h-14 bg-slate-900 hover:bg-slate-800 text-white font-black text-lg rounded-xl shadow-xl shadow-slate-200 transition-all active:scale-95"
                            >
                                확인
                            </Button>
                        </div>
                    </div>
                ) : (
                    <div className="flex flex-col md:flex-row h-full">
                        {/* Left Panel: Summary (Riot Style) */}
                        <div className="w-full md:w-[320px] bg-slate-50 p-8 border-r border-slate-100 flex flex-col">
                            <div className="mb-8">
                                <div className="flex flex-col items-center text-center">
                                    <div className="h-20 w-20 rounded-2xl bg-gradient-to-br from-blue-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-blue-200 mb-4 transform -rotate-3">
                                        <Coins className="h-10 w-10 text-white" />
                                    </div>
                                    <h2 className="text-2xl font-black text-slate-800 tracking-tight">크레딧 충전하기</h2>
                                    <div className="h-1 w-12 bg-blue-600 rounded-full mt-2" />
                                </div>
                            </div>

                            <div className="space-y-6 flex-1">
                                <div className="space-y-2">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">결제 알림 계정</p>
                                    <p className="text-sm font-bold text-slate-700 truncate">{user?.email}</p>
                                </div>

                                <div className="space-y-4 pt-4 border-t border-slate-200">
                                    <div className="flex justify-between items-end">
                                        <p className="text-sm font-bold text-slate-500">충전 예정</p>
                                        <div className="text-right">
                                            <p className="text-2xl font-black text-blue-600 leading-none">+{totalCredits.toLocaleString()}</p>
                                            <p className="text-[10px] font-bold text-blue-400 mt-1 uppercase">Credits</p>
                                        </div>
                                    </div>
                                    {selectedOption.bonus && (
                                        <div className="flex justify-between text-xs font-bold">
                                            <span className="text-emerald-500">보너스 합산됨</span>
                                            <span className="text-emerald-500">+{selectedOption.bonus.toLocaleString()}</span>
                                        </div>
                                    )}
                                </div>

                                <div className="mt-auto pt-8">
                                    <div className="p-4 bg-white rounded-xl border border-slate-200 shadow-sm space-y-3">
                                        <div className="flex items-center gap-2 text-[10px] font-bold text-slate-400">
                                            <ShieldCheck className="h-3 w-3" />
                                            보안 결제 시스템 작동 중
                                        </div>
                                        <div className="flex justify-between items-center text-sm">
                                            <span className="font-bold text-slate-500">결제 금액</span>
                                            <span className="text-xl font-black text-slate-900">{selectedOption.price.toLocaleString()}원</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Right Panel: Selection Area */}
                        <div className="flex-1 p-8 overflow-y-auto max-h-[85vh]">
                            <div className="space-y-8">
                                {/* Step 1: Amount Selection */}
                                <section className="space-y-4">
                                    <div className="flex items-center justify-between">
                                        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                            <span className="h-6 w-6 rounded-full bg-slate-900 text-white text-xs flex items-center justify-center font-black">1</span>
                                            충전 금액 선택
                                        </h3>
                                    </div>

                                    <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
                                        {CREDIT_OPTIONS.map((option) => (
                                            <div
                                                key={option.id}
                                                onClick={() => setSelectedId(option.id)}
                                                className={cn(
                                                    "relative cursor-pointer group transition-all duration-200 rounded-xl border-2 p-4",
                                                    selectedId === option.id
                                                        ? "border-blue-600 bg-blue-50/30 ring-4 ring-blue-50"
                                                        : "border-slate-100 bg-white hover:border-slate-200 hover:shadow-md"
                                                )}
                                            >
                                                {selectedId === option.id && (
                                                    <CheckCircle2 className="absolute top-2 right-2 h-4 w-4 text-blue-600" />
                                                )}
                                                <div className="space-y-1">
                                                    <p className="text-[10px] font-bold text-slate-400 uppercase">Credits</p>
                                                    <p className="text-xl font-black text-slate-800">{option.credits.toLocaleString()}</p>
                                                    <p className="text-sm font-bold text-slate-500 group-hover:text-slate-700">{option.price.toLocaleString()}원</p>
                                                </div>
                                                {option.bonus && (
                                                    <Badge className="absolute -bottom-2 -right-2 bg-emerald-500 text-[10px] font-black border-2 border-white">
                                                        +{option.bonus.toLocaleString()} 보너스
                                                    </Badge>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </section>

                                {/* Step 2: Payment Method Selection */}
                                <section className="space-y-4">
                                    <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                        <span className="h-6 w-6 rounded-full bg-slate-900 text-white text-xs flex items-center justify-center font-black">2</span>
                                        결제 수단
                                    </h3>

                                    <div className="p-4 rounded-xl border-2 border-blue-600 bg-blue-50 flex items-center justify-between">
                                        <div className="flex items-center gap-3">
                                            <div className="h-10 w-10 rounded-lg bg-white shadow-sm flex items-center justify-center">
                                                <CreditCard className="h-6 w-6 text-blue-600" />
                                            </div>
                                            <div>
                                                <p className="font-bold text-slate-800 text-sm">신용/체크카드</p>
                                                <p className="text-[10px] text-slate-500 font-medium">토스페이먼츠 보안결제</p>
                                            </div>
                                        </div>
                                        <CheckCircle2 className="h-5 w-5 text-blue-600" />
                                    </div>
                                </section>

                                <div className="pt-4">
                                    <Button
                                        onClick={handleCharge}
                                        disabled={isCharging}
                                        className={cn(
                                            "w-full h-14 text-lg font-black tracking-tight rounded-xl transition-all duration-300",
                                            "bg-blue-600 hover:bg-blue-700 text-white shadow-xl shadow-blue-200 transform translate-y-0 active:translate-y-1"
                                        )}
                                    >
                                        {isCharging ? (
                                            <div className="flex items-center gap-2">
                                                <div className="h-5 w-5 border-4 border-white border-t-transparent rounded-full animate-spin" />
                                                결제 처리 중...
                                            </div>
                                        ) : (
                                            "결제하기"
                                        )}
                                    </Button>
                                    <p className="text-[10px] text-center text-slate-400 mt-4">
                                        충전 된 크레딧은 채용 서비스 유료 기능 이용에 사용되며, 환불 규정은 이용약관을 따릅니다.
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
}
