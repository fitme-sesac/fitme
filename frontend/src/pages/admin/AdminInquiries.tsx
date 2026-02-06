import { useEffect, useState, useCallback } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { StatCard } from "@/components/admin/StatCard";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MessageSquare, Clock, CheckCircle, Search } from "lucide-react";
import { getInquiries, getFAQStatistics, deleteFAQ } from "@/api/admin";
import { FAQDetailEditModal } from "@/components/admin/FAQDetailEditModal";
import { toast } from "sonner";

type InquiryStatus = "PUBLIC" | "PRIVATE";

interface Inquiry {
    id: number;
    question: string;
    isPublic: boolean;
    status: InquiryStatus;
    createdAt: string;
}

type PublicTab = "ALL" | "PUBLIC" | "PRIVATE";

const AdminInquiries = () => {
    const [inquiries, setInquiries] = useState<Inquiry[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [stats, setStats] = useState({ total: 0, publicCount: 0, privateCount: 0 });
    const [publicTab, setPublicTab] = useState<PublicTab>("ALL");
    const [search, setSearch] = useState("");
    const [detailModalOpen, setDetailModalOpen] = useState(false);
    const [selectedFaqId, setSelectedFaqId] = useState<number | null>(null);

    const fetchInquiries = useCallback(async () => {
        setLoading(true);
        try {
            const isPublicParam = publicTab === "ALL" ? undefined : publicTab === "PUBLIC";
            const [data, statsData] = await Promise.all([
                getInquiries({ page, size: 20, isPublic: isPublicParam, question: search || undefined }),
                getFAQStatistics(),
            ]);

            setInquiries(
                data.content?.map((i: { id: number; question: string; isPublic: boolean; createdAt: string }) => ({
                    ...i,
                    status: i.isPublic ? "PUBLIC" : "PRIVATE",
                })) ?? []
            );
            setTotal(data.totalElements ?? 0);

            if (statsData) {
                setStats({
                    total: statsData.totalCount ?? 0,
                    publicCount: statsData.publicCount ?? 0,
                    privateCount: (statsData.totalCount ?? 0) - (statsData.publicCount ?? 0),
                });
            }
        } catch (error) {
            console.error("문의 목록 로드 실패:", error);
            toast.error("문의 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    }, [page, publicTab, search]);

    useEffect(() => {
        void fetchInquiries();
    }, [fetchInquiries]);

    const handleSearch = () => {
        setPage(0);
        void fetchInquiries();
    };

    const handlePublicTabChange = (value: string) => {
        setPublicTab(value as PublicTab);
        setPage(0);
    };

    const handleViewDetail = (item: Inquiry) => {
        setSelectedFaqId(item.id);
        setDetailModalOpen(true);
    };

    const handleDelete = async (item: Inquiry) => {
        if (!window.confirm(`"${item.question}" FAQ를 삭제하시겠습니까?`)) return;
        try {
            await deleteFAQ(item.id);
            toast.success("삭제되었습니다.");
            void fetchInquiries();
        } catch (error) {
            console.error("FAQ 삭제 실패:", error);
            toast.error("삭제에 실패했습니다.");
        }
    };

    const columns: Column<Inquiry>[] = [
        { key: "id", label: "ID" },
        { key: "question", label: "질문" },
        {
            key: "status",
            label: "상태",
            render: (item: Inquiry) => <StatusBadge status={item.status} />,
        },
        {
            key: "createdAt",
            label: "등록일",
            render: (item: Inquiry) =>
                item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-",
        },
    ];

    return (
        <AdminLayout title="문의 관리" subtitle="사용자 문의와 FAQ를 관리합니다">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
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

            <Tabs value={publicTab} onValueChange={handlePublicTabChange} className="mb-6">
                <TabsList>
                    <TabsTrigger value="ALL">전체</TabsTrigger>
                    <TabsTrigger value="PUBLIC">공개</TabsTrigger>
                    <TabsTrigger value="PRIVATE">비공개</TabsTrigger>
                </TabsList>
            </Tabs>

            <div className="flex gap-4 mb-6">
                <div className="relative flex-1 max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                        placeholder="질문 검색..."
                        className="pl-9"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                    />
                </div>
                <Button onClick={handleSearch}>검색</Button>
            </div>

            {loading ? (
                <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={inquiries}
                    totalItems={total}
                    currentPage={page}
                    pageSize={20}
                    onPageChange={setPage}
                    actions={[
                        { label: "수정", onClick: handleViewDetail },
                        { label: "삭제", onClick: handleDelete },
                    ]}
                />
            )}

            {selectedFaqId && (
                <FAQDetailEditModal
                    open={detailModalOpen}
                    onOpenChange={setDetailModalOpen}
                    faqId={selectedFaqId}
                    onSuccess={() => {
                        void fetchInquiries();
                    }}
                />
            )}
        </AdminLayout>
    );
};

export default AdminInquiries;
