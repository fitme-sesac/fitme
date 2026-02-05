import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AlertTriangle } from "lucide-react";
import { getReports, processReport } from "@/api/admin";
import { toast } from "sonner";

interface Report {
    id: number;
    reportId: number;
    reporterMemberId: number;
    targetType: string;
    targetId: number;
    reason: string;
    status: string;
    createdAt: string;
    // Missing fields populated manually
}

const AdminReports = () => {
    const [reports, setReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [status, setStatus] = useState("PENDING");

    const fetchReports = async () => {
        setLoading(true);
        try {
            const data = await getReports(status, { page, size: 20 });
            setReports(data.content?.map((r: any) => ({
                ...r,
                id: r.reportId,
                reason: r.reasonDetail || r.reasonCode, // Map detail or code to reason
                targetId: r.targetMemberId || r.targetJobId // Pick whichever is present
            })) || []);
            setTotal(data.totalElements || 0);
        } catch (error) {
            console.error("신고 목록 로드 실패:", error);
            toast.error("신고 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchReports();
    }, [page, status]);

    const handleProcess = async (report: Report, action: "APPROVE" | "REJECT") => {
        try {
            await processReport(report.reportId, {
                decision: action === "APPROVE" ? "ACCEPT" : "REJECT",
                violationType: action === "APPROVE" ? "MINOR_ETC" : undefined,
                reason: action === "APPROVE" ? "신고 승인 처리" : "신고 거절",
            });
            toast.success("신고가 처리되었습니다.");
            fetchReports();
        } catch (error) {
            toast.error("처리에 실패했습니다.");
        }
    };

    const columns = [
        { key: "reportId", label: "ID" },
        { key: "targetType", label: "유형" },
        { key: "targetId", label: "대상 ID" },
        { key: "reason", label: "사유" },
        {
            key: "status",
            label: "상태",
            render: (item: Report) => <StatusBadge status={item.status?.toLowerCase()} />,
        },
        {
            key: "createdAt",
            label: "신고일",
            render: (item: Report) => item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-",
        },
    ];

    const pendingCount = status === "PENDING" ? total : 0;

    return (
        <AdminLayout title="신고 관리" subtitle="사용자 신고를 확인하고 처리합니다">
            {/* Warning Banner */}
            {pendingCount > 0 && (
                <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-xl p-4 mb-6 flex items-center gap-3">
                    <AlertTriangle className="w-5 h-5 text-yellow-600" />
                    <span className="text-yellow-700">
                        처리가 필요한 신고가 <strong>{pendingCount}건</strong> 있습니다.
                    </span>
                </div>
            )}

            {/* Status Tabs */}
            <Tabs value={status} onValueChange={setStatus} className="mb-6">
                <TabsList>
                    <TabsTrigger value="PENDING">대기중</TabsTrigger>
                    <TabsTrigger value="RESOLVED">처리완료</TabsTrigger>
                    <TabsTrigger value="REJECTED">거절됨</TabsTrigger>
                </TabsList>
            </Tabs>

            {/* Table */}
            {loading ? (
                <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={reports}
                    totalItems={total}
                    currentPage={page}
                    onPageChange={setPage}
                    actions={[
                        { label: "상세보기", onClick: (item) => console.log("View", item) },
                        { label: "승인 (조치)", onClick: (item) => handleProcess(item, "APPROVE") },
                        { label: "거절", onClick: (item) => handleProcess(item, "REJECT") },
                    ]}
                />
            )}
        </AdminLayout>
    );
};

export default AdminReports;
