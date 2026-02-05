import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { AdminLayout } from "@/components/admin/AdminLayout";
import { StatusBadge } from "@/components/admin/StatusBadge";
import { Button } from "@/components/ui/button";
import { ArrowLeft, User, Mail, Phone, Calendar, Shield } from "lucide-react";
import { getMember } from "@/api/admin";
import { toast } from "sonner";

interface MemberProfile {
    memberId: number;
    name: string;
    email: string;
    phone: string;
    role: string;
    status: string;
    createdAt: string;
    userid?: string;
    gender?: string;
    birthday?: string;
}

const ROLE_LABEL: Record<string, string> = {
    CANDIDATE: "구직자",
    EMPLOYER: "기업회원",
    SERVICEADMIN: "서비스관리자",
    APPROVEADMIN: "승인관리자",
    MASTER: "마스터",
};

export default function AdminMemberProfilePage() {
    const { memberId } = useParams<{ memberId: string }>();
    const navigate = useNavigate();
    const [member, setMember] = useState<MemberProfile | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const id = memberId ? parseInt(memberId, 10) : NaN;
        if (!id || Number.isNaN(id)) {
            toast.error("잘못된 회원 정보입니다.");
            navigate("/admin/members");
            return;
        }
        getMember(id)
            .then((data) => setMember(data))
            .catch(() => {
                toast.error("회원 정보를 불러오는데 실패했습니다.");
                navigate("/admin/members");
            })
            .finally(() => setLoading(false));
    }, [memberId, navigate]);

    if (loading) {
        return (
            <AdminLayout title="회원 프로필" subtitle="로딩 중...">
                <div className="text-center py-12 text-muted-foreground">로딩 중...</div>
            </AdminLayout>
        );
    }

    if (!member) {
        return null;
    }

    return (
        <AdminLayout title="회원 프로필" subtitle={`${member.name} (ID: ${member.memberId})`}>
            <Button
                variant="ghost"
                className="mb-6"
                onClick={() => navigate("/admin/members")}
            >
                <ArrowLeft className="h-4 w-4 mr-2" />
                회원 목록으로
            </Button>

            <div className="bg-card rounded-xl border border-border overflow-hidden max-w-2xl">
                <div className="p-6 space-y-6">
                    <div className="flex items-center gap-4">
                        <div className="rounded-full bg-muted flex items-center justify-center h-16 w-16">
                            <User className="h-8 w-8 text-muted-foreground" />
                        </div>
                        <div>
                            <h2 className="text-xl font-semibold">{member.name}</h2>
                            <p className="text-sm text-muted-foreground">
                                {ROLE_LABEL[member.role] ?? member.role} · ID {member.memberId}
                            </p>
                            <StatusBadge status={member.status?.toLowerCase() || "active"} />
                        </div>
                    </div>

                    <dl className="grid gap-4">
                        {member.userid && (
                            <div className="flex items-center gap-3">
                                <Shield className="h-5 w-5 text-muted-foreground shrink-0" />
                                <div>
                                    <dt className="text-sm text-muted-foreground">아이디</dt>
                                    <dd className="font-medium">{member.userid}</dd>
                                </div>
                            </div>
                        )}
                        <div className="flex items-center gap-3">
                            <Mail className="h-5 w-5 text-muted-foreground shrink-0" />
                            <div>
                                <dt className="text-sm text-muted-foreground">이메일</dt>
                                <dd className="font-medium">{member.email || "-"}</dd>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <Phone className="h-5 w-5 text-muted-foreground shrink-0" />
                            <div>
                                <dt className="text-sm text-muted-foreground">연락처</dt>
                                <dd className="font-medium">{member.phone || "-"}</dd>
                            </div>
                        </div>
                        {member.gender && (
                            <div className="flex items-center gap-3">
                                <User className="h-5 w-5 text-muted-foreground shrink-0" />
                                <div>
                                    <dt className="text-sm text-muted-foreground">성별</dt>
                                    <dd className="font-medium">{member.gender}</dd>
                                </div>
                            </div>
                        )}
                        {member.birthday && (
                            <div className="flex items-center gap-3">
                                <Calendar className="h-5 w-5 text-muted-foreground shrink-0" />
                                <div>
                                    <dt className="text-sm text-muted-foreground">생년월일</dt>
                                    <dd className="font-medium">{member.birthday}</dd>
                                </div>
                            </div>
                        )}
                        <div className="flex items-center gap-3">
                            <Calendar className="h-5 w-5 text-muted-foreground shrink-0" />
                            <div>
                                <dt className="text-sm text-muted-foreground">가입일</dt>
                                <dd className="font-medium">
                                    {member.createdAt
                                        ? new Date(member.createdAt).toLocaleDateString("ko-KR")
                                        : "-"}
                                </dd>
                            </div>
                        </div>
                    </dl>
                </div>
            </div>
        </AdminLayout>
    );
}
