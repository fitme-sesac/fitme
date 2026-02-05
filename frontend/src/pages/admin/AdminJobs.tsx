import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Search } from "lucide-react";
import { getAdminJobs, updateJobStatus, deleteJobPermanent } from "@/api/admin";
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

type StatusTab = "ALL" | "OPEN" | "CLOSED" | "DELETED";

const AdminJobs = () => {
    const [jobs, setJobs] = useState<Job[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [search, setSearch] = useState("");
    const [statusTab, setStatusTab] = useState<StatusTab>("ALL");

    const fetchJobs = async () => {
        setLoading(true);
        try {
            const statusParam = statusTab === "ALL" ? undefined : statusTab;
            const data = await getAdminJobs({
                page,
                size: 20,
                search: search || undefined,
                status: statusParam,
            });
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
    }, [page, statusTab]);

    const handleSearch = () => {
        setPage(0);
        fetchJobs();
    };

    const handleStatusTabChange = (value: string) => {
        setStatusTab(value as StatusTab);
        setPage(0);
    };

    const handleStatusChange = async (job: Job, status: string) => {
        try {
            await updateJobStatus(job.jobId, status);
            toast.success(status === "DELETED" ? "삭제되었습니다. 삭제 탭에서 확인할 수 있습니다." : "채용공고 상태가 변경되었습니다.");
            fetchJobs();
        } catch (error) {
            toast.error("상태 변경에 실패했습니다.");
        }
    };

    const handlePermanentDelete = async (job: Job) => {
        if (!window.confirm("이 공고를 완전히 삭제하시겠습니까? 복구할 수 없습니다.")) return;
        try {
            await deleteJobPermanent(job.jobId);
            toast.success("완전 삭제되었습니다.");
            fetchJobs();
        } catch (error: unknown) {
            const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
            toast.error(msg || "완전 삭제에 실패했습니다.");
        }
    };

    const handleRestore = async (job: Job) => {
        try {
            await updateJobStatus(job.jobId, "OPEN");
            toast.success("복구되었습니다. 전체/승인 탭에서 확인할 수 있습니다.");
            fetchJobs();
        } catch (error) {
            toast.error("복구에 실패했습니다.");
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
            {/* Status Tabs */}
            <Tabs value={statusTab} onValueChange={handleStatusTabChange} className="mb-6">
                <TabsList>
                    <TabsTrigger value="ALL">전체</TabsTrigger>
                    <TabsTrigger value="OPEN">승인</TabsTrigger>
                    <TabsTrigger value="CLOSED">마감</TabsTrigger>
                    <TabsTrigger value="DELETED">삭제</TabsTrigger>
                </TabsList>
            </Tabs>

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
                    actions={
                        statusTab === "DELETED"
                            ? [
                                  { label: "상세보기", onClick: (item) => window.open(`/jobs/${item.jobId}`, "_blank") },
                                  { label: "복구", onClick: (item) => handleRestore(item) },
                                  { label: "완전삭제", onClick: (item) => handlePermanentDelete(item) },
                              ]
                            : [
                                  { label: "상세보기", onClick: (item) => window.open(`/jobs/${item.jobId}`, "_blank") },
                                  { label: "승인", onClick: (item) => handleStatusChange(item, "OPEN") },
                                  { label: "마감", onClick: (item) => handleStatusChange(item, "CLOSED") },
                                  { label: "삭제", onClick: (item) => handleStatusChange(item, "DELETED") },
                              ]
                    }
                />
            )}
        </AdminLayout>
    );
};

export default AdminJobs;
