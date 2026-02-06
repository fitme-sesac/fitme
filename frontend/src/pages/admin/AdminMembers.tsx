import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Search } from "lucide-react";
import { getMembers, updateMemberStatus } from "@/api/admin";
import { toast } from "sonner";

type MemberStatus = "ACTIVE" | "SUSPENDED";
type MemberRole = "CANDIDATE" | "EMPLOYER" | "ADMIN";

interface Member {
    id: number;
    memberId: number;
    name: string;
    email: string;
    phone: string;
    role: MemberRole;
    status: MemberStatus;
    createdAt: string;
}

type StatusTab = "ALL" | "ACTIVE" | "SUSPENDED";

const AdminMembers = () => {
    const navigate = useNavigate();
    const [members, setMembers] = useState<Member[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [search, setSearch] = useState("");
    const [statusTab, setStatusTab] = useState<StatusTab>("ALL");

    const fetchMembers = useCallback(async () => {
        setLoading(true);
        try {
            const statusParam = statusTab === "ALL" ? undefined : statusTab;
            const data = await getMembers({
                page,
                size: 20,
                search: search || undefined,
                status: statusParam,
            });
            const membersWithId = data.content?.map((m: Omit<Member, 'id'>) => ({ ...m, id: m.memberId })) || [];
            setMembers(membersWithId);
            setTotal(data.totalElements || 0);
        } catch (error) {
            console.error("회원 목록 로드 실패:", error);
            toast.error("회원 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    }, [page, statusTab, search]);

    useEffect(() => {
        void fetchMembers();
    }, [fetchMembers]);

    const handleSearch = () => {
        setPage(0);
        void fetchMembers();
    };

    const handleStatusTabChange = (value: string) => {
        setStatusTab(value as StatusTab);
        setPage(0);
    };

    const handleViewProfile = (member: Member) => {
        if (member.role === "CANDIDATE") {
            navigate(`/talents/${member.memberId}`);
        } else {
            navigate(`/admin/members/${member.memberId}`);
        }
    };

    const handleStatusChange = async (member: Member, status: MemberStatus) => {
        try {
            await updateMemberStatus(member.memberId, status);
            toast.success("회원 상태가 변경되었습니다.");
            void fetchMembers();
        } catch (error) {
            toast.error("상태 변경에 실패했습니다.");
        }
    };

    const columns: Column<Member>[] = [
        { key: "memberId", label: "ID" },
        { key: "name", label: "이름" },
        { key: "email", label: "이메일" },
        { key: "phone", label: "연락처" },
        { key: "role", label: "역할" },
        {
            key: "status",
            label: "상태",
            render: (item: Member) => <StatusBadge status={item.status} />,
        },
        {
            key: "createdAt",
            label: "가입일",
            render: (item: Member) => item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-",
        },
    ];

    return (
        <AdminLayout title="회원 관리" subtitle="서비스 회원을 관리합니다">
            {/* Status Tabs */}
            <Tabs value={statusTab} onValueChange={handleStatusTabChange} className="mb-6">
                <TabsList>
                    <TabsTrigger value="ALL">전체</TabsTrigger>
                    <TabsTrigger value="ACTIVE">활성</TabsTrigger>
                    <TabsTrigger value="SUSPENDED">정지</TabsTrigger>
                </TabsList>
            </Tabs>

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
                    pageSize={20}
                    onPageChange={setPage}
                    actions={[
                        { label: "프로필 보기", onClick: (item: Member) => handleViewProfile(item) },
                        {
                            label: "정지",
                            onClick: (item: Member) => handleStatusChange(item, "SUSPENDED"),
                            disabled: (item: Member) => item.status === "SUSPENDED",
                        },
                        {
                            label: "활성화",
                            onClick: (item: Member) => handleStatusChange(item, "ACTIVE"),
                            disabled: (item: Member) => item.status === "ACTIVE",
                        },
                    ]}
                />
            )}
        </AdminLayout>
    );
};

export default AdminMembers;
