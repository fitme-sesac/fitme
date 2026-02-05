import { useState, useEffect } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Search, Plus } from "lucide-react";
import { getAdminNotices, deleteNotice } from "@/api/admin";
import { CreateNoticeModal } from "@/components/admin/CreateNoticeModal";
import { NoticeDetailEditModal } from "@/components/admin/NoticeDetailEditModal";
import { toast } from "sonner";

interface Notice {
    id: number;
    title: string;
    body: string;
    noticeType: string;
    status: string;
    createdAt: string;
    createdBy: number;
}

type NoticeTypeTab = "ALL" | "OPS" | "TERMS" | "PRIVACY" | "POLICY";

const AdminCommunity = () => {
    const [posts, setPosts] = useState<Notice[]>([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [noticeTypeTab, setNoticeTypeTab] = useState<NoticeTypeTab>("ALL");
    const [detailModalOpen, setDetailModalOpen] = useState(false);
    const [selectedNoticeId, setSelectedNoticeId] = useState<number | null>(null);

    const fetchPosts = async () => {
        setLoading(true);
        try {
            const noticeTypeParam =
                noticeTypeTab === "ALL" ? undefined : noticeTypeTab;
            const data = await getAdminNotices({
                page,
                size: 10,
                noticeType: noticeTypeParam,
            });
            setPosts(data.content || []);
            setTotal(data.totalElements || 0);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPosts();
    }, [page, noticeTypeTab]);

    const handleNoticeTypeTabChange = (value: string) => {
        setNoticeTypeTab(value as NoticeTypeTab);
        setPage(0);
    };

    const handleViewDetail = (item: Notice) => {
        setSelectedNoticeId(item.id);
        setDetailModalOpen(true);
    };

    const handleDelete = async (item: Notice) => {
        if (!window.confirm(`"${item.title}" 공지를 삭제하시겠습니까?`)) return;
        try {
            await deleteNotice(item.id);
            toast.success("삭제되었습니다.");
            fetchPosts();
        } catch (error) {
            console.error(error);
            toast.error("삭제에 실패했습니다.");
        }
    };

    const columns = [
        { key: "id", label: "ID" },
        { key: "title", label: "제목" },
        { key: "noticeType", label: "유형" },
        {
            key: "status",
            label: "상태",
            render: (item: Notice) => <StatusBadge status={item.status} />,
        },
        { key: "createdAt", label: "작성일", render: (item: Notice) => new Date(item.createdAt).toLocaleDateString() },
    ];

    return (
        <AdminLayout title="커뮤니티/공지 관리" subtitle="공지사항 및 게시글을 관리합니다">
            {/* Type Tabs */}
            <Tabs
                value={noticeTypeTab}
                onValueChange={handleNoticeTypeTabChange}
                className="mb-6"
            >
                <TabsList>
                    <TabsTrigger value="ALL">전체</TabsTrigger>
                    <TabsTrigger value="OPS">운영 공지</TabsTrigger>
                    <TabsTrigger value="TERMS">이용약관</TabsTrigger>
                    <TabsTrigger value="PRIVACY">개인정보 처리방침</TabsTrigger>
                    <TabsTrigger value="POLICY">정책</TabsTrigger>
                </TabsList>
            </Tabs>

            {/* Actions */}
            <div className="flex gap-4 mb-6">
                <div className="relative flex-1 max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                        placeholder="게시글 검색..."
                        className="pl-9"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                </div>
                <Button onClick={() => setIsCreateModalOpen(true)}>
                    <Plus className="w-4 h-4 mr-2" />
                    공지사항 작성
                </Button>
            </div>

            {/* Table */}
            {loading ? (
                <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={posts}
                    totalItems={total}
                    currentPage={page}
                    onPageChange={setPage}
                    actions={[
                        { label: "상세보기", onClick: handleViewDetail },
                        { label: "숨김", onClick: (item) => console.log("Hide", item) },
                        { label: "삭제", onClick: handleDelete },
                    ]}
                />
            )}

            <CreateNoticeModal
                open={isCreateModalOpen}
                onOpenChange={setIsCreateModalOpen}
                onSuccess={fetchPosts}
            />
            <NoticeDetailEditModal
                open={detailModalOpen}
                onOpenChange={setDetailModalOpen}
                noticeId={selectedNoticeId}
                onSuccess={fetchPosts}
            />
        </AdminLayout>
    );
};

export default AdminCommunity;
