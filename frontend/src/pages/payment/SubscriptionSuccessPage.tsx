import { useEffect, useState, useRef } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { useCreateSubscription } from "@/features/payment/hooks/useSubscription";
import { useAuth } from "@/contexts/AuthContext";
import { http } from "@/api/http";

import { Loader2, CheckCircle2, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function SubscriptionSuccessPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { user } = useAuth();

    const [status, setStatus] = useState<"PROCESSING" | "SUCCESS" | "ERROR">("PROCESSING");
    const [errorMessage, setErrorMessage] = useState("");

    const { mutate: createSubscription } = useCreateSubscription();

    const authKey = searchParams.get("authKey");
    const customerKey = searchParams.get("customerKey");
    const productIdParam = searchParams.get("productId");
    const productId = productIdParam ? parseInt(productIdParam) : 1;

    // Use a ref to prevent double execution in StrictMode
    const processingRef = useRef(false);

    useEffect(() => {
        if (!authKey || !customerKey || processingRef.current) {
            return;
        }

        const processSubscription = async () => {
            try {
                // 1. Fetch Employer ID
                const dashRes = await http.get("/api/employer/dashboard");
                const employerId = dashRes.data?.profile?.employerId;

                if (!employerId) {
                    throw new Error("기업 정보를 찾을 수 없습니다. 기업 회원으로 로그인해주세요.");
                }

                // 2. Create Subscription
                const payload = {
                    employerId,
                    productId,
                    authKey,
                    customerKey
                };
                console.log("Creating subscription with payload:", payload);

                createSubscription(
                    payload,
                    {
                        onSuccess: () => {
                            setStatus("SUCCESS");
                        },
                        onError: (err: any) => {
                            console.error("Subscription creation failed:", err);
                            console.error("Error response data:", err.response?.data);
                            setStatus("ERROR");
                            const detail = err.response?.data?.message || err.message;
                            setErrorMessage(`구독 생성 실패: ${detail} (PID: ${productId})`);
                        }
                    }
                );

            } catch (err: any) {
                console.error("Process error:", err);
                setStatus("ERROR");
                setErrorMessage(err.message || "처리 중 오류가 발생했습니다.");
            }
        };

        processingRef.current = true;
        processSubscription();
    }, [authKey, customerKey, productId, createSubscription]);

    if (status === "PROCESSING") {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center p-4 bg-slate-50">
                <Loader2 className="h-12 w-12 text-blue-600 animate-spin mb-4" />
                <h2 className="text-xl font-bold text-slate-800">구독 처리 중...</h2>
                <p className="text-slate-500">잠시만 기다려주세요.</p>
            </div>
        );
    }

    if (status === "ERROR") {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center p-4 bg-slate-50">
                <div className="bg-white p-8 rounded-2xl shadow-xl max-w-md w-full text-center">
                    <div className="mx-auto h-20 w-20 bg-red-50 rounded-full flex items-center justify-center mb-6">
                        <AlertCircle className="h-10 w-10 text-red-500" />
                    </div>
                    <h2 className="text-2xl font-bold text-slate-900 mb-2">오류가 발생했습니다</h2>
                    <p className="text-slate-500 mb-8 break-keep">{errorMessage}</p>
                    <Button onClick={() => navigate("/subscription")} className="w-full h-12 text-lg">
                        다시 시도하기
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col items-center justify-center p-4 bg-slate-50">
            <div className="bg-white p-10 rounded-2xl shadow-xl max-w-md w-full text-center">
                <div className="mx-auto h-24 w-24 bg-emerald-50 rounded-full flex items-center justify-center mb-6 animate-in zoom-in duration-300">
                    <CheckCircle2 className="h-12 w-12 text-emerald-500" />
                </div>
                <h2 className="text-2xl font-black text-slate-900 mb-2">구독이 시작되었습니다!</h2>
                <p className="text-slate-500 mb-8">
                    카드가 성공적으로 등록되었으며, <br />
                    첫 달 멤버십 비용 결제가 완료되었습니다.
                </p>

                <div className="space-y-3">
                    <Button
                        onClick={() => navigate("/employer/dashboard")}
                        className="w-full h-12 text-lg font-bold bg-slate-900 hover:bg-slate-800"
                    >
                        대시보드로 이동
                    </Button>
                    <Button
                        variant="ghost"
                        onClick={() => navigate("/")}
                        className="w-full text-slate-400"
                    >
                        메인으로 돌아가기
                    </Button>
                </div>
            </div>
        </div>
    );
}
