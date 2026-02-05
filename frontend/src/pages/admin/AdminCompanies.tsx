import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Search } from "lucide-react";
import { getEmployers, verifyEmployer } from "@/api/admin";
import { toast } from "sonner";

interface Employer {
    id: number;
    employerId: number;
    companyName: string;
    status: string;
    createdAt: string;
}

type StatusTab = "ALL" | "ACTIVE" | "REJECTED";

const AdminCompanies = () => {
    const navigate = useNavigate();
    const [companies, setCompanies] = useState<Employer[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [search, setSearch] = useState("");
    const [statusTab, setStatusTab] = useState<StatusTab>("ALL");

    const fetchCompanies = async () => {
        setLoading(true);
        try {
            const statusParam = statusTab === "ALL" ? undefined : statusTab;
            const data = await getEmployers({
                page,
                size: 20,
                search: search || undefined,
                status: statusParam,
            });
            setCompanies(data.content?.map((e: any) => ({ ...e, id: e.employerId })) || []);
            setTotal(data.totalElements || 0);
        } catch (error) {
            console.error("기업 목록 로드 실패:", error);
            toast.error("기업 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCompanies();
    }, [page, statusTab]);

    const handleSearch = () => {
        setPage(0);
        fetchCompanies();
    };

    const handleStatusTabChange = (value: string) => {
        setStatusTab(value as StatusTab);
        setPage(0);
    };

    const handleViewProfile = (company: Employer) => {
        navigate(`/admin/companies/${company.employerId}`);
    };

    const handleVerify = async (company: Employer, action: 'APPROVE' | 'REJECT') => {
        try {
            await verifyEmployer(company.employerId, action);
            toast.success(action === 'APPROVE' ? "기업이 승인되었습니다." : "기업이 거절되었습니다.");
            fetchCompanies();
        } catch (error) {
            toast.error("처리에 실패했습니다.");
        }
    };

    const columns = [
        { key: "employerId", label: "ID" },
        { key: "companyName", label: "기업명" },
        {
            key: "status",
            label: "상태",
            render: (item: Employer) => <StatusBadge status={item.status?.toLowerCase() || "active"} />,
        },
        {
            key: "createdAt",
            label: "등록일",
            render: (item: Employer) => item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-",
        },
    ];

    return (
        <AdminLayout title="기업 관리" subtitle="등록된 기업을 관리하고 인증을 처리합니다">
            {/* Status Tabs */}
            <Tabs value={statusTab} onValueChange={handleStatusTabChange} className="mb-6">
                <TabsList>
                    <TabsTrigger value="ALL">전체</TabsTrigger>
                    <TabsTrigger value="ACTIVE">승인</TabsTrigger>
                    <TabsTrigger value="REJECTED">거절</TabsTrigger>
                </TabsList>
            </Tabs>

            {/* Search */}
            <div className="flex gap-4 mb-6">
                <div className="relative flex-1 max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                        placeholder="기업명 검색..."
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
                    data={companies}
                    totalItems={total}
                    currentPage={page}
                    onPageChange={setPage}
                    actions={[
                        { label: "상세보기", onClick: (item) => handleViewProfile(item) },
                        { label: "승인", onClick: (item) => handleVerify(item, 'APPROVE') },
                        { label: "거절", onClick: (item) => handleVerify(item, 'REJECT') },
                    ]}
                />
            )}
        </AdminLayout>
    );
};

export default AdminCompanies;
