import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { StatCard } from "@/components/admin/StatCard";
import { MessageSquare, Clock, CheckCircle } from "lucide-react";
import { getInquiries, getFAQStatistics } from "@/api/admin";
import { toast } from "sonner";

interface Inquiry {
    id: number;
    question: string;
    category: string;
    status: string;
    createdAt: string;
}

const AdminInquiries = () => {
    const [inquiries, setInquiries] = useState<Inquiry[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [stats, setStats] = useState({ total: 0, publicCount: 0, privateCount: 0 });

    const fetchInquiries = async () => {
        setLoading(true);
        try {
            const [data, statsData] = await Promise.all([
                getInquiries({ page, size: 20 }),
                getFAQStatistics()
            ]);

            setInquiries(data.content?.map((i: any) => ({
                ...i,
                id: i.id,
                category: "일반",
                status: i.isPublic ? "ACTIVE" : "HIDDEN"
            })) || []);
            setTotal(data.totalElements || 0);

            // Backend returns: totalCount, publicCount, lockedCount
            if (statsData) {
                setStats({
                    total: statsData.totalCount || 0,
                    publicCount: statsData.publicCount || 0,
                    privateCount: (statsData.totalCount || 0) - (statsData.publicCount || 0)
                });
            }
        } catch (error) {
            console.error("문의 목록 로드 실패:", error);
            toast.error("문의 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchInquiries();
    }, [page]);

    const columns = [
        { key: "id", label: "ID" },
        { key: "question", label: "질문" },
        { key: "category", label: "카테고리" },
        {
            key: "status",
            label: "상태",
            render: (item: Inquiry) => <StatusBadge status={item.status} />,
        },
        {
            key: "createdAt",
            label: "등록일",
            render: (item: Inquiry) => item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-",
        },
    ];

    return (
        <AdminLayout title="문의 관리" subtitle="사용자 문의와 FAQ를 관리합니다">
            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <StatCard
                    title="전체 문의 (FAQ)"
                    value={stats.total.toLocaleString()}
                    icon={MessageSquare}
                    iconColor="bg-blue-500/10 text-blue-500"
                />
                <StatCard
                    title="공개됨"
                    value={stats.publicCount.toLocaleString()}
                    icon={CheckCircle}
                    iconColor="bg-green-500/10 text-green-500"
                />
                <StatCard
                    title="비공개"
                    value={stats.privateCount.toLocaleString()}
                    icon={Clock}
                    iconColor="bg-yellow-500/10 text-yellow-500"
                />
            </div>

            {/* Table */}
            {loading ? (
                <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={inquiries}
                    totalItems={total}
                    currentPage={page}
                    onPageChange={setPage}
                    actions={[
                        { label: "상세보기", onClick: (item) => console.log("View", item) },
                        { label: "수정", onClick: (item) => console.log("Edit", item) },
                        { label: "삭제", onClick: (item) => console.log("Delete", item) },
                    ]}
                />
            )}
        </AdminLayout>
    );
};

export default AdminInquiries;
