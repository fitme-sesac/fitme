import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Card } from "@/components/ui/card";

export default function PaymentFailPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const code = searchParams.get("code");
    const message = searchParams.get("message");

    useEffect(() => {
        // Redirect to next url or default path with error params
        const nextUrl = searchParams.get("next") || "/";
        const redirectUrl = nextUrl.startsWith("http") ? "/" : nextUrl;

        const newSearchParams = new URLSearchParams();
        newSearchParams.set("payment_fail", "true");
        if (code) newSearchParams.set("code", code);
        if (message) newSearchParams.set("message", message);

        // If redirectUrl already has search params, append them
        const [path, search] = redirectUrl.split("?");
        if (search) {
            const existingParams = new URLSearchParams(search);
            existingParams.forEach((value, key) => newSearchParams.set(key, value));
        }


        navigate(`${path}?${newSearchParams.toString()}`, { replace: true });
    }, [navigate, searchParams, code, message]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
            <Card className="p-8 text-center">
                <p className="text-slate-500">결제 실패 확인 중...</p>
            </Card>
        </div>
    );
}
