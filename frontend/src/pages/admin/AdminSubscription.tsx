import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { StatCard } from "@/components/admin/StatCard";
import { CreditCard, CheckCircle, XCircle } from "lucide-react";
import { getAdminSubscriptions } from "@/api/admin";
import { toast } from "sonner";

interface Subscription {
    subscriptionId: number;
    employerId: number;
    product: {
        productId: number;
        name: string;
        price: number;
        description?: string;
    } | null;
    nextProduct?: {
        productId: number;
        name: string;
        price: number;
        description?: string;
    } | null;
    status: 'ACTIVE' | 'CANCELED' | 'PAYMENT_FAILED';
    startedAt: string;
    nextBillingAt: string;
    endedAt?: string;
    cardCompany: string;
    cardNumber: string;
}

// DataTable에서 id 속성을 요구하므로, 기존 Subscription 타입에 id를 추가한 새 타입을 정의합니다.
interface SubscriptionForTable extends Subscription {
    id: number;
}

const AdminSubscription = () => {
    const [subscriptions, setSubscriptions] = useState<SubscriptionForTable[]>([]);
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState({ total: 0, active: 0, failed: 0 });

    const fetchSubscriptions = async () => {
        setLoading(true);
        try {
            console.log("API 호출 시작: /admin/api/subscriptions");
            const response = await getAdminSubscriptions();
            console.log("구독 데이터:", response);

            // 인증 실패 시 HTML 응답이 오는 경우를 처리합니다.
            if (typeof response === 'string' && response.includes('<!doctype html>')) {
                console.error('인증 실패: HTML 응답 수신');
                toast.error("관리자 권한이 필요합니다. 다시 로그인해주세요.");
                setSubscriptions([]);
                setStats({ total: 0, active: 0, failed: 0 });
                return;
            }

            const rawData: Subscription[] = Array.isArray(response) ? response : (response?.content || []);
            console.log("원본 데이터 샘플:", rawData.length > 0 ? rawData[0] : "데이터 없음");

            const formattedData: SubscriptionForTable[] = rawData.map((s) => ({
                ...s,
                id: s.subscriptionId, // subscriptionId를 id로 매핑합니다.
                product: s.product || { productId: 0, name: "알 수 없음", price: 0 }
            }));
            console.log("변환된 데이터 샘플:", formattedData.length > 0 ? formattedData[0] : "데이터 없음");

            setSubscriptions(formattedData);

            // 통계 계산
            const total = formattedData.length;
            const active = formattedData.filter((s) => s.status === 'ACTIVE').length;
            const failed = formattedData.filter((s) => s.status === 'PAYMENT_FAILED').length;

            setStats({ total, active, failed });
            console.log("통계:", { total, active, failed });
            console.log("API 응답 성공");
        } catch (error) {
            console.error("구독 목록 로드 실패:", error);
            toast.error("구독 목록을 불러오는데 실패했습니다.");
            
            setSubscriptions([]);
            setStats({ total: 0, active: 0, failed: 0 });
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        // useEffect에서 async 함수를 직접 호출하는 대신, 반환된 프로미스를 void로 처리하여 경고를 방지합니다.
        void fetchSubscriptions();
    }, []);

    const columns: Column<SubscriptionForTable>[] = [
        { key: "subscriptionId", label: "ID" },
        {
            key: "product",
            label: "상품명",
            render: (item: SubscriptionForTable) => item.product?.name || "-"
        },
        {
            key: "price",
            label: "가격",
            render: (item: SubscriptionForTable) => item.product?.price ? `${item.product.price.toLocaleString()}원` : "-"
        },
        {
            key: "status",
            label: "상태",
            render: (item: SubscriptionForTable) => <StatusBadge status={item.status} />,
        },
        {
            key: "nextBillingAt",
            label: "다음 결제일",
            render: (item: SubscriptionForTable) => item.nextBillingAt ? new Date(item.nextBillingAt).toLocaleDateString() : "-",
        },
        {
            key: "card",
            label: "결제 수단",
            render: (item: SubscriptionForTable) => item.cardCompany ? `${item.cardCompany} (${item.cardNumber})` : "-"
        },
    ];

    return (
        <AdminLayout title="구독 관리" subtitle="기업 회원의 구독 현황을 관리합니다">
            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <StatCard
                    title="전체 구독"
                    value={stats.total.toLocaleString()}
                    icon={CreditCard}
                    iconColor="bg-blue-500/10 text-blue-500"
                />
                <StatCard
                    title="활성 구독"
                    value={stats.active.toLocaleString()}
                    icon={CheckCircle}
                    iconColor="bg-green-500/10 text-green-500"
                />
                <StatCard
                    title="결제 실패"
                    value={stats.failed.toLocaleString()}
                    icon={XCircle}
                    iconColor="bg-red-500/10 text-red-500"
                />
            </div>

            {/* Table */}
            {loading ? (
                <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={subscriptions}
                    actions={[
                        { label: "상세보기", onClick: (item) => console.log("View", item) },
                        { label: "구독 중지", onClick: (item) => console.log("Cancel", item) },
                    ]}
                />
            )}
        </AdminLayout>
    );
};

export default AdminSubscription;
