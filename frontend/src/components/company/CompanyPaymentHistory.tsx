import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";
import { getMyPayments, cancelPayment } from "@/api/payment";
import { getMyLedgers } from "@/api/wallet";
import { Loader2, RefreshCw, Coins, Receipt, Crown } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export function CompanyPaymentHistory() {
    const { userRole } = useAuth();
    const [payments, setPayments] = useState<any[]>([]);
    const [ledgers, setLedgers] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isCancelling, setIsCancelling] = useState<string | null>(null);

    const [errorMsg, setErrorMsg] = useState<string | null>(null);
    const [debugInfo, setDebugInfo] = useState<string>("");

    const fetchHistory = async () => {
        setIsLoading(true);
        setErrorMsg(null);
        try {
            console.log("Fetching history...");
            // Role Normalization for Frontend consistency
            const effectiveRole = (userRole === "ROLE_COMPANY" || userRole === "COMPANY") ? "EMPLOYER" : userRole;

            const results = await Promise.allSettled([
                getMyPayments(effectiveRole),
                getMyLedgers(effectiveRole)
            ]);

            const paymentsResult = results[0];
            const ledgersResult = results[1];

            if (paymentsResult.status === 'fulfilled') {
                console.log("Payments loaded:", paymentsResult.value);
                setPayments(paymentsResult.value.content || []);
            } else {
                console.error("Payments failed:", paymentsResult.reason);
                setDebugInfo(prev => prev + "Payments API Failed: " + (paymentsResult.reason?.message || "Unknown error") + "\n");
            }

            if (ledgersResult.status === 'fulfilled') {
                console.log("Ledgers loaded:", ledgersResult.value);
                setLedgers(ledgersResult.value.content || []);
            } else {
                console.error("Ledgers failed:", ledgersResult.reason);
                setDebugInfo(prev => prev + "Ledgers API Failed: " + (ledgersResult.reason?.message || "Unknown error") + "\n");
            }

            if (paymentsResult.status === 'rejected' || ledgersResult.status === 'rejected') {
                setErrorMsg("일부 내역을 불러오지 못했습니다.");
            }

        } catch (error: any) {
            console.error("Failed to fetch history (Fatal):", error);
            setErrorMsg("내역 조회 중 오류가 발생했습니다.");
            setDebugInfo(error?.message || "Critical Error");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchHistory();
    }, [userRole]);

    const handleRefund = async (paymentKey: string) => {
        if (!window.confirm("정말로 환불하시겠습니까? 환불 시 크레딧이 회수됩니다.")) return;

        setIsCancelling(paymentKey);
        try {
            await cancelPayment(paymentKey, "사용자 요청 환불");
            alert("환불 요청이 완료되었습니다.");
            fetchHistory(); // Refresh
        } catch (error: any) {
            console.error("Refund failed:", error);
            alert(`환불 실패: ${error.message || "알 수 없는 오류가 발생했습니다."}`);
        } finally {
            setIsCancelling(null);
        }
    };

    // Merge payments and ledgers for a unified chronological view
    const unifiedHistory = [
        ...payments.map(p => ({
            id: `pay-${p.paymentId}`,
            type: 'charge',
            amount: p.creditAmount || 0,  // 크레딧 수량을 메인 금액으로
            paidAmount: p.totalAmount,     // 결제금액을 보조로
            title: '크레딧 충전',  // 단건결제는 '크레딧 충전'으로 통일
            date: p.approvedAt ? new Date(p.approvedAt).toLocaleString() : '진행 중',
            isPlus: true,
            payMethod: p.method,
            status: p.status === '승인 완료' ? '승인 완료' : (p.status === '전체 취소' || p.status === '부분 취소') ? '환불 완료' : '진행 중',
            paymentKey: p.orderId,
            canRefund: p.status === '승인 완료',
            sourceType: 'PAYMENT' as const // sourceType 추가
        })),
        ...ledgers
            .filter((l: any) => l.sourceType !== 'PAYMENT') // 결제(PAYMENT)로 인한 충전 중복 제거
            .map(l => ({
                id: `ledger-${l.ledgerId}`,
                type: l.type === 'CREDIT' ? 'charge' : 'use',
                amount: l.amount,
                paidAmount: null,
                title: l.sourceType === 'SUBSCRIPTION' ? '구독 크레딧 지급' : l.memo,
                date: new Date(l.occurredAt).toLocaleString(),
                isPlus: l.type === 'CREDIT',
                payMethod: '크레딧',
                status: l.type === 'CREDIT' ? '충전 완료' : '사용 완료',
                canRefund: false,
                paymentKey: undefined,
                sourceType: l.sourceType // sourceType 추가
            }))
    ].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between">
                <div>
                    <CardTitle>결제 및 크레딧 내역</CardTitle>
                    <CardDescription>충전 및 사용 내역을 확인하세요</CardDescription>
                </div>
                <Button variant="ghost" size="sm" onClick={fetchHistory} disabled={isLoading}>
                    <RefreshCw className={cn("h-4 w-4 mr-2", isLoading && "animate-spin")} />
                    새로고침
                </Button>
            </CardHeader>
            <CardContent>
                <div className="space-y-2">
                    {errorMsg && (
                        <div className="p-4 rounded-lg bg-red-50 text-red-600 text-sm mb-4">
                            <p className="font-bold mb-1">{errorMsg}</p>
                            <pre className="text-xs opacity-75">{debugInfo}</pre>
                        </div>
                    )}
                    {payments.length === 0 && ledgers.length === 0 && !isLoading ? (
                        <div className="text-center py-8 text-muted-foreground">
                            내역이 없습니다. (No Data)
                        </div>
                    ) : (
                        unifiedHistory.map((item) => {
                            // 구독 관련 내역 여부 확인 (sourceType으로 정확하게 판단)
                            const isSubscription = item.sourceType === 'SUBSCRIPTION';

                            return (
                                <div key={item.id} className="p-4 rounded-lg border hover:bg-muted/50 transition-colors">
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-start gap-4">
                                            <div className={cn(
                                                "mt-1 w-10 h-10 rounded-full flex items-center justify-center shrink-0",
                                                isSubscription
                                                    ? "bg-purple-100 text-purple-600"
                                                    : item.isPlus ? "bg-emerald-100 text-emerald-600" : "bg-slate-100 text-slate-600"
                                            )}>
                                                {isSubscription ? <Crown className="h-5 w-5" /> : (item.isPlus ? <Coins className="h-5 w-5" /> : <Receipt className="h-5 w-5" />)}
                                            </div>
                                            <div>
                                                <div className="flex items-center gap-2 mb-1">
                                                    <p className="font-bold text-base">{item.title}</p>
                                                    {isSubscription && <Badge variant="secondary" className="text-[10px] px-1.5 h-5 bg-purple-100 text-purple-700 hover:bg-purple-100">구독혜택</Badge>}
                                                </div>
                                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                                    <span>{item.date}</span>
                                                    <span className="w-0.5 h-3 bg-slate-200"></span>
                                                    <span>{item.payMethod}</span>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="text-right">
                                            <p className={cn(
                                                "text-lg font-bold",
                                                isSubscription ? "text-purple-600" : (item.isPlus ? "text-emerald-600" : "text-slate-900")
                                            )}>
                                                {item.isPlus ? '+' : '-'}{Number(item.amount).toLocaleString()} 크레딧
                                            </p>
                                            {item.paidAmount && (
                                                <p className="text-xs text-muted-foreground">
                                                    결제 {Number(item.paidAmount).toLocaleString()}원
                                                </p>
                                            )}
                                            <div className="flex items-center justify-end gap-2 mt-1">
                                                <p className="text-xs text-muted-foreground font-medium">
                                                    {item.status}
                                                </p>
                                                {item.canRefund && (
                                                    <Button
                                                        variant="outline"
                                                        size="sm"
                                                        className="h-6 text-xs text-red-500 hover:text-red-600 hover:bg-red-50 border-red-200"
                                                        onClick={() => handleRefund(item.paymentKey)}
                                                        disabled={isCancelling === item.paymentKey}
                                                    >
                                                        {isCancelling === item.paymentKey ? "처리 중..." : "환불"}
                                                    </Button>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </CardContent>
        </Card>
    );
}
