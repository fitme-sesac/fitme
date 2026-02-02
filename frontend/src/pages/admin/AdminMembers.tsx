import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search, UserX, UserCheck } from "lucide-react";
import { getMembers, updateMemberStatus } from "@/api/admin";
import { toast } from "sonner";

interface Member {
    id: number;
    memberId: number;
    name: string;
    email: string;
    phone: string;
    role: string;
    status: string;
    createdAt: string;
}

const AdminMembers = () => {
    const [members, setMembers] = useState<Member[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [search, setSearch] = useState("");

    const fetchMembers = async () => {
        setLoading(true);
        try {
            const data = await getMembers({ page, size: 20, search: search || undefined });
            setMembers(data.content?.map((m: any) => ({ ...m, id: m.memberId })) || []);
            setTotal(data.totalElements || 0);
        } catch (error) {
            console.error("회원 목록 로드 실패:", error);
            toast.error("회원 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMembers();
    }, [page]);

    const handleSearch = () => {
        setPage(0);
        fetchMembers();
    };

    const handleStatusChange = async (member: Member, status: string) => {
        try {
            await updateMemberStatus(member.memberId, status);
            toast.success("회원 상태가 변경되었습니다.");
            fetchMembers();
        } catch (error) {
            toast.error("상태 변경에 실패했습니다.");
        }
    };

    const columns = [
        { key: "memberId", label: "ID" },
        { key: "name", label: "이름" },
        { key: "email", label: "이메일" },
        { key: "phone", label: "연락처" },
        { key: "role", label: "역할" },
        {
            key: "status",
            label: "상태",
            render: (item: Member) => <StatusBadge status={item.status?.toLowerCase() || "active"} />,
        },
        {
            key: "createdAt",
            label: "가입일",
            render: (item: Member) => item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-",
        },
    ];

    return (
        <AdminLayout title="회원 관리" subtitle="서비스 회원을 관리합니다">
            {/* Search */}
            <div className="flex gap-4 mb-6">
                <div className="relative flex-1 max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                        placeholder="이름, 이메일 검색..."
                        className="pl-9"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                    />
                </div>
                <Button onClick={handleSearch}>검색</Button>
            </div>

            {/* Table */}
            {loading ? (
                <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={members}
                    totalItems={total}
                    currentPage={page}
                    onPageChange={setPage}
                    actions={[
                        { label: "프로필 보기", onClick: (item) => console.log("View", item) },
                        {
                            label: "정지",
                            onClick: (item) => handleStatusChange(item, "SUSPENDED"),
                        },
                        {
                            label: "활성화",
                            onClick: (item) => handleStatusChange(item, "ACTIVE"),
                        },
                    ]}
                />
            )}
        </AdminLayout>
    );
};

export default AdminMembers;
