import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { StatCard } from "@/components/admin/StatCard";
import { DataTable } from "@/components/admin/DataTable";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Users, Briefcase, Building2, Flag, TrendingUp, Clock } from "lucide-react";
import { getAdminStats, getReports } from "@/api/admin";

interface DashboardStats {
    totalMembers: number;
    totalJobs: number;
    totalCompanies: number;
    totalRevenue: number;
    pendingReports: number;
    todayApplications: number;
    weeklyNewMembers: number;
}

interface Report {
    // 백엔드 응답은 reportId를 사용한다. DataTable 호환을 위해 id를 매핑한다.
    id?: number;
    reportId: number;
    reporterMemberId: number;
    targetType: string;
    status: string;
    createdAt: string;
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

    const toReportBadgeStatus = (raw: string) => {
        const s = (raw || "").toUpperCase();
        if (s === "OPEN") return "pending";
        if (s === "ACCEPTED") return "resolved";
        if (s === "REJECTED") return "rejected";
        return (raw || "").toLowerCase();
    };

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [statsData, reportsData] = await Promise.all([
                    getAdminStats(),
                    getReports("PENDING", { page: 0, size: 5 }),
                ]);
                setStats(statsData);
                const content = reportsData?.content || [];
                setRecentReports(content.map((r: any) => ({
                    ...r,
                    id: r.reportId ?? r.id,
                })));
            } catch (error) {
                console.error("대시보드 데이터 로드 실패:", error);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const reportColumns = [
        { key: "reportId", label: "ID" },
        { key: "targetType", label: "유형" },
        {
            key: "status",
            label: "상태",
            render: (item: Report) => {
                const badge = toReportBadgeStatus(item.status);
                const label = badge === "pending" ? "대기중" : (badge === "resolved" ? "처리완료" : undefined);
                return <StatusBadge status={badge} label={label} />;
            }
        },
        {
            key: "createdAt",
            label: "신고일",
            render: (item: Report) => {
                const d = item.createdAt ? new Date(item.createdAt) : null;
                return d ? d.toLocaleString() : "";
            }
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
                    icon={TrendingUp}
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
                            { label: "상세보기", onClick: (item) => console.log("View", item) },
                        ]}
                    />
                )}
            </div>
        </AdminLayout>
    );
};

export default AdminDashboard;
