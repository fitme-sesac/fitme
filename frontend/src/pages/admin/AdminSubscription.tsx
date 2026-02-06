import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { StatCard } from "@/components/admin/StatCard";
import { CreditCard, CheckCircle, XCircle } from "lucide-react";
import { getAdminSubscriptions } from "@/api/admin";
import { toast } from "sonner";

interface Subscription {
    subscriptionId: number;
    employerId: number;
    product: {
        name: string;
        price: number;
    };
    status: string;
    startedAt: string;
    nextBillingAt: string;
    cardCompany: string;
    cardNumber: string;
}

const AdminSubscription = () => {
    const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState({ total: 0, active: 0, failed: 0 });

    const fetchSubscriptions = async () => {
        setLoading(true);
        try {
            const response = await getAdminSubscriptions();
            // Handle both List and Page responses, and ensure array
            const rawData = Array.isArray(response) ? response : (response?.content || []);

            // Map subscriptionId to id for DataTable
            const formattedData = rawData.map((s: any) => ({
                ...s,
                id: s.subscriptionId
            }));

            setSubscriptions(formattedData);

            const total = formattedData.length;
            const active = formattedData.filter((s: any) => s.status === 'ACTIVE').length;
            const failed = formattedData.filter((s: any) => s.status === 'PAYMENT_FAILED').length;

            setStats({ total, active, failed });
        } catch (error) {
            console.error("구독 목록 로드 실패:", error);
            toast.error("구독 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSubscriptions();
    }, []);

    const columns = [
        { key: "subscriptionId", label: "ID" },
        {
            key: "product",
            label: "상품명",
            render: (item: Subscription) => item.product?.name || "-"
        },
        {
            key: "price",
            label: "가격",
            render: (item: Subscription) => item.product?.price ? `${item.product.price.toLocaleString()}원` : "-"
        },
        {
            key: "status",
            label: "상태",
            render: (item: Subscription) => <StatusBadge status={item.status} />,
        },
        {
            key: "nextBillingAt",
            label: "다음 결제일",
            render: (item: Subscription) => item.nextBillingAt ? new Date(item.nextBillingAt).toLocaleDateString() : "-",
        },
        {
            key: "card",
            label: "결제 수단",
            render: (item: Subscription) => item.cardCompany ? `${item.cardCompany} (${item.cardNumber})` : "-"
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
