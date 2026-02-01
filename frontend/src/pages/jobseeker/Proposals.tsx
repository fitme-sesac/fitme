// frontend/src/pages/jobseeker/Proposals.tsx
import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import {
    Building2,
    Calendar,
    Clock,
    Check,
    X,
    Mail,
    Briefcase,
    DollarSign,
    ChevronRight,
    Bell,
    Inbox,
    Filter
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuth } from "@/contexts/AuthContext";
import {
    getReceivedProposals,
    getProposal,
    respondToProposal,
    ProposalResponse,
    PageResponse
} from "@/api/proposal";
import { toast } from "sonner";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs";

const statusConfig: Record<string, { label: string; color: string; icon: any }> = {
    PENDING: { label: "대기중", color: "bg-yellow-100 text-yellow-800 border-yellow-200", icon: Clock },
    VIEWED: { label: "확인됨", color: "bg-blue-100 text-blue-800 border-blue-200", icon: Mail },
    ACCEPTED: { label: "수락됨", color: "bg-green-100 text-green-800 border-green-200", icon: Check },
    REJECTED: { label: "거절됨", color: "bg-red-100 text-red-800 border-red-200", icon: X },
    EXPIRED: { label: "만료됨", color: "bg-gray-100 text-gray-800 border-gray-200", icon: Clock },
    CANCELED: { label: "취소됨", color: "bg-gray-100 text-gray-600 border-gray-200", icon: X },
};

export default function Proposals() {
    const { user } = useAuth();
    const [proposals, setProposals] = useState<ProposalResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [activeTab, setActiveTab] = useState("all");

    // 상세 모달
    const [selectedProposal, setSelectedProposal] = useState<ProposalResponse | null>(null);
    const [showDetailModal, setShowDetailModal] = useState(false);

    // 응답 모달
    const [showRespondModal, setShowRespondModal] = useState(false);
    const [respondType, setRespondType] = useState<"accept" | "reject">("accept");
    const [responseMessage, setResponseMessage] = useState("");
    const [responding, setResponding] = useState(false);

    useEffect(() => {
        loadProposals();
    }, [page]);

    const loadProposals = async () => {
        try {
            setLoading(true);
            const data = await getReceivedProposals(page, 10);
            setProposals(data.content);
            setTotalPages(data.totalPages);
        } catch (error: any) {
            toast.error("제안 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const handleViewDetail = async (proposal: ProposalResponse) => {
        try {
            // API 호출로 VIEWED 처리
            const updated = await getProposal(proposal.proposalId);
            setSelectedProposal(updated);
            setShowDetailModal(true);

            // 목록 갱신 (VIEWED 상태 반영)
            setProposals(prev => prev.map(p =>
                p.proposalId === updated.proposalId ? updated : p
            ));
        } catch (error) {
            toast.error("제안 정보를 불러오는데 실패했습니다.");
        }
    };

    const openRespondModal = (type: "accept" | "reject") => {
        setRespondType(type);
        setResponseMessage("");
        setShowRespondModal(true);
        setShowDetailModal(false);
    };

    const handleRespond = async () => {
        if (!selectedProposal) return;

        setResponding(true);
        try {
            const updated = await respondToProposal(selectedProposal.proposalId, {
                accept: respondType === "accept",
                message: responseMessage || undefined,
            });

            toast.success(respondType === "accept" ? "제안을 수락했습니다!" : "제안을 거절했습니다.");
            setShowRespondModal(false);

            // 목록 갱신
            setProposals(prev => prev.map(p =>
                p.proposalId === updated.proposalId ? updated : p
            ));
        } catch (error: any) {
            const msg = error?.response?.data?.message || "응답 처리에 실패했습니다.";
            toast.error(msg);
        } finally {
            setResponding(false);
        }
    };

    const filteredProposals = proposals.filter(p => {
        if (activeTab === "all") return true;
        if (activeTab === "pending") return p.status === "PENDING" || p.status === "VIEWED";
        if (activeTab === "responded") return p.status === "ACCEPTED" || p.status === "REJECTED";
        return true;
    });

    const formatDate = (dateStr?: string) => {
        if (!dateStr) return "-";
        return new Date(dateStr).toLocaleDateString("ko-KR", {
            year: "numeric",
            month: "long",
            day: "numeric"
        });
    };

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64">
                <Header />

                <main className="container max-w-4xl mx-auto py-6 px-4 md:px-8">
                    {/* 헤더 */}
                    <div className="mb-8">
                        <div className="flex items-center gap-3 mb-2">
                            <div className="p-2 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600">
                                <Bell className="h-6 w-6 text-white" />
                            </div>
                            <h1 className="text-2xl font-bold">받은 포지션 제안</h1>
                        </div>
                        <p className="text-muted-foreground">
                            기업에서 보낸 포지션 제안을 확인하고 응답하세요.
                        </p>
                    </div>

                    {/* 탭 필터 */}
                    <Tabs value={activeTab} onValueChange={setActiveTab} className="mb-6">
                        <TabsList className="grid w-full grid-cols-3">
                            <TabsTrigger value="all">전체</TabsTrigger>
                            <TabsTrigger value="pending">대기중</TabsTrigger>
                            <TabsTrigger value="responded">응답완료</TabsTrigger>
                        </TabsList>
                    </Tabs>

                    {/* 제안 목록 */}
                    {loading ? (
                        <div className="flex items-center justify-center py-20">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                        </div>
                    ) : filteredProposals.length === 0 ? (
                        <Card className="text-center py-16">
                            <CardContent>
                                <Inbox className="h-16 w-16 text-muted-foreground/30 mx-auto mb-4" />
                                <h3 className="text-lg font-medium mb-2">받은 제안이 없습니다</h3>
                                <p className="text-muted-foreground text-sm mb-4">
                                    프로필을 완성하고 이력서를 등록하면<br />
                                    기업에서 제안이 올 수 있어요!
                                </p>
                                <Button asChild>
                                    <Link to="/resume">이력서 관리하기</Link>
                                </Button>
                            </CardContent>
                        </Card>
                    ) : (
                        <div className="space-y-4">
                            {filteredProposals.map((proposal) => {
                                const status = statusConfig[proposal.status] || statusConfig.PENDING;
                                const StatusIcon = status.icon;

                                return (
                                    <Card
                                        key={proposal.proposalId}
                                        className="hover:shadow-md transition-shadow cursor-pointer"
                                        onClick={() => handleViewDetail(proposal)}
                                    >
                                        <CardContent className="p-5">
                                            <div className="flex items-start justify-between gap-4">
                                                <div className="flex items-start gap-4 flex-1">
                                                    {/* 기업 로고 */}
                                                    <Avatar className="h-14 w-14 rounded-xl border">
                                                        <AvatarImage src={proposal.employerLogo} />
                                                        <AvatarFallback className="rounded-xl bg-gradient-to-br from-blue-50 to-indigo-100 text-blue-600 font-bold">
                                                            {proposal.employerName?.charAt(0) || "?"}
                                                        </AvatarFallback>
                                                    </Avatar>

                                                    {/* 제안 정보 */}
                                                    <div className="flex-1 min-w-0">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <Badge className={`${status.color} border`}>
                                                                <StatusIcon className="h-3 w-3 mr-1" />
                                                                {status.label}
                                                            </Badge>
                                                            {proposal.status === "PENDING" && (
                                                                <Badge variant="destructive" className="animate-pulse">
                                                                    NEW
                                                                </Badge>
                                                            )}
                                                        </div>

                                                        <h3 className="font-bold text-lg mb-1 truncate">
                                                            {proposal.title}
                                                        </h3>

                                                        <p className="text-sm text-muted-foreground flex items-center gap-1 mb-2">
                                                            <Building2 className="h-3.5 w-3.5" />
                                                            {proposal.employerName}
                                                        </p>

                                                        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                                                            {proposal.offeredPosition && (
                                                                <span className="flex items-center gap-1">
                                                                    <Briefcase className="h-3 w-3" />
                                                                    {proposal.offeredPosition}
                                                                </span>
                                                            )}
                                                            {proposal.offeredSalary && (
                                                                <span className="flex items-center gap-1">
                                                                    <DollarSign className="h-3 w-3" />
                                                                    {proposal.offeredSalary}
                                                                </span>
                                                            )}
                                                            <span className="flex items-center gap-1">
                                                                <Calendar className="h-3 w-3" />
                                                                {formatDate(proposal.createdAt)}
                                                            </span>
                                                        </div>
                                                    </div>
                                                </div>

                                                <ChevronRight className="h-5 w-5 text-muted-foreground shrink-0" />
                                            </div>
                                        </CardContent>
                                    </Card>
                                );
                            })}

                            {/* 페이지네이션 */}
                            {totalPages > 1 && (
                                <div className="flex justify-center gap-2 mt-6">
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        disabled={page === 0}
                                        onClick={() => setPage(p => p - 1)}
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
                                        onClick={() => setPage(p => p + 1)}
                                    >
                                        다음
                                    </Button>
                                </div>
                            )}
                        </div>
                    )}
                </main>

                <Footer />
            </div>

            {/* 상세 모달 */}
            <Dialog open={showDetailModal} onOpenChange={setShowDetailModal}>
                <DialogContent className="sm:max-w-[550px]">
                    <DialogHeader>
                        <DialogTitle>포지션 제안 상세</DialogTitle>
                    </DialogHeader>

                    {selectedProposal && (
                        <div className="space-y-4 py-2">
                            {/* 기업 정보 */}
                            <div className="flex items-center gap-3 p-4 bg-muted/30 rounded-lg">
                                <Avatar className="h-12 w-12 rounded-xl border">
                                    <AvatarImage src={selectedProposal.employerLogo} />
                                    <AvatarFallback className="rounded-xl">
                                        {selectedProposal.employerName?.charAt(0)}
                                    </AvatarFallback>
                                </Avatar>
                                <div>
                                    <p className="font-bold">{selectedProposal.employerName}</p>
                                    <p className="text-sm text-muted-foreground">
                                        {formatDate(selectedProposal.createdAt)} 제안
                                    </p>
                                </div>
                            </div>

                            {/* 제안 내용 */}
                            <div className="space-y-3">
                                <h3 className="font-bold text-lg">{selectedProposal.title}</h3>

                                {selectedProposal.offeredPosition && (
                                    <div className="flex items-center gap-2 text-sm">
                                        <Briefcase className="h-4 w-4 text-muted-foreground" />
                                        <span className="font-medium">포지션:</span>
                                        <span>{selectedProposal.offeredPosition}</span>
                                    </div>
                                )}

                                {selectedProposal.offeredSalary && (
                                    <div className="flex items-center gap-2 text-sm">
                                        <DollarSign className="h-4 w-4 text-muted-foreground" />
                                        <span className="font-medium">제안 연봉:</span>
                                        <span>{selectedProposal.offeredSalary}</span>
                                    </div>
                                )}

                                {selectedProposal.expiresAt && (
                                    <div className="flex items-center gap-2 text-sm">
                                        <Clock className="h-4 w-4 text-muted-foreground" />
                                        <span className="font-medium">유효 기한:</span>
                                        <span>{formatDate(selectedProposal.expiresAt)}</span>
                                    </div>
                                )}

                                {selectedProposal.message && (
                                    <div className="mt-4 p-4 bg-muted/30 rounded-lg">
                                        <p className="text-sm font-medium mb-2">제안 메시지</p>
                                        <p className="text-sm text-muted-foreground whitespace-pre-wrap">
                                            {selectedProposal.message}
                                        </p>
                                    </div>
                                )}
                            </div>

                            {/* 응답 내역 (응답한 경우) */}
                            {selectedProposal.respondedAt && (
                                <div className="p-4 border rounded-lg">
                                    <p className="text-sm font-medium mb-2">
                                        나의 응답 ({selectedProposal.status === "ACCEPTED" ? "수락" : "거절"})
                                    </p>
                                    <p className="text-xs text-muted-foreground mb-1">
                                        {formatDate(selectedProposal.respondedAt)}
                                    </p>
                                    {selectedProposal.responseMessage && (
                                        <p className="text-sm text-muted-foreground">
                                            {selectedProposal.responseMessage}
                                        </p>
                                    )}
                                </div>
                            )}
                        </div>
                    )}

                    <DialogFooter>
                        {selectedProposal &&
                            (selectedProposal.status === "PENDING" || selectedProposal.status === "VIEWED") && (
                                <>
                                    <Button variant="outline" onClick={() => openRespondModal("reject")}>
                                        <X className="h-4 w-4 mr-1" />
                                        거절하기
                                    </Button>
                                    <Button
                                        onClick={() => openRespondModal("accept")}
                                        style={{ background: 'linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)' }}
                                    >
                                        <Check className="h-4 w-4 mr-1" />
                                        수락하기
                                    </Button>
                                </>
                            )}
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* 응답 모달 */}
            <Dialog open={showRespondModal} onOpenChange={setShowRespondModal}>
                <DialogContent className="sm:max-w-[400px]">
                    <DialogHeader>
                        <DialogTitle>
                            {respondType === "accept" ? "제안 수락하기" : "제안 거절하기"}
                        </DialogTitle>
                        <DialogDescription>
                            {respondType === "accept"
                                ? "수락하면 기업 담당자에게 알림이 전송됩니다."
                                : "거절 시 기업에게 간단한 메시지를 남길 수 있습니다."}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="py-4">
                        <Label htmlFor="response-message">
                            {respondType === "accept" ? "감사 메시지 (선택)" : "거절 사유 (선택)"}
                        </Label>
                        <Textarea
                            id="response-message"
                            placeholder={respondType === "accept"
                                ? "좋은 제안 감사합니다. 연락 기다리겠습니다."
                                : "현재 다른 기회를 검토 중입니다."}
                            value={responseMessage}
                            onChange={(e) => setResponseMessage(e.target.value)}
                            rows={3}
                            className="mt-2"
                        />
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setShowRespondModal(false)}>
                            취소
                        </Button>
                        <Button
                            onClick={handleRespond}
                            disabled={responding}
                            variant={respondType === "accept" ? "default" : "destructive"}
                        >
                            {responding ? "처리 중..." : (respondType === "accept" ? "수락" : "거절")}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
