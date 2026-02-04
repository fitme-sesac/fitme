import { useState, useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { createPayment } from "@/api/payment";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Coins, ArrowLeft, ShieldCheck, Loader2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";

// 결제위젯 SDK 타입 선언
declare global {
    interface Window {
        PaymentWidget: (clientKey: string, customerKey: string) => any;
    }
}

interface CreditOption {
    id: string;
    credits: number;
    price: number;
    bonus?: number;
}

// TODO: 상품 정보는 백엔드 API(/api/v1/products)에서 조회하도록 개선 필요
// 현재는 백엔드의 Product 테이블과 동기화되어야 함
const CREDIT_OPTIONS: CreditOption[] = [
    { id: "1", credits: 1000, price: 11000 },
    { id: "2", credits: 5000, price: 55000, bonus: 250 },
    { id: "3", credits: 10000, price: 110000, bonus: 1000 },
    { id: "4", credits: 30000, price: 330000, bonus: 4500 },
    { id: "5", credits: 50000, price: 550000, bonus: 10000 },
    { id: "6", credits: 100000, price: 990000, bonus: 25000 },
];

// 프로덕션에서는 반드시 VITE_TOSS_CLIENT_KEY 환경변수가 설정되어야 함
const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;

// 결제위젯 SDK 스크립트 로드
const loadPaymentWidgetScript = (): Promise<void> => {
    return new Promise((resolve, reject) => {
        if (window.PaymentWidget) {
            resolve();
            return;
        }

        const script = document.createElement("script");
        script.src = "https://js.tosspayments.com/v1/payment-widget";
        script.async = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error("Failed to load payment widget script"));
        document.head.appendChild(script);
    });
};

export default function PaymentCheckoutPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth() as any;

    // URL에서 선택된 상품 정보 가져오기
    const selectedOptionId = searchParams.get("option") || "1";
    const selectedOption = CREDIT_OPTIONS.find(o => o.id === selectedOptionId) || CREDIT_OPTIONS[0];

    const [isLoading, setIsLoading] = useState(true);
    const [isWidgetReady, setIsWidgetReady] = useState(false);
    const [isCharging, setIsCharging] = useState(false);
    const [orderId, setOrderId] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    const paymentWidgetRef = useRef<any>(null);
    const paymentMethodsWidgetRef = useRef<any>(null);
    const customerKey = user?.id ? `customer_${user.id}` : `guest_${Date.now()}`;

    // 페이지 로드 시 결제 준비
    useEffect(() => {
        const preparePayment = async () => {
            try {
                setIsLoading(true);
                setError(null);

                // 결제 설정 확인
                if (!clientKey) {
                    throw new Error("결제 설정이 올바르지 않습니다. 관리자에게 문의하세요.");
                }

                // 1. 백엔드에 주문 생성
                const backendOrder = await createPayment({
                    amount: selectedOption.price,
                    orderName: `${selectedOption.credits.toLocaleString()} 크레딧 충전`,
                    buyerType: "MEMBER",
                    productCode: `CREDIT_${selectedOption.credits}`,
                    idempotencyKey: `ORDER-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
                });

                if (!backendOrder?.orderId) {
                    throw new Error("주문 생성에 실패했습니다.");
                }

                setOrderId(backendOrder.orderId);

                // 2. 결제위젯 SDK 로드
                await loadPaymentWidgetScript();

                // 3. 위젯 초기화
                setTimeout(() => {
                    initializePaymentWidget(backendOrder.orderId);
                    setIsLoading(false);
                }, 100);

            } catch (err: any) {
                console.error("Payment prepare error:", err);
                setError(err?.message || "결제 준비 중 오류가 발생했습니다.");
                setIsLoading(false);
            }
        };

        preparePayment();
    }, [selectedOption.price]);

    // 결제위젯 초기화
    const initializePaymentWidget = (currentOrderId: string) => {
        try {
            const paymentWidget = window.PaymentWidget(clientKey, customerKey);
            paymentWidgetRef.current = paymentWidget;

            // 결제수단 위젯 렌더링
            const paymentMethodsWidget = paymentWidget.renderPaymentMethods(
                '#payment-methods-widget',
                { value: selectedOption.price },
                { variantKey: 'DEFAULT' }
            );
            paymentMethodsWidgetRef.current = paymentMethodsWidget;

            // 약관 동의 위젯 렌더링
            paymentWidget.renderAgreement('#agreement-widget', {
                variantKey: 'AGREEMENT'
            });

            setIsWidgetReady(true);
            console.log("Payment Widget Initialized");
        } catch (err: any) {
            console.error("Widget initialization failed:", err);
            setError(`위젯 초기화 실패: ${err.message}`);
        }
    };

    // 결제 요청
    const handleRequestPayment = async () => {
        if (!paymentWidgetRef.current || !orderId) {
            alert("결제 위젯이 준비되지 않았습니다.");
            return;
        }

        setIsCharging(true);

        try {
            await paymentWidgetRef.current.requestPayment({
                orderId: orderId,
                orderName: `${selectedOption.credits.toLocaleString()} 크레딧 충전`,
                customerName: user?.user_metadata?.display_name || user?.name || "사용자",
                customerEmail: user?.email,
                successUrl: `${window.location.origin}/payment/success`,
                failUrl: `${window.location.origin}/payment/fail`
            });
        } catch (error: any) {
            console.error("Payment Error:", error);
            setIsCharging(false);

            const cancelCodes = ['USER_CANCEL', 'PAY_PROCESS_CANCELED', 'PAY_PROCESS_ABORTED', 'INVALID_PROCESS'];
            if (cancelCodes.includes(error?.code) || error?.message?.includes('취소')) {
                return;
            }

            alert(`결제 오류: ${error?.message || "결제 진행 중 오류가 발생했습니다."}`);
        }
    };

    // 뒤로가기
    const handleBack = () => {
        navigate(-1);
    };

    // 로딩 중
    if (isLoading) {
        return (
            <div className="min-h-screen bg-slate-50 flex items-center justify-center">
                <Card className="p-8 text-center">
                    <Loader2 className="h-12 w-12 animate-spin text-blue-600 mx-auto mb-4" />
                    <p className="text-slate-500 font-medium">결제 정보를 불러오는 중...</p>
                </Card>
            </div>
        );
    }

    // 에러 발생
    if (error) {
        return (
            <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
                <Card className="p-8 text-center max-w-md w-full space-y-4">
                    <div className="h-16 w-16 bg-red-100 rounded-full flex items-center justify-center mx-auto">
                        <span className="text-2xl text-red-600 font-bold">!</span>
                    </div>
                    <h2 className="text-xl font-bold text-slate-900">결제 준비 실패</h2>
                    <p className="text-slate-500">{error}</p>
                    <Button onClick={handleBack} variant="outline" className="w-full">
                        돌아가기
                    </Button>
                </Card>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50">
            {/* 헤더 */}
            <div className="bg-white border-b sticky top-0 z-10">
                <div className="max-w-4xl mx-auto px-4 py-4 flex items-center gap-4">
                    <Button variant="ghost" size="icon" onClick={handleBack}>
                        <ArrowLeft className="h-5 w-5" />
                    </Button>
                    <h1 className="text-xl font-bold text-slate-900">결제하기</h1>
                </div>
            </div>

            <div className="max-w-4xl mx-auto p-4 grid md:grid-cols-3 gap-6">
                {/* 좌측: 결제 위젯 */}
                <div className="md:col-span-2 space-y-4">
                    <Card className="p-6">
                        <h2 className="text-lg font-bold text-slate-900 mb-4">결제 수단 선택</h2>
                        <div id="payment-methods-widget" className="min-h-[300px]" />
                    </Card>

                    <Card className="p-6">
                        <div id="agreement-widget" />
                    </Card>
                </div>

                {/* 우측: 주문 요약 */}
                <div className="space-y-4">
                    <Card className="p-6 sticky top-20">
                        <h3 className="text-lg font-bold text-slate-900 mb-4">주문 요약</h3>

                        <div className="space-y-4">
                            <div className="flex items-center gap-3 p-4 bg-slate-50 rounded-lg">
                                <div className="h-12 w-12 bg-blue-100 rounded-full flex items-center justify-center">
                                    <Coins className="h-6 w-6 text-blue-600" />
                                </div>
                                <div>
                                    <p className="font-bold text-slate-900">
                                        {selectedOption.credits.toLocaleString()} 크레딧
                                    </p>
                                    {selectedOption.bonus && (
                                        <Badge variant="secondary" className="text-xs">
                                            +{selectedOption.bonus.toLocaleString()} 보너스
                                        </Badge>
                                    )}
                                </div>
                            </div>

                            <div className="border-t pt-4">
                                <div className="flex justify-between items-center">
                                    <span className="text-slate-500">상품 금액</span>
                                    <span className="font-medium">{selectedOption.price.toLocaleString()}원</span>
                                </div>
                            </div>

                            <div className="border-t pt-4">
                                <div className="flex justify-between items-center">
                                    <span className="font-bold text-slate-900">총 결제 금액</span>
                                    <span className="text-2xl font-black text-blue-600">
                                        {selectedOption.price.toLocaleString()}원
                                    </span>
                                </div>
                            </div>

                            <Button
                                onClick={handleRequestPayment}
                                disabled={!isWidgetReady || isCharging}
                                className="w-full h-12 text-lg font-bold bg-blue-600 hover:bg-blue-700"
                            >
                                {isCharging ? (
                                    <>
                                        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                                        결제 진행 중...
                                    </>
                                ) : (
                                    `${selectedOption.price.toLocaleString()}원 결제하기`
                                )}
                            </Button>

                            <div className="flex items-center justify-center gap-2 text-xs text-slate-400">
                                <ShieldCheck className="h-3 w-3" />
                                <span>토스페이먼츠 안전결제</span>
                            </div>
                        </div>
                    </Card>
                </div>
            </div>
        </div>
    );
}
