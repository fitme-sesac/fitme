// @ts-nocheck
import { Button } from "@/components/ui/button";
import { ChevronRight } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyPayments } from "@/api/payment";
import { getScrapCount, getRecentViewedJobs } from "@/features/job/api/jobApi";

export function JobSeekerWidgets() {
    const [scrapCount, setScrapCount] = useState<number>(0);
    const [recentViewCount, setRecentViewCount] = useState<number>(0);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchCounts = async () => {
            try {
                // 스크랩 수 조회
                const scrapResponse = await getScrapCount();
                setScrapCount(scrapResponse?.count ?? scrapResponse ?? 0);
            } catch (error) {
                console.debug("스크랩 수 조회 실패:", error);
            }

            try {
                // 최근 본 공고 수 조회
                const recentResponse = await getRecentViewedJobs(100);
                const recentList = Array.isArray(recentResponse?.content) 
                    ? recentResponse.content 
                    : (Array.isArray(recentResponse) ? recentResponse : []);
                setRecentViewCount(recentList.length);
            } catch (error) {
                console.debug("최근 본 공고 조회 실패:", error);
            }

            setLoading(false);
        };

        fetchCounts();
    }, []);

    return (
        <div className="space-y-6">
            {/* Counts */}
            <div className="bg-white rounded-2xl border shadow-sm overflow-hidden">
                <div
                    className="flex justify-between items-center p-5 border-b hover:bg-muted/30 cursor-pointer transition-colors group"
                    role="button"
                    tabIndex={0}
                    onClick={() => navigate("/mypage?tab=saved")}
                    onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            navigate("/mypage?tab=saved");
                        }
                    }}
                >
                    <span className="text-sm font-medium text-muted-foreground group-hover:text-foreground transition-colors">스크랩 공고</span>
                    <div className="flex items-center gap-2">
                        <span className="font-bold text-lg text-[#5A639C]">{loading ? '-' : scrapCount}</span>
                        <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:translate-x-1 transition-transform" />
                    </div>
                </div>
                <div
                    className="flex justify-between items-center p-5 hover:bg-muted/30 cursor-pointer transition-colors group"
                    role="button"
                    tabIndex={0}
                    onClick={() => navigate("/jobs")}
                    onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            navigate("/jobs");
                        }
                    }}
                >
                    <span className="text-sm font-medium text-muted-foreground group-hover:text-foreground transition-colors">최근 본 공고</span>
                    <div className="flex items-center gap-2">
                        <span className="font-bold text-lg text-[#5A639C]">{loading ? '-' : recentViewCount}</span>
                        <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:translate-x-1 transition-transform" />
                    </div>
                </div>
            </div>

            {/* Payment History Widget */}
            <PaymentHistoryWidget />
        </div>
    );
}

function PaymentHistoryWidget() {
    const [payments, setPayments] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchPayments = async () => {
            try {
                const response = await getMyPayments();
                // Check if response has content (Page<PaymentResponse>)
                const content = response?.content || (Array.isArray(response) ? response : []);
                setPayments(content.slice(0, 3));
            } catch (error) {
                console.error("Failed to fetch payments:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchPayments();
    }, []);

    return (
        <div className="bg-white rounded-2xl border shadow-sm overflow-hidden">
            <div className="p-5 border-b flex justify-between items-center">
                <h3 className="font-bold text-foreground">최근 결제 내역</h3>
                <ChevronRight className="h-4 w-4 text-muted-foreground cursor-pointer hover:text-[#5A639C]" />
            </div>

            <div className="divide-y">
                {loading ? (
                    <div className="p-6 text-center text-sm text-muted-foreground">
                        로딩 중...
                    </div>
                ) : payments.length > 0 ? (
                    payments.map((payment) => (
                        <div key={payment.paymentId} className="p-4 hover:bg-gray-50 transition-colors flex justify-between items-center group cursor-pointer">
                            <div className="space-y-1">
                                <div className="text-sm font-medium text-foreground group-hover:text-[#5A639C] transition-colors">
                                    {payment.orderName}
                                </div>
                                <div className="text-xs text-muted-foreground">
                                    {new Date(payment.approvedAt).toLocaleDateString()}
                                </div>
                            </div>
                            <div className="text-right">
                                <div className="font-bold text-sm">
                                    {payment.totalAmount.toLocaleString()}원
                                </div>
                                <span className={`text-[10px] px-2 py-0.5 rounded-full ${payment.status === 'DONE' ? 'bg-green-100 text-green-700' :
                                    payment.status === 'CANCELED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-700'
                                    }`}>
                                    {payment.status === 'DONE' ? '결제완료' : payment.status === 'CANCELED' ? '취소됨' : payment.status}
                                </span>
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="p-8 text-center text-sm text-muted-foreground">
                        결제 내역이 없습니다.
                    </div>
                )}
            </div>

            {/* Top button */}
            <Button variant="outline" className="w-full text-xs font-bold rounded-xl h-10 hover:bg-[#5A639C] hover:text-white transition-colors border-dashed" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
                <span className="mr-1">^</span> TOP
            </Button>
        </div>
    );
}
