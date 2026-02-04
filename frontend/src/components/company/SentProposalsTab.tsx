import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
    Briefcase,
    Calendar,
    Clock,
    ChevronRight,
    Send,
    Loader2,
    UserCircle,
} from "lucide-react";
import { getSentProposals, ProposalResponse, cancelProposal } from "@/api/proposal";
import { toast } from "sonner";

const statusConfig: Record<string, { label: string; color: string }> = {
    PENDING: { label: "대기중", color: "bg-yellow-100 text-yellow-800 border-yellow-200" },
    VIEWED: { label: "확인됨", color: "bg-blue-100 text-blue-800 border-blue-200" },
    ACCEPTED: { label: "수락됨", color: "bg-green-100 text-green-800 border-green-200" },
    REJECTED: { label: "거절됨", color: "bg-red-100 text-red-800 border-red-200" },
    EXPIRED: { label: "만료됨", color: "bg-gray-100 text-gray-800 border-gray-200" },
    CANCELED: { label: "취소됨", color: "bg-gray-100 text-gray-600 border-gray-200" },
};

export function SentProposalsTab() {
    const [proposals, setProposals] = useState<ProposalResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [cancelingId, setCancelingId] = useState<number | null>(null);

    useEffect(() => {
        loadProposals();
    }, [page]);

    const loadProposals = async () => {
        try {
            setLoading(true);
            const data = await getSentProposals(page, 10);
            setProposals(data.content || []);
            setTotalPages(data.totalPages ?? 0);
        } catch {
            setProposals([]);
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async (proposalId: number) => {
        setCancelingId(proposalId);
        try {
            await cancelProposal(proposalId);
            toast.success("제안을 취소했습니다.");
            loadProposals();
        } catch (e: any) {
            toast.error(e?.response?.data?.message ?? "취소에 실패했습니다.");
        } finally {
            setCancelingId(null);
        }
    };

    const formatDate = (dateStr?: string) => {
        if (!dateStr) return "-";
        return new Date(dateStr).toLocaleDateString("ko-KR", {
            year: "numeric",
            month: "long",
            day: "numeric",
        });
    };

    if (loading && proposals.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-primary mb-4" />
                <p className="text-muted-foreground">제안 목록을 불러오는 중...</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Send className="h-5 w-5 text-primary" />
                        인재풀에서 제안한 인원
                    </CardTitle>
                    <p className="text-sm text-muted-foreground">
                        AI 인재 추천·인재풀에서 보낸 포지션 제안 목록입니다. 수락/거절 상태를 확인하고 인재 상세로 이동할 수 있습니다.
                    </p>
                </CardHeader>
                <CardContent>
                    {proposals.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-12 text-center">
                            <UserCircle className="h-14 w-14 text-muted-foreground/50 mb-4" />
                            <h3 className="font-medium text-foreground mb-1">보낸 제안이 없습니다</h3>
                            <p className="text-sm text-muted-foreground mb-4">
                                AI 인재 추천 또는 인재풀에서 인재에게 제안을 보내보세요.
                            </p>
                            <Button asChild variant="outline">
                                <Link to="/company/dashboard?tab=ai-matching">AI 인재 추천 보기</Link>
                            </Button>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {proposals.map((p) => {
                                const status = statusConfig[p.status] ?? statusConfig.PENDING;
                                return (
                                    <Card key={p.proposalId} className="overflow-hidden">
                                        <CardContent className="p-0">
                                            <div className="flex flex-col sm:flex-row sm:items-center gap-4 p-4">
                                                <Link
                                                    to={p.candidateId ? `/talents/${p.candidateId}` : "#"}
                                                    className="flex flex-1 items-center gap-4 min-w-0"
                                                >
                                                    <Avatar className="h-12 w-12 rounded-xl shrink-0">
                                                        <AvatarImage src={undefined} />
                                                        <AvatarFallback className="rounded-xl bg-primary/10 text-primary">
                                                            {p.candidateName?.charAt(0) ?? "?"}
                                                        </AvatarFallback>
                                                    </Avatar>
                                                    <div className="flex-1 min-w-0">
                                                        <div className="flex flex-wrap items-center gap-2 mb-1">
                                                            <span className="font-semibold text-foreground truncate">
                                                                {p.candidateName ?? "이름 없음"}
                                                            </span>
                                                            <Badge variant="outline" className={status.color}>
                                                                {status.label}
                                                            </Badge>
                                                        </div>
                                                        <p className="text-sm font-medium text-foreground truncate">
                                                            {p.title}
                                                        </p>
                                                        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground mt-1">
                                                            {p.offeredPosition && (
                                                                <span className="flex items-center gap-1">
                                                                    <Briefcase className="h-3 w-3" />
                                                                    {p.offeredPosition}
                                                                </span>
                                                            )}
                                                            <span className="flex items-center gap-1">
                                                                <Calendar className="h-3 w-3" />
                                                                제안일: {formatDate(p.createdAt)}
                                                            </span>
                                                            {p.respondedAt && (
                                                                <span className="flex items-center gap-1">
                                                                    <Clock className="h-3 w-3" />
                                                                    응답: {formatDate(p.respondedAt)}
                                                                </span>
                                                            )}
                                                        </div>
                                                    </div>
                                                    {p.candidateId && (
                                                        <ChevronRight className="h-5 w-5 text-muted-foreground shrink-0 hidden sm:block" />
                                                    )}
                                                </Link>
                                                <div className="flex items-center gap-2 shrink-0">
                                                    {p.candidateId && (
                                                        <Button asChild variant="outline" size="sm">
                                                            <Link to={`/talents/${p.candidateId}`}>
                                                                인재 상세
                                                            </Link>
                                                        </Button>
                                                    )}
                                                    {(p.status === "PENDING" || p.status === "VIEWED") && (
                                                        <Button
                                                            variant="ghost"
                                                            size="sm"
                                                            className="text-destructive hover:text-destructive"
                                                            onClick={() => handleCancel(p.proposalId)}
                                                            disabled={cancelingId === p.proposalId}
                                                        >
                                                            {cancelingId === p.proposalId ? (
                                                                <Loader2 className="h-4 w-4 animate-spin" />
                                                            ) : (
                                                                "제안 취소"
                                                            )}
                                                        </Button>
                                                    )}
                                                </div>
                                            </div>
                                        </CardContent>
                                    </Card>
                                );
                            })}
                            {totalPages > 1 && (
                                <div className="flex justify-center gap-2 pt-4">
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        disabled={page === 0}
                                        onClick={() => setPage((prev) => prev - 1)}
                                    >
                                        이전
                                    </Button>
                                    <span className="flex items-center px-4 text-sm text-muted-foreground">
                                        {page + 1} / {totalPages}
                                    </span>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        disabled={page >= totalPages - 1}
                                        onClick={() => setPage((prev) => prev + 1)}
                                    >
                                        다음
                                    </Button>
                                </div>
                            )}
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
