import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Search } from "lucide-react";
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

type StatusTab = "ALL" | "ACTIVE" | "SUSPENDED";

const AdminMembers = () => {
    const navigate = useNavigate();
    const [members, setMembers] = useState<Member[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [search, setSearch] = useState("");
    const [statusTab, setStatusTab] = useState<StatusTab>("ALL");

    const fetchMembers = async () => {
        setLoading(true);
        try {
            const statusParam = statusTab === "ALL" ? undefined : statusTab;
            const data = await getMembers({
                page,
                size: 20,
                search: search || undefined,
                status: statusParam,
            });
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
    }, [page, statusTab]);

    const handleSearch = () => {
        setPage(0);
        fetchMembers();
    };

    const handleStatusTabChange = (value: string) => {
        setStatusTab(value as StatusTab);
        setPage(0);
    };

    const handleViewProfile = (member: Member) => {
        // 구직자는 인재풀 프로필로, 기업회원 등은 회원 프로필 페이지로
        if (member.role === "CANDIDATE") {
            navigate(`/talents/${member.memberId}`);
        } else {
            navigate(`/admin/members/${member.memberId}`);
        }
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
                    onPageChange={setPage}
                    actions={[
                        { label: "프로필 보기", onClick: (item) => handleViewProfile(item) },
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
