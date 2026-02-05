import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Building2, Mail, Phone, MapPin, Globe, Calendar } from "lucide-react";
import { getEmployer } from "@/api/admin";
import { toast } from "sonner";

interface EmployerProfile {
    employerId: number;
    companyName: string;
    status: string;
    createdAt: string;
    logoUrl?: string;
    industry?: string;
    foundedYear?: number;
    employeeCount?: number;
    location?: string;
    description?: string;
    culture?: string;
    benefits?: string;
    techStack?: string;
    contactEmail?: string;
    contactPhone?: string;
    websiteUrl?: string;
}

export default function AdminCompanyProfilePage() {
    const { employerId } = useParams<{ employerId: string }>();
    const navigate = useNavigate();
    const [employer, setEmployer] = useState<EmployerProfile | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const id = employerId ? parseInt(employerId, 10) : NaN;
        if (!id || Number.isNaN(id)) {
            toast.error("잘못된 기업 정보입니다.");
            navigate("/admin/companies");
            return;
        }
        getEmployer(id)
            .then((data) => setEmployer(data))
            .catch(() => {
                toast.error("기업 정보를 불러오는데 실패했습니다.");
                navigate("/admin/companies");
            })
            .finally(() => setLoading(false));
    }, [employerId, navigate]);

    if (loading) {
        return (
            <AdminLayout title="기업 프로필" subtitle="로딩 중...">
                <div className="text-center py-12 text-muted-foreground">로딩 중...</div>
            </AdminLayout>
        );
    }

    if (!employer) {
        return null;
    }

    return (
        <AdminLayout title="기업 프로필" subtitle={`${employer.companyName} (ID: ${employer.employerId})`}>
            <Button
                variant="ghost"
                className="mb-6"
                onClick={() => navigate("/admin/companies")}
            >
                <ArrowLeft className="h-4 w-4 mr-2" />
                기업 목록으로
            </Button>

            <div className="bg-card rounded-xl border border-border overflow-hidden max-w-3xl">
                <div className="p-6 space-y-6">
                    <div className="flex items-center gap-4">
                        {employer.logoUrl ? (
                            <img
                                src={employer.logoUrl}
                                alt={employer.companyName}
                                className="h-16 w-16 rounded-lg object-cover border border-border"
                            />
                        ) : (
                            <div className="rounded-lg bg-muted flex items-center justify-center h-16 w-16">
                                <Building2 className="h-8 w-8 text-muted-foreground" />
                            </div>
                        )}
                        <div>
                            <h2 className="text-xl font-semibold">{employer.companyName}</h2>
                            <p className="text-sm text-muted-foreground">
                                ID {employer.employerId}
                                {employer.industry && ` · ${employer.industry}`}
                            </p>
                            <StatusBadge status={employer.status?.toLowerCase() || "active"} />
                        </div>
                    </div>

                    <dl className="grid gap-4">
                        {employer.contactEmail && (
                            <div className="flex items-center gap-3">
                                <Mail className="h-5 w-5 text-muted-foreground shrink-0" />
                                <div>
                                    <dt className="text-sm text-muted-foreground">이메일</dt>
                                    <dd className="font-medium">{employer.contactEmail}</dd>
                                </div>
                            </div>
                        )}
                        {employer.contactPhone && (
                            <div className="flex items-center gap-3">
                                <Phone className="h-5 w-5 text-muted-foreground shrink-0" />
                                <div>
                                    <dt className="text-sm text-muted-foreground">연락처</dt>
                                    <dd className="font-medium">{employer.contactPhone}</dd>
                                </div>
                            </div>
                        )}
                        {employer.location && (
                            <div className="flex items-center gap-3">
                                <MapPin className="h-5 w-5 text-muted-foreground shrink-0" />
                                <div>
                                    <dt className="text-sm text-muted-foreground">위치</dt>
                                    <dd className="font-medium">{employer.location}</dd>
                                </div>
                            </div>
                        )}
                        {(employer.foundedYear != null || employer.employeeCount != null) && (
                            <div className="flex items-center gap-3">
                                <Building2 className="h-5 w-5 text-muted-foreground shrink-0" />
                                <div>
                                    <dt className="text-sm text-muted-foreground">설립연도 / 직원 수</dt>
                                    <dd className="font-medium">
                                        {employer.foundedYear != null ? `${employer.foundedYear}년` : "-"}
                                        {employer.employeeCount != null ? ` · ${employer.employeeCount}명` : ""}
                                    </dd>
                                </div>
                            </div>
                        )}
                        {employer.websiteUrl && (
                            <div className="flex items-center gap-3">
                                <Globe className="h-5 w-5 text-muted-foreground shrink-0" />
                                <div>
                                    <dt className="text-sm text-muted-foreground">웹사이트</dt>
                                    <dd className="font-medium">
                                        <a
                                            href={employer.websiteUrl.startsWith("http") ? employer.websiteUrl : `https://${employer.websiteUrl}`}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="text-primary hover:underline"
                                        >
                                            {employer.websiteUrl}
                                        </a>
                                    </dd>
                                </div>
                            </div>
                        )}
                        <div className="flex items-center gap-3">
                            <Calendar className="h-5 w-5 text-muted-foreground shrink-0" />
                            <div>
                                <dt className="text-sm text-muted-foreground">등록일</dt>
                                <dd className="font-medium">
                                    {employer.createdAt
                                        ? new Date(employer.createdAt).toLocaleDateString("ko-KR")
                                        : "-"}
                                </dd>
                            </div>
                        </div>
                    </dl>

                    {employer.description && (
                        <div>
                            <h3 className="text-sm font-medium text-muted-foreground mb-2">소개</h3>
                            <p className="text-sm whitespace-pre-wrap">{employer.description}</p>
                        </div>
                    )}
                    {employer.culture && (
                        <div>
                            <h3 className="text-sm font-medium text-muted-foreground mb-2">문화</h3>
                            <p className="text-sm whitespace-pre-wrap">{employer.culture}</p>
                        </div>
                    )}
                    {employer.benefits && (
                        <div>
                            <h3 className="text-sm font-medium text-muted-foreground mb-2">복리후생</h3>
                            <p className="text-sm whitespace-pre-wrap">{employer.benefits}</p>
                        </div>
                    )}
                    {employer.techStack && (
                        <div>
                            <h3 className="text-sm font-medium text-muted-foreground mb-2">기술 스택</h3>
                            <p className="text-sm whitespace-pre-wrap">{employer.techStack}</p>
                        </div>
                    )}
                </div>
            </div>
        </AdminLayout>
    );
}
