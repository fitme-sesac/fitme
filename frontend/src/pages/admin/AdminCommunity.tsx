import { useState, useEffect } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search, Plus } from "lucide-react";
import { getAdminNotices } from "@/api/admin";
import { CreateNoticeModal } from "@/components/admin/CreateNoticeModal";

interface Notice {
    id: number;
    title: string;
    body: string;
    noticeType: string;
    status: string;
    createdAt: string;
    createdBy: number;
}

const AdminCommunity = () => {
    const [posts, setPosts] = useState<Notice[]>([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    const fetchPosts = async () => {
        setLoading(true);
        try {
            // 현재는 공지사항(Notices) API만 연동. 추후 커뮤니티 게시글 API 별도 연동 필요.
            const data = await getAdminNotices({ page, size: 10 });
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
    }, [page]);

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
                        { label: "상세보기", onClick: (item) => console.log("View", item) },
                        { label: "숨김", onClick: (item) => console.log("Hide", item) },
                        { label: "삭제", onClick: (item) => console.log("Delete", item) },
                    ]}
                />
            )}

            <CreateNoticeModal
                open={isCreateModalOpen}
                onOpenChange={setIsCreateModalOpen}
                onSuccess={fetchPosts}
            />
        </AdminLayout>
    );
};

export default AdminCommunity;
