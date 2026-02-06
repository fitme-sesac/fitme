import { useState, useEffect, useCallback } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Search, Plus } from "lucide-react";
import { getAdminNotices, deleteNotice } from "@/api/admin";
import { CreateNoticeModal } from "@/components/admin/CreateNoticeModal";
import { NoticeDetailEditModal } from "@/components/admin/NoticeDetailEditModal";
import { toast } from "sonner";

type NoticeType = "OPS" | "TERMS" | "PRIVACY" | "POLICY";
type NoticeStatus = "PUBLISHED" | "DRAFT" | "ARCHIVED";

interface Notice {
    id: number;
    title: string;
    body: string;
    noticeType: NoticeType;
    status: NoticeStatus;
    createdAt: string;
    createdBy: number;
}

type NoticeTypeTab = "ALL" | NoticeType;

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

    const fetchPosts = useCallback(async () => {
        setLoading(true);
        try {
            const noticeTypeParam = noticeTypeTab === "ALL" ? undefined : noticeTypeTab;
            const data = await getAdminNotices({
                page,
                size: 10,
                noticeType: noticeTypeParam,
                title: search || undefined,
            });
            setPosts(data.content || []);
            setTotal(data.totalElements || 0);
        } catch (error) {
            console.error("공지 목록 로드 실패:", error);
            toast.error("공지 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    }, [page, noticeTypeTab, search]);

    useEffect(() => {
        void fetchPosts();
    }, [fetchPosts]);
    
    const handleSearch = () => {
        setPage(0);
        void fetchPosts();
    };

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
            void fetchPosts();
        } catch (error) {
            console.error("공지 삭제 실패:", error);
            toast.error("삭제에 실패했습니다.");
        }
    };

    const columns: Column<Notice>[] = [
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

            <div className="flex gap-4 mb-6">
                <div className="relative flex-1 max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                        placeholder="제목으로 검색..."
                        className="pl-9"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                    />
                </div>
                <Button onClick={handleSearch}>검색</Button>
                <Button onClick={() => setIsCreateModalOpen(true)}>
                    <Plus className="w-4 h-4 mr-2" />
                    공지사항 작성
                </Button>
            </div>

            {loading ? (
                <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={posts}
                    totalItems={total}
                    currentPage={page}
                    pageSize={10}
                    onPageChange={setPage}
                    actions={[
                        { label: "수정", onClick: handleViewDetail },
                        { label: "삭제", onClick: handleDelete },
                    ]}
                />
            )}

            <CreateNoticeModal
                open={isCreateModalOpen}
                onOpenChange={setIsCreateModalOpen}
                onSuccess={() => {
                    void fetchPosts();
                }}
            />
            {selectedNoticeId && (
                <NoticeDetailEditModal
                    open={detailModalOpen}
                    onOpenChange={setDetailModalOpen}
                    noticeId={selectedNoticeId}
                    onSuccess={() => {
                        void fetchPosts();
                    }}
                />
            )}
        </AdminLayout>
    );
};

export default AdminCommunity;
