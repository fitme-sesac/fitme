import { useEffect, useState, useCallback } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { StatCard } from "@/components/admin/StatCard";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import ReportDetailModal from "@/components/admin/ReportDetailModal";
import { Users, Briefcase, Building2, Flag, TrendingUp, Clock } from "lucide-react";
import { getAdminStats, getReports } from "@/api/admin";
import { toast } from "sonner";

interface DashboardStats {
    totalMembers: number;
    totalJobs: number;
    totalCompanies: number;
    totalRevenue: number;
    pendingReports: number;
    todayApplications: number;
    weeklyNewMembers: number;
}

type ReportStatus = "PENDING" | "RESOLVED" | "REJECTED";
type ReportTargetType = "MEMBER" | "JOB";

interface Report {
    id: number;
    reportId: number;
    reporterMemberId: number;
    targetType: ReportTargetType;
    targetId: number;
    targetMemberId?: number;
    targetJobId?: number;
    reasonCode: string;
    reasonDetail: string;
    status: ReportStatus;
    createdAt: string;
    processedAt?: string;
    adminMemberId?: number;
}

const AdminDashboard = () => {
    const [stats, setStats] = useState<DashboardStats>({
        totalMembers: 0,
        totalJobs: 0,
        totalCompanies: 0,
        totalRevenue: 0,
        pendingReports: 0,
        todayApplications: 0,
        weeklyNewMembers: 0,
    });
    const [recentReports, setRecentReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedReport, setSelectedReport] = useState<Report | null>(null);
    const [modalOpen, setModalOpen] = useState(false);

    const fetchData = useCallback(async () => {
        setLoading(true);
        try {
            const [statsData, reportsData] = await Promise.all([
                getAdminStats(),
                getReports("PENDING", { page: 0, size: 5 }),
            ]);
            setStats(statsData);
            
            const mappedReports = (reportsData.content || []).map((r: Omit<Report, 'id' | 'targetId'>) => ({
                ...r,
                id: r.reportId,
                targetId: r.targetMemberId || r.targetJobId || 0,
            }));
            setRecentReports(mappedReports);
        } catch (error) {
            console.error("대시보드 데이터 로드 실패:", error);
            toast.error("대시보드 데이터를 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void fetchData();
    }, [fetchData]);

    const handleViewReport = (report: Report) => {
        setSelectedReport(report);
        setModalOpen(true);
    };

    const reportColumns: Column<Report>[] = [
        { key: "reportId", label: "ID" },
        { key: "targetType", label: "유형" },
        {
            key: "status",
            label: "상태",
            render: (item: Report) => <StatusBadge status={item.status} />
        },
        { 
            key: "createdAt", 
            label: "신고일",
            render: (item: Report) => item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-"
        },
    ];

    return (
        <AdminLayout title="대시보드" subtitle="FitMe 서비스 현황을 한눈에 확인하세요">
            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <StatCard
                    title="전체 회원"
                    value={stats.totalMembers.toLocaleString()}
                    change={`이번 주 +${stats.weeklyNewMembers}명`}
                    changeType="positive"
                    icon={Users}
                    iconColor="bg-blue-500/10 text-blue-500"
                />
                <StatCard
                    title="총 수익"
                    value={`${stats.totalRevenue?.toLocaleString() || 0}원`}
                    icon={TrendingUp}
                    iconColor="bg-emerald-500/10 text-emerald-500"
                />
                <StatCard
                    title="등록 기업"
                    value={stats.totalCompanies.toLocaleString()}
                    icon={Building2}
                    iconColor="bg-purple-500/10 text-purple-500"
                />
                <StatCard
                    title="대기중 신고"
                    value={stats.pendingReports.toLocaleString()}
                    change={stats.pendingReports > 0 ? "처리 필요" : ""}
                    changeType={stats.pendingReports > 0 ? "negative" : "neutral"}
                    icon={Flag}
                    iconColor="bg-red-500/10 text-red-500"
                />
            </div>

            {/* Additional Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                <StatCard
                    title="오늘 지원"
                    value={stats.todayApplications.toLocaleString()}
                    icon={Briefcase}
                    iconColor="bg-orange-500/10 text-orange-500"
                />
                <StatCard
                    title="신규 회원 (주간)"
                    value={stats.weeklyNewMembers.toLocaleString()}
                    icon={Clock}
                    iconColor="bg-teal-500/10 text-teal-500"
                />
            </div>

            {/* Recent Reports */}
            <div className="space-y-4">
                <h2 className="text-lg font-semibold">최근 대기중 신고</h2>
                {loading ? (
                    <div className="text-center py-8 text-muted-foreground">로딩 중...</div>
                ) : (
                    <DataTable
                        columns={reportColumns}
                        data={recentReports}
                        actions={[
                            { label: "상세보기", onClick: handleViewReport },
                        ]}
                    />
                )}
            </div>

            {/* Report Detail Modal */}
            <ReportDetailModal
                report={selectedReport}
                open={modalOpen}
                onOpenChange={setModalOpen}
                onReportProcessed={() => {
                    void fetchData();
                }}
            />
        </AdminLayout>
    );
};

export default AdminDashboard;
