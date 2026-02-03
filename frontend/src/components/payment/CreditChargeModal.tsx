import { useState, useEffect } from "react";
import { useCreditPayment } from "@/features/payment/hooks/useCreditPayment";
import {
    Dialog,
    DialogContent,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Checkbox } from "@/components/ui/checkbox";
import { Coins, CheckCircle2, ShieldCheck, X, Loader2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { cn } from "@/lib/utils";
import { fetchProducts, Product } from "@/api/product";
import { PaymentProduct } from "@/features/payment/hooks/useCreditPayment";

export function CreditChargeModal({
    open,
    onOpenChange
}: {
    open: boolean;
    onOpenChange: (open: boolean) => void
}) {
    const { user } = useAuth() as any;
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [products, setProducts] = useState<Product[]>([]);
    const [isLoadingProducts, setIsLoadingProducts] = useState(false);

    const {
        isCharging,
        isInternalConfirming,
        isConfirming,
        isSuccess,
        isFail,
        paymentMethod,
        simplePayType,
        agreed,
        successAmount,
        failMessage,
        setPaymentMethod,
        setSimplePayType,
        setAgreed,
        requestPayment,
        resetParams,
        closeResultAndReset
    } = useCreditPayment({ onOpenChange });

    useEffect(() => {
        if (open) {
            const loadProducts = async () => {
                setIsLoadingProducts(true);
                try {
                    const data = await fetchProducts("ONE_TIME");
                    setProducts(data.content);
                    if (data.content.length > 0 && selectedId === null) {
                        // Default select logic (maybe the middle one or first)
                        // Selecting the 3rd one if exists, else first
                        if (data.content.length >= 3) {
                            setSelectedId(data.content[2].productId);
                        } else {
                            setSelectedId(data.content[0].productId);
                        }
                    }
                } catch (error) {
                    console.error("Failed to load products", error);
                } finally {
                    setIsLoadingProducts(false);
                }
            };
            loadProducts();
        }
    }, [open]);

    const selectedProduct = products.find(p => p.productId === selectedId) || products[0];
    const totalCredits = selectedProduct ? selectedProduct.creditAmount : 0;
    const totalPrice = selectedProduct ? selectedProduct.priceAmount : 0;

    // 성공 시 표시할 크레딧 계산 (금액 기반 역추적)
    const getCreditsFromAmount = (amount: number) => {
        const product = products.find(p => p.priceAmount === amount);
        if (product) {
            return product.creditAmount.toLocaleString();
        }
        return "0";
    };

    const handleCharge = () => {
        if (!selectedProduct) return;

        // Convert to PaymentProduct interface expected by hook
        const paymentOption: PaymentProduct = {
            id: selectedProduct.productId,
            credits: selectedProduct.creditAmount,
            price: selectedProduct.priceAmount,
            productCode: selectedProduct.productCode,
            name: selectedProduct.name
        };

        requestPayment(paymentOption);
    };

    const isResultView = isSuccess || isFail || isConfirming;

    return (
        <Dialog open={open} onOpenChange={() => closeResultAndReset()}>
            <DialogContent className={cn(
                "max-w-5xl p-0 overflow-hidden border-none bg-white rounded-2xl shadow-2xl transition-all [&>button]:hidden",
                isResultView ? "max-w-md" : "w-[95vw] max-h-[90vh] h-[750px] flex flex-col"
            )}>
                {isResultView ? (
                    <div className="p-10 text-center space-y-8 bg-white">
                        <div className="flex justify-center">
                            {isConfirming || isInternalConfirming ? (
                                <div className="h-24 w-24 bg-blue-50 rounded-full flex items-center justify-center">
                                    <Loader2 className="h-12 w-12 text-blue-500 animate-spin" />
                                </div>
                            ) : isSuccess ? (
                                <div className="h-24 w-24 bg-emerald-50 rounded-full flex items-center justify-center animate-in zoom-in duration-500">
                                    <CheckCircle2 className="h-12 w-12 text-emerald-500" />
                                </div>
                            ) : isFail ? (
                                <div className="h-24 w-24 bg-red-50 rounded-full flex items-center justify-center animate-in zoom-in duration-500">
                                    <X className="h-12 w-12 text-red-500" />
                                </div>
                            ) : (
                                <div className="h-24 w-24 bg-blue-50 rounded-full flex items-center justify-center">
                                    <Loader2 className="h-12 w-12 text-blue-500 animate-spin" />
                                </div>
                            )}
                        </div>
                        <div className="space-y-2">
                            {isConfirming || isInternalConfirming ? (
                                <>
                                    <h2 className="text-2xl font-bold text-slate-900">결제 정보 확인 중</h2>
                                    <p className="text-slate-500 font-medium">결제 정보를 안전하게 확인하고 있습니다.<br />잠시만 기다려 주세요.</p>
                                </>
                            ) : isSuccess ? (
                                <>
                                    <h2 className="text-2xl font-bold text-slate-900">충전이 완료되었습니다!</h2>
                                    <p className="text-slate-500">
                                        충전된 크레딧: <span className="font-bold text-slate-900">{getCreditsFromAmount(Number(successAmount || 0))} 크레딧</span><br />
                                        결제금액: {Number(successAmount || 0).toLocaleString()}원
                                    </p>
                                    <p className="text-sm font-bold text-blue-600 mt-2">
                                        크레딧이 성공적으로 충전되었습니다.
                                    </p>
                                </>
                            ) : isFail ? (
                                <>
                                    <h2 className="text-2xl font-bold text-slate-900">충전에 실패했습니다</h2>
                                    <p className="text-slate-500 break-keep">{failMessage || "잠시 후 다시 시도해주세요."}</p>
                                </>
                            ) : (
                                <>
                                    <h2 className="text-2xl font-bold text-slate-900">결제 정보 확인 중</h2>
                                    <p className="text-slate-500 font-medium">잠시만 기다려 주세요...</p>
                                </>
                            )}
                        </div>
                        <div className="pt-4 flex gap-2">
                            {!isConfirming && !isInternalConfirming && isFail && (
                                <Button variant="outline" onClick={resetParams} className="flex-1 text-lg py-6 rounded-xl">
                                    다시 시도
                                </Button>
                            )}
                            {!isInternalConfirming && !isConfirming && (
                                <Button onClick={closeResultAndReset} className={cn("flex-1 text-lg py-6 rounded-xl text-white", isSuccess ? "bg-blue-600 hover:bg-blue-700" : "bg-slate-900 hover:bg-slate-800")}>
                                    확인
                                </Button>
                            )}
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
                            </div>

                            <div className="mt-auto">
                                <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
                                    <div className="flex items-center gap-2 text-[10px] font-bold text-slate-400 mb-2">
                                        <ShieldCheck className="h-3 w-3" />
                                        보안 결제 시스템 작동 중
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="font-bold text-slate-500 text-sm">결제 금액</span>
                                        <span className="text-lg font-black text-slate-900">{totalPrice.toLocaleString()}원</span>
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
                                {isLoadingProducts ? (
                                    <div className="flex justify-center items-center h-40">
                                        <Loader2 className="h-8 w-8 text-blue-500 animate-spin" />
                                    </div>
                                ) : (
                                    <div className="grid grid-cols-2 lg:grid-cols-3 gap-3 mb-8">
                                        {products.map((opt) => (
                                            <div
                                                key={opt.productId}
                                                onClick={() => setSelectedId(opt.productId)}
                                                className={cn(
                                                    "relative cursor-pointer rounded-xl border-2 p-4 transition-all duration-200 hover:shadow-md",
                                                    selectedId === opt.productId
                                                        ? "border-blue-500 bg-white ring-2 ring-blue-50/50"
                                                        : "border-slate-100 bg-white hover:border-slate-200"
                                                )}
                                            >
                                                {/* Popular badge logic could go here if we had data, currently omitted */}

                                                {selectedId === opt.productId && (
                                                    <div className="absolute top-2 right-2 text-blue-500">
                                                        <CheckCircle2 className="h-4 w-4 fill-blue-50" />
                                                    </div>
                                                )}
                                                <div className="space-y-0.5">
                                                    <p className="text-[9px] font-bold text-slate-400 uppercase tracking-wide">CREDITS</p>
                                                    <p className="text-xl font-black text-slate-800">{opt.creditAmount.toLocaleString()}</p>
                                                    <p className="text-sm font-medium text-slate-500">{opt.priceAmount.toLocaleString()}원</p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}

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
                                        disabled={isCharging || !selectedProduct}
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
