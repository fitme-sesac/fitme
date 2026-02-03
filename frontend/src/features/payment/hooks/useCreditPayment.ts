import { useState, useEffect, useRef, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { createPayment, confirmPayment } from "@/api/payment";
import { useAuth } from "@/contexts/AuthContext";

// Define a local interface for AuthContext to improved type safety
// Ideally this should be exported from AuthContext.tsx
interface AuthContextType {
    user: any;
    isCompany: boolean;
    userRole: string;
    refreshCredits: (role?: string) => Promise<void>;
    checkSession: () => Promise<any>;
}

interface UseCreditPaymentProps {
    onOpenChange?: (open: boolean) => void;
}

export interface PaymentProduct {
    id: string | number;
    credits: number;
    price: number;
    productCode: string;
    name?: string;
}

export function useCreditPayment({ onOpenChange }: UseCreditPaymentProps = {}) {
    const auth = useAuth() as AuthContextType; // Type assertion for better safety within this file
    const { user, isCompany, checkSession } = auth;

    const [searchParams, setSearchParams] = useSearchParams();

    // Status states
    const [isCharging, setIsCharging] = useState(false);
    const [isInternalConfirming, setIsInternalConfirming] = useState(false);

    // Payment Method states
    const [paymentMethod, setPaymentMethod] = useState("SIMPLE");
    const [simplePayType, setSimplePayType] = useState<string | null>(null);
    const [agreed, setAgreed] = useState(false);

    // URL Param states
    const isSuccess = searchParams.get("payment_success") === "true";
    const isFail = searchParams.get("payment_fail") === "true";
    const isConfirming = searchParams.get("payment_confirm") === "true";

    const successOrderId = searchParams.get("orderId");
    const successAmount = searchParams.get("amount");
    const failMessage = searchParams.get("message");

    const confirmRef = useRef(false);

    // Payment Confirmation Logic
    useEffect(() => {
        if (!isConfirming) {
            confirmRef.current = false;
            return;
        }

        if (confirmRef.current || isInternalConfirming) return;
        confirmRef.current = true;

        const confirm = async () => {
            const paymentKey = searchParams.get("paymentKey");
            const orderId = searchParams.get("orderId");
            const amount = Number(searchParams.get("amount"));

            if (!paymentKey || !orderId || !amount) return;

            setIsInternalConfirming(true);
            try {
                await confirmPayment({
                    paymentKey,
                    orderId,
                    amount
                });

                // Retry logic for credit refresh
                let retryCount = 0;
                const maxRetries = 3;
                let lastError = null;

                while (retryCount < maxRetries) {
                    await new Promise(resolve => setTimeout(resolve, retryCount === 0 ? 300 : 500));

                    try {
                        // Use checkSession to fetch latest role and credits
                        if (checkSession) {
                            await checkSession();
                        }
                        lastError = null;
                        break;
                    } catch (error) {
                        lastError = error;
                        retryCount++;
                        console.warn(`크레딧 갱신 재시도 ${retryCount}/${maxRetries}:`, error);
                    }
                }

                if (lastError && retryCount >= maxRetries) {
                    console.error("크레딧 갱신 실패, 최대 재시도 횟수 초과:", lastError);
                }

                // Success: Switch URL params
                const newParams = new URLSearchParams();
                newParams.set("payment_success", "true");
                newParams.set("orderId", orderId);
                newParams.set("amount", String(amount));
                setSearchParams(newParams, { replace: true });

            } catch (err: any) {
                console.error("Payment confirmation failed:", err);
                const errorMsg = err.response?.data?.message || "결제 승인 중 오류가 발생했습니다.";

                // Fail: Switch URL params
                const newParams = new URLSearchParams();
                newParams.set("payment_fail", "true");
                newParams.set("message", errorMsg);
                setSearchParams(newParams, { replace: true });
            } finally {
                setIsInternalConfirming(false);
            }
        };

        confirm();
    }, [isConfirming, searchParams, setSearchParams, checkSession]);

    // Payment Request Logic
    const requestPayment = useCallback(async (option: PaymentProduct) => {
        if (!agreed) {
            alert("구매 조건 및 결제 진행 동의가 필요합니다.");
            return;
        }

        const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
        const customerKey = user?.id || user?.email?.replace(/[^a-zA-Z0-9]/g, "") || "ANONYMOUS";

        setIsCharging(true);

        try {
            const productCode = option.productCode;

            // 1. Create Order on Backend
            let backendOrder;
            try {
                backendOrder = await createPayment({
                    amount: option.price,
                    orderName: option.name || `${option.credits.toLocaleString()} 크레딧 충전`,
                    buyerType: isCompany ? "EMPLOYER" : "MEMBER",
                    productCode: productCode,
                    idempotencyKey: `ORDER-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
                });
            } catch (err: any) {
                console.error("Backend payment creation failed:", err);
                throw new Error(err.response?.data?.message || "서버 결제 요청 생성에 실패했습니다.");
            }

            const orderId = backendOrder.orderId;

            // 2. Mock Payment for Test/Dev
            if (!clientKey || clientKey === "test") {
                const currentPath = encodeURIComponent(window.location.pathname + window.location.search);
                window.location.href = `${window.location.origin}/payment/success?paymentKey=mock_${Date.now()}&orderId=${orderId}&amount=${option.price}&next=${currentPath}`;
                return;
            }

            // 3. Request Toss Payment
            const tossPayments = await loadTossPayments(clientKey);
            const payment = tossPayments.payment({ customerKey });

            let method = "CARD";
            if (paymentMethod === "SIMPLE") {
                if (simplePayType === "kakao") method = "CARD"; // Toss treats kakao as card flow often, or needs specific method if simple pay
            } else if (paymentMethod === "PHONE") {
                method = "MOBILE_PHONE";
            } else if (paymentMethod === "TRANSFER") {
                method = "TRANSFER";
            } else if (paymentMethod === "GIFT") {
                method = "GIFT_CERTIFICATE";
            }

            // Adjust method for specific simple payments if needed by Toss SDK.
            // For now keeping logic same as original file.

            await payment.requestPayment({
                method: method as any,
                amount: {
                    value: option.price,
                    currency: "KRW",
                },
                orderId: orderId,
                orderName: `${option.credits.toLocaleString()} 크레딧 충전`,
                customerEmail: user?.email,
                customerName: user?.user_metadata?.display_name || "사용자",
                successUrl: `${window.location.origin}/payment/success?next=${encodeURIComponent(window.location.pathname + window.location.search)}`,
                failUrl: `${window.location.origin}/payment/fail?next=${encodeURIComponent(window.location.pathname + window.location.search)}`,
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
    }, [agreed, user, isCompany, paymentMethod, simplePayType]);

    // Reset URL Params
    const resetParams = useCallback(() => {
        const newParams = new URLSearchParams(searchParams);
        newParams.delete("payment_fail");
        newParams.delete("message");
        newParams.delete("code");
        newParams.delete("payment_success");
        newParams.delete("payment_confirm");
        newParams.delete("orderId");
        newParams.delete("amount");
        newParams.delete("paymentKey");
        setSearchParams(newParams, { replace: true });
    }, [searchParams, setSearchParams]);

    const closeResultAndReset = useCallback(() => {
        if (isSuccess || isFail || isConfirming) {
            resetParams();
        }
        onOpenChange?.(false);
    }, [isSuccess, isFail, isConfirming, resetParams, onOpenChange]);


    return {
        // States
        isCharging,
        isInternalConfirming,
        isConfirming,
        isSuccess,
        isFail,
        paymentMethod,
        simplePayType,
        agreed,
        successOrderId,
        successAmount,
        failMessage,

        // Setters
        setPaymentMethod,
        setSimplePayType,
        setAgreed,

        // Actions
        requestPayment,
        resetParams,
        closeResultAndReset
    };
}
