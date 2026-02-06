import { useEffect, useState, useCallback } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import ReportDetailModal from "@/components/admin/ReportDetailModal";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AlertTriangle } from "lucide-react";
import { getReports, processReport } from "@/api/admin";
import { toast } from "sonner";

type ReportStatus = "PENDING" | "RESOLVED" | "REJECTED";
type ReportTargetType = "MEMBER" | "JOB";
type ReportReasonCode = "SPAM" | "ABUSE" | "FRAUD" | "ETC";

interface Report {
    id: number;
    reportId: number;
    reporterMemberId: number;
    targetType: ReportTargetType;
    targetId: number;
    targetMemberId?: number;
    targetJobId?: number;
    reasonCode: ReportReasonCode;
    reasonDetail: string;
    status: ReportStatus;
    createdAt: string;
    processedAt?: string;
    adminMemberId?: number;
}

const AdminReports = () => {
    const [reports, setReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(0);
    const [status, setStatus] = useState<ReportStatus>("PENDING");
    const [selectedReport, setSelectedReport] = useState<Report | null>(null);
    const [modalOpen, setModalOpen] = useState(false);

    const fetchReports = useCallback(async () => {
        setLoading(true);
        try {
            const data = await getReports(status, { page, size: 20 });
            const reportsWithId = data.content?.map((r: Omit<Report, 'id' | 'targetId'>) => ({
                ...r,
                id: r.reportId,
                targetId: r.targetMemberId || r.targetJobId || 0
            })) || [];
            setReports(reportsWithId);
            setTotal(data.totalElements || 0);
        } catch (error) {
            console.error("신고 목록 로드 실패:", error);
            toast.error("신고 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    }, [page, status]);

    useEffect(() => {
        void fetchReports();
    }, [fetchReports]);

    const handleProcess = async (report: Report, action: "APPROVE" | "REJECT") => {
        try {
            const requestData = {
                decision: action === "APPROVE" ? "ACCEPT" : "REJECT",
                violationType: action === "APPROVE" ? "MINOR_ETC" : undefined,
                reason: action === "APPROVE" ? "신고 승인 처리" : "신고 거절",
            };
            
            await processReport(report.reportId, requestData);
            toast.success("신고가 처리되었습니다.");
            void fetchReports();
        } catch (error: unknown) {
            console.error("신고 처리 실패:", error);
            let errorMessage = "처리에 실패했습니다.";
            if (error instanceof Error && 'response' in error && error.response && typeof error.response === 'object' && error.response !== null && 'data' in error.response) {
                const responseData = error.response.data as { message?: string; error?: string };
                errorMessage = `처리에 실패했습니다: ${responseData.message || responseData.error || '알 수 없는 오류'}`;
            }
            toast.error(errorMessage);
        }
    };

    const handleViewReport = (report: Report) => {
        setSelectedReport(report);
        setModalOpen(true);
    };
    
    const handleStatusTabChange = (value: string) => {
        setStatus(value as ReportStatus);
        setPage(0);
    };

    const columns: Column<Report>[] = [
        { key: "reportId", label: "ID" },
        { key: "targetType", label: "유형" },
        { key: "targetId", label: "대상 ID" },
        { 
            key: "reasonCode", 
            label: "사유",
            render: (item: Report) => item.reasonDetail || item.reasonCode || "-"
        },
        {
            key: "status",
            label: "상태",
            render: (item: Report) => <StatusBadge status={item.status} />,
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
            {pendingCount > 0 && (
                <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-xl p-4 mb-6 flex items-center gap-3">
                    <AlertTriangle className="w-5 h-5 text-yellow-600" />
                    <span className="text-yellow-700">
                        처리가 필요한 신고가 <strong>{pendingCount}건</strong> 있습니다.
                    </span>
                </div>
            )}

            <Tabs value={status} onValueChange={handleStatusTabChange} className="mb-6">
                <TabsList>
                    <TabsTrigger value="PENDING">대기중</TabsTrigger>
                    <TabsTrigger value="RESOLVED">처리완료</TabsTrigger>
                    <TabsTrigger value="REJECTED">거절됨</TabsTrigger>
                </TabsList>
            </Tabs>

            {loading ? (
                <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={reports}
                    totalItems={total}
                    currentPage={page}
                    pageSize={20}
                    onPageChange={setPage}
                    actions={[
                        { label: "상세보기", onClick: handleViewReport },
                        { label: "승인 (조치)", onClick: (item: Report) => handleProcess(item, "APPROVE"), disabled: (item: Report) => item.status !== 'PENDING' },
                        { label: "거절", onClick: (item: Report) => handleProcess(item, "REJECT"), disabled: (item: Report) => item.status !== 'PENDING' },
                    ]}
                />
            )}

            <ReportDetailModal
                report={selectedReport}
                open={modalOpen}
                onOpenChange={setModalOpen}
                onReportProcessed={() => {
                    void fetchReports();
                }}
            />
        </AdminLayout>
    );
};

export default AdminReports;
