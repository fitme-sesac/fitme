import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Loader2 } from "lucide-react";

export default function PaymentSuccessPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    useEffect(() => {
        const paymentKey = searchParams.get("paymentKey");
        const orderId = searchParams.get("orderId");
        const amount = searchParams.get("amount");
        const nextUrl = searchParams.get("next") || "/";

        if (!paymentKey || !orderId || !amount) {
            navigate(nextUrl, { replace: true });
            return;
        }

        // Redirect back to next url with 'payment_confirm=true'
        // CRITICAL: We create a fresh URLSearchParams to avoid carrying over 'payment_fail' or other stale params
        const [path] = nextUrl.split("?");
        const newSearchParams = new URLSearchParams();
        newSearchParams.set("payment_confirm", "true");
        newSearchParams.set("paymentKey", paymentKey);
        newSearchParams.set("orderId", orderId);
        newSearchParams.set("amount", amount);

        navigate(`${path}?${newSearchParams.toString()}`, { replace: true });
    }, [searchParams, navigate]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50">
            <div className="flex flex-col items-center gap-4">
                <Loader2 className="h-10 w-10 animate-spin text-blue-600" />
                <p className="text-slate-500 font-medium">결제 확인 페이지로 이동 중...</p>
            </div>
        </div>
    );
}
