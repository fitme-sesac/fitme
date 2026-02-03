import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";
import { getMyPayments, cancelPayment } from "@/api/payment";
import { getMyLedgers } from "@/api/wallet";
import { Loader2, RefreshCw, Coins, Receipt } from "lucide-react";
import { cn } from "@/lib/utils";

export function CompanyPaymentHistory() {
    const { userRole } = useAuth();
    const [payments, setPayments] = useState<any[]>([]);
    const [ledgers, setLedgers] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isCancelling, setIsCancelling] = useState<string | null>(null);

    const fetchHistory = async () => {
        setIsLoading(true);
        try {
            const [paymentsData, ledgersData] = await Promise.all([
                getMyPayments(userRole),
                getMyLedgers(userRole)
            ]);
            setPayments(paymentsData.content || []);
            setLedgers(ledgersData.content || []);
        } catch (error) {
            console.error("Failed to fetch history:", error);
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
            amount: p.totalAmount,
            title: p.orderName,
            date: p.approvedAt ? new Date(p.approvedAt).toLocaleString() : '진행 중',
            isPlus: true,
            payMethod: p.method,
            status: p.status === 'DONE' ? '승인 완료' : p.status === 'CANCELED' ? '환불 완료' : '진행 중',
            paymentKey: p.paymentKey,
            canRefund: p.status === 'DONE'
        })),
        ...ledgers.map(l => ({
            id: `ledger-${l.ledgerId}`,
            type: l.type === 'CREDIT' ? 'charge' : 'use',
            amount: l.amount,
            title: l.memo,
            date: new Date(l.occurredAt).toLocaleString(),
            isPlus: l.type === 'CREDIT',
            payMethod: '크레딧',
            status: l.type === 'CREDIT' ? '충전 완료' : '사용 완료',
            canRefund: false,
            paymentKey: undefined
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
                    {payments.length === 0 && ledgers.length === 0 && !isLoading ? (
                        <div className="text-center py-8 text-muted-foreground">
                            내역이 없습니다.
                        </div>
                    ) : (
                        unifiedHistory.map((item) => (
                            <div key={item.id} className="p-4 rounded-lg border hover:bg-muted/50 transition-colors">
                                <div className="flex items-center justify-between">
                                    <div className="flex items-start gap-4">
                                        <div className={cn(
                                            "mt-1 w-10 h-10 rounded-full flex items-center justify-center shrink-0",
                                            item.isPlus ? "bg-emerald-100 text-emerald-600" : "bg-slate-100 text-slate-600"
                                        )}>
                                            {item.isPlus ? <Coins className="h-5 w-5" /> : <Receipt className="h-5 w-5" />}
                                        </div>
                                        <div>
                                            <p className="font-bold text-base mb-1">{item.title}</p>
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
                                            item.isPlus ? "text-emerald-600" : "text-slate-900"
                                        )}>
                                            {item.isPlus ? '+' : '-'}{Number(item.amount).toLocaleString()}
                                        </p>
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
                        ))
                    )}
                </div>
            </CardContent>
        </Card>
    );
}
