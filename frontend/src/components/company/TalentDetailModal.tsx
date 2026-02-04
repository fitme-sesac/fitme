import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
    MapPin,
    Briefcase,
    GraduationCap,
    DollarSign,
    Mail,
    Phone,
    Clock,
    Send,
    CheckCircle,
    Building2,
    Sparkles
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/contexts/AuthContext";
import { createProposal, ProposalCreateRequest } from "@/api/proposal";
import { getTalentDetail } from "@/api/talents";
import { http } from "@/api/http";
import { toast } from "sonner";
import { ScrollArea } from "@/components/ui/scroll-area";

interface TalentDetailModalProps {
    talentId: number | null;
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function TalentDetailModal({ talentId, open, onOpenChange }: TalentDetailModalProps) {
    const navigate = useNavigate();
    const { isCompany } = useAuth();

    const [talent, setTalent] = useState<any>(null);
    const [loading, setLoading] = useState(false);
    const [hasSubscription, setHasSubscription] = useState<boolean | null>(null);

    // Proposal Dialog State (Nested)
    const [showProposalDialog, setShowProposalDialog] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    // 제안 폼 상태
    const [proposalForm, setProposalForm] = useState({
        title: "",
        message: "",
        offeredPosition: "",
        offeredSalary: "",
        expirationDays: 14,
    });

    // 인재 상세 조회
    useEffect(() => {
        if (open && talentId) {
            setLoading(true);
            getTalentDetail(talentId)
                .then((data) => setTalent(data ?? null))
                .catch(() => setTalent(null))
                .finally(() => setLoading(false));
        } else {
            setTalent(null);
        }
    }, [open, talentId]);

    // 구독 상태 확인 (기업 회원인 경우에만)
    useEffect(() => {
        if (open && isCompany) {
            http.get("/api/subscriptions/my/status")
                .then((res) => {
                    setHasSubscription(res.data?.hasActiveSubscription ?? false);
                })
                .catch(() => {
                    setHasSubscription(false);
                });
        }
    }, [open, isCompany]);

    const handleProposalSubmit = async () => {
        if (!talent || !proposalForm.title.trim()) {
            toast.error("제안 제목을 입력해주세요.");
            return;
        }

        setSubmitting(true);
        try {
            const request: ProposalCreateRequest = {
                candidateId: talent.id,
                title: proposalForm.title,
                message: proposalForm.message || undefined,
                offeredPosition: proposalForm.offeredPosition || undefined,
                offeredSalary: proposalForm.offeredSalary || undefined,
                expirationDays: proposalForm.expirationDays,
            };

            await createProposal(request);
            toast.success("포지션 제안을 보냈습니다!");
            setShowProposalDialog(false);
            onOpenChange(false); // Close the main modal too
            setProposalForm({
                title: "",
                message: "",
                offeredPosition: "",
                offeredSalary: "",
                expirationDays: 14,
            });
        } catch (error: any) {
            const msg = error?.response?.data?.message || error?.message || "제안 전송에 실패했습니다.";
            toast.error(msg);
        } finally {
            setSubmitting(false);
        }
    };

    if (!open) return null;

    const matchColor = talent?.matchScore >= 90 ? "text-blue-600 bg-blue-50 border-blue-200" :
        talent?.matchScore >= 80 ? "text-green-600 bg-green-50 border-green-200" :
            "text-yellow-600 bg-yellow-50 border-yellow-200";

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-4xl max-h-[90vh] p-0 overflow-hidden flex flex-col bg-slate-50">
                <DialogHeader className="p-6 pb-2 bg-white border-b shrink-0">
                    <DialogTitle>인재 상세 정보</DialogTitle>
                </DialogHeader>

                <ScrollArea className="flex-1 p-6">
                    {loading ? (
                        <div className="flex items-center justify-center py-20">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                        </div>
                    ) : !talent ? (
                        <div className="text-center py-20">
                            <p className="text-muted-foreground">인재 정보를 불러올 수 없습니다.</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                            {/* 왼쪽: 프로필 카드 */}
                            <div className="lg:col-span-1">
                                <Card>
                                    <CardContent className="pt-6 text-center">
                                        {/* 아바타 */}
                                        <div className="relative inline-block mb-4">
                                            <Avatar className="h-28 w-28 border-4 border-white shadow-lg">
                                                <AvatarImage src={talent.avatar} />
                                                <AvatarFallback className="text-3xl font-bold bg-gradient-to-br from-blue-100 to-indigo-100 text-blue-600">
                                                    {talent.name.charAt(0)}
                                                </AvatarFallback>
                                            </Avatar>
                                            {talent.isNew && (
                                                <Badge className="absolute -top-1 -right-1 bg-orange-500 text-white">
                                                    NEW
                                                </Badge>
                                            )}
                                        </div>

                                        <h1 className="text-xl font-bold mb-1">{talent.name}</h1>
                                        <p className="text-muted-foreground mb-3">{talent.title}</p>

                                        {/* 매칭 점수 */}
                                        <div className={`inline-flex items-center gap-1 px-3 py-1 rounded-full border text-sm font-medium mb-4 ${matchColor}`}>
                                            <CheckCircle className="w-4 h-4" />
                                            {talent.matchScore}% 매칭
                                        </div>

                                        {/* 기본 정보 */}
                                        <div className="space-y-2 text-sm text-left border-t pt-4 mt-4">
                                            <div className="flex items-center gap-2">
                                                <Briefcase className="w-4 h-4 text-muted-foreground" />
                                                <span>경력 {talent.experience}</span>
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <MapPin className="w-4 h-4 text-muted-foreground" />
                                                <span>{talent.location}</span>
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <GraduationCap className="w-4 h-4 text-muted-foreground" />
                                                <span>{talent.education}</span>
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <DollarSign className="w-4 h-4 text-muted-foreground" />
                                                <span>{talent.salary}</span>
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <Clock className="w-4 h-4 text-muted-foreground" />
                                                <span>{talent.lastUpdated} 업데이트</span>
                                            </div>
                                        </div>

                                        {/* 제안하기 버튼 */}
                                        {isCompany && (
                                            <Button
                                                className="w-full mt-6 font-bold"
                                                style={{ background: 'linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)' }}
                                                disabled={hasSubscription === null}
                                                onClick={() => {
                                                    // 구독 상태 확인
                                                    if (hasSubscription === false) {
                                                        if (window.confirm("포지션 제안은 열람권이 필요합니다.\n구독 페이지로 이동하시겠습니까?")) {
                                                            navigate("/subscription");
                                                        }
                                                        return;
                                                    }
                                                    setShowProposalDialog(true);
                                                }}
                                            >
                                                <Send className="mr-2 h-4 w-4" />
                                                포지션 제안하기
                                            </Button>
                                        )}
                                    </CardContent>
                                </Card>
                            </div>

                            {/* 오른쪽: 상세 정보 */}
                            <div className="lg:col-span-2 space-y-6">
                                {/* AI 10줄 요약 */}
                                <Card>
                                    <CardHeader>
                                        <CardTitle className="flex items-center gap-2">
                                            <Sparkles className="h-5 w-5 text-purple-600 fill-purple-100" />
                                            AI 10줄 요약
                                        </CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <p className="text-muted-foreground leading-relaxed whitespace-pre-wrap">
                                            {talent.summary}
                                        </p>
                                    </CardContent>
                                </Card>

                                {/* 보유 스킬 */}
                                <Card>
                                    <CardHeader>
                                        <CardTitle className="flex items-center gap-2">
                                            <Sparkles className="h-5 w-5" />
                                            보유 스킬
                                        </CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <div className="flex flex-wrap gap-2">
                                            {(talent.skills ?? []).map((skill: string, i: number) => (
                                                <Badge key={i} variant="secondary" className="px-3 py-1">
                                                    {skill}
                                                </Badge>
                                            ))}
                                        </div>
                                    </CardContent>
                                </Card>

                                {/* 연락처 (기업 회원에게만 표시) */}
                                {isCompany && (
                                    <Card>
                                        <CardHeader>
                                            <CardTitle className="flex items-center gap-2">
                                                <Mail className="h-5 w-5" />
                                                연락처
                                            </CardTitle>
                                        </CardHeader>
                                        <CardContent className="space-y-3">
                                            <div className="flex items-center gap-3">
                                                <Mail className="h-4 w-4 text-muted-foreground" />
                                                <span>{talent.email}</span>
                                            </div>
                                            <div className="flex items-center gap-3">
                                                <Phone className="h-4 w-4 text-muted-foreground" />
                                                <span>{talent.phone}</span>
                                            </div>
                                            <p className="text-xs text-muted-foreground mt-2">
                                                * 연락처는 열람권 보유 시 전체 공개됩니다.
                                            </p>
                                        </CardContent>
                                    </Card>
                                )}
                            </div>
                        </div>
                    )}
                </ScrollArea>

                {/* Nested Proposal Dialog */}
                <Dialog open={showProposalDialog} onOpenChange={setShowProposalDialog}>
                    <DialogContent className="sm:max-w-[500px]">
                        <DialogHeader>
                            <DialogTitle className="flex items-center gap-2">
                                <Building2 className="h-5 w-5" />
                                {talent?.name}님에게 포지션 제안
                            </DialogTitle>
                            <DialogDescription>
                                인재에게 포지션 제안을 보내면 알림이 전송됩니다.
                            </DialogDescription>
                        </DialogHeader>

                        <div className="space-y-4 py-4">
                            <div className="space-y-2">
                                <Label htmlFor="modal-title">제안 제목 *</Label>
                                <Input
                                    id="modal-title"
                                    placeholder="예: [FitMe] 시니어 프론트엔드 개발자 포지션 제안"
                                    value={proposalForm.title}
                                    onChange={(e) => setProposalForm(prev => ({ ...prev, title: e.target.value }))}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="modal-position">제안 포지션</Label>
                                <Input
                                    id="modal-position"
                                    placeholder="예: Senior Frontend Developer"
                                    value={proposalForm.offeredPosition}
                                    onChange={(e) => setProposalForm(prev => ({ ...prev, offeredPosition: e.target.value }))}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="modal-salary">제안 연봉</Label>
                                <Input
                                    id="modal-salary"
                                    placeholder="예: 8,000만원 ~ 1억원"
                                    value={proposalForm.offeredSalary}
                                    onChange={(e) => setProposalForm(prev => ({ ...prev, offeredSalary: e.target.value }))}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="modal-message">제안 메시지</Label>
                                <Textarea
                                    id="modal-message"
                                    placeholder="인재에게 전달할 메시지를 작성해주세요..."
                                    rows={4}
                                    value={proposalForm.message}
                                    onChange={(e) => setProposalForm(prev => ({ ...prev, message: e.target.value }))}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="modal-expiration">제안 유효 기간</Label>
                                <Select
                                    value={String(proposalForm.expirationDays)}
                                    onValueChange={(val) => setProposalForm(prev => ({ ...prev, expirationDays: parseInt(val) }))}
                                >
                                    <SelectTrigger>
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="7">7일</SelectItem>
                                        <SelectItem value="14">14일</SelectItem>
                                        <SelectItem value="30">30일</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        <DialogFooter>
                            <Button variant="outline" onClick={() => setShowProposalDialog(false)}>
                                취소
                            </Button>
                            <Button
                                onClick={handleProposalSubmit}
                                disabled={submitting || !proposalForm.title.trim()}
                                style={{ background: 'linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)' }}
                            >
                                {submitting ? "전송 중..." : "제안 보내기"}
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
            </DialogContent>
        </Dialog>
    );
}

