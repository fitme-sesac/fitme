import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import { getAdminJobs, updateJobStatus } from "@/api/admin";
import { toast } from "sonner";

interface Job {
    id: number;
    jobId: number;
    title: string;
    companyName: string;
    status: string;
    viewCount: number;
    createdAt: string;
    deadline: string;
}

const AdminJobs = () => {
    const [jobs, setJobs] = useState<Job[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [search, setSearch] = useState("");

    const fetchJobs = async () => {
        setLoading(true);
        try {
            const data = await getAdminJobs({ page, size: 20, search: search || undefined });
            setJobs(data.content?.map((j: any) => ({ ...j, id: j.jobId })) || []);
            setTotal(data.totalElements || 0);
        } catch (error) {
            console.error("채용공고 목록 로드 실패:", error);
            toast.error("채용공고 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchJobs();
    }, [page]);

    const handleSearch = () => {
        setPage(0);
        fetchJobs();
    };

    const handleStatusChange = async (job: Job, status: string) => {
        try {
            await updateJobStatus(job.jobId, status);
            toast.success("채용공고 상태가 변경되었습니다.");
            fetchJobs();
        } catch (error) {
            toast.error("상태 변경에 실패했습니다.");
        }
    };

    const columns = [
        { key: "jobId", label: "ID" },
        { key: "title", label: "제목" },
        { key: "companyName", label: "기업명" },
        {
            key: "status",
            label: "상태",
            render: (item: Job) => <StatusBadge status={item.status?.toLowerCase() || "open"} />,
        },
        { key: "viewCount", label: "조회수" },
        {
            key: "createdAt",
            label: "등록일",
            render: (item: Job) => item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-",
        },
        {
            key: "deadline",
            label: "마감일",
            render: (item: Job) => item.deadline ? new Date(item.deadline).toLocaleDateString() : "-",
        },
    ];

    return (
        <AdminLayout title="채용공고 관리" subtitle="등록된 채용공고를 관리합니다">
            {/* Search */}
            <div className="flex gap-4 mb-6">
                <div className="relative flex-1 max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                        placeholder="제목 검색..."
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
                    data={jobs}
                    totalItems={total}
                    currentPage={page}
                    onPageChange={setPage}
                    actions={[
                        { label: "상세보기", onClick: (item) => window.open(`/jobs/${item.jobId}`, "_blank") },
                        { label: "승인", onClick: (item) => handleStatusChange(item, "OPEN") },
                        { label: "마감", onClick: (item) => handleStatusChange(item, "CLOSED") },
                        { label: "삭제", onClick: (item) => handleStatusChange(item, "DELETED") },
                    ]}
                />
            )}
        </AdminLayout>
    );
};

export default AdminJobs;
