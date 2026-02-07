import { useState, useEffect } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { StatusBadge } from "./StatusBadge";
import { Calendar, User, Flag, FileText, Target } from "lucide-react";
import { processReport } from "@/api/admin";
import { toast } from "sonner";

interface Report {
    id: number;
    reportId: number;
    reporterMemberId: number;
    targetType: string;
    targetId: number;
    targetMemberId?: number;
    targetJobId?: number;
    reasonCode: string;
    reasonDetail: string;
    status: string;
    createdAt: string;
    processedAt?: string;
    adminMemberId?: number;
}

interface ReportDetailModalProps {
    report: Report | null;
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onReportProcessed?: () => void;
}

const ReportDetailModal = ({ report, open, onOpenChange, onReportProcessed }: ReportDetailModalProps) => {
    const [processing, setProcessing] = useState(false);

    if (!report) return null;

    const handleProcess = async (action: "APPROVE" | "REJECT") => {
        console.log("=== 신고 처리 시작 ===");
        console.log("버튼 클릭됨:", action);
        console.log("처리할 신고 정보:", {
            reportId: report.reportId,
            id: report.id,
            status: report.status,
            targetType: report.targetType
        });
        
        if (!report.reportId) {
            console.error("신고 ID가 없습니다:", report);
            toast.error("신고 ID가 없습니다.");
            return;
        }
        
        setProcessing(true);
        try {
            const requestData: any = {
                decision: action === "APPROVE" ? "ACCEPT" : "REJECT",
                reason: action === "APPROVE" ? "신고 승인 처리" : "신고 거절",
            };

            // 승인 시에만 violationType 추가
            if (action === "APPROVE") {
                requestData.violationType = "MINOR_ETC";
            }

            console.log("API 요청 데이터:", {
                reportId: report.reportId,
                requestData
            });

            const result = await processReport(report.reportId, requestData);
            
            console.log("신고 처리 성공:", result);
            toast.success(`신고가 ${action === "APPROVE" ? "승인" : "거절"}되었습니다.`);
            onReportProcessed?.();
            onOpenChange(false);
        } catch (error: any) {
            console.error("=== 신고 처리 실패 ===");
            console.error("에러 객체:", error);
            
            // 에러 상세 정보 출력
            if (error.response) {
                console.error("HTTP 상태:", error.response.status);
                console.error("응답 헤더:", error.response.headers);
                console.error("응답 데이터:", error.response.data);
                console.error("응답 데이터 JSON:", JSON.stringify(error.response.data, null, 2));
                
                // 구체적인 에러 메시지 표시
                const errorMessage = error.response.data?.message || 
                                   error.response.data?.error || 
                                   error.response.data?.detail ||
                                   `HTTP ${error.response.status} 에러`;
                toast.error(`처리에 실패했습니다: ${errorMessage}`);
            } else if (error.request) {
                console.error("요청 실패 (응답 없음):", error.request);
                toast.error("서버에 연결할 수 없습니다.");
            } else {
                console.error("에러 메시지:", error.message);
                toast.error(`처리에 실패했습니다: ${error.message}`);
            }
        } finally {
            setProcessing(false);
        }
    };

    const getTargetTypeLabel = (type: string) => {
        switch (type) {
            case "MEMBER": return "회원";
            case "JOB_POSTING": return "채용공고";
            case "COMMUNITY_POST": return "커뮤니티 게시글";
            default: return type;
        }
    };

    const getReasonCodeLabel = (code: string) => {
        switch (code) {
            case "SPAM": return "스팸/광고";
            case "INAPPROPRIATE": return "부적절한 내용";
            case "HARASSMENT": return "괴롭힘/욕설";
            case "COPYRIGHT": return "저작권 침해";
            case "FRAUD": return "사기/허위정보";
            case "OTHER": return "기타";
            default: return code;
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <Flag className="w-5 h-5" />
                        신고 상세 정보 (ID: {report.reportId})
                    </DialogTitle>
                </DialogHeader>

                <div className="space-y-6">
                    {/* 기본 정보 */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                                <User className="w-4 h-4" />
                                신고자 ID
                            </div>
                            <div className="text-sm">{report.reporterMemberId}</div>
                        </div>
                        <div className="space-y-2">
                            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                                <Calendar className="w-4 h-4" />
                                신고일
                            </div>
                            <div className="text-sm">
                                {report.createdAt ? new Date(report.createdAt).toLocaleString() : "-"}
                            </div>
                        </div>
                    </div>

                    <Separator />

                    {/* 신고 대상 정보 */}
                    <div className="space-y-4">
                        <h3 className="font-medium flex items-center gap-2">
                            <Target className="w-4 h-4" />
                            신고 대상
                        </h3>
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <div className="text-sm font-medium text-muted-foreground">유형</div>
                                <Badge variant="outline">{getTargetTypeLabel(report.targetType)}</Badge>
                            </div>
                            <div className="space-y-2">
                                <div className="text-sm font-medium text-muted-foreground">대상 ID</div>
                                <div className="text-sm">
                                    {report.targetMemberId || report.targetJobId || report.targetId}
                                </div>
                            </div>
                        </div>
                    </div>

                    <Separator />

                    {/* 신고 사유 */}
                    <div className="space-y-4">
                        <h3 className="font-medium flex items-center gap-2">
                            <FileText className="w-4 h-4" />
                            신고 사유
                        </h3>
                        <div className="space-y-3">
                            <div>
                                <div className="text-sm font-medium text-muted-foreground mb-1">사유 코드</div>
                                <Badge>{getReasonCodeLabel(report.reasonCode)}</Badge>
                            </div>
                            {report.reasonDetail && (
                                <div>
                                    <div className="text-sm font-medium text-muted-foreground mb-1">상세 내용</div>
                                    <div className="text-sm bg-muted/50 p-3 rounded-lg">
                                        {report.reasonDetail}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>

                    <Separator />

                    {/* 처리 상태 */}
                    <div className="space-y-4">
                        <h3 className="font-medium">처리 상태</h3>
                        <div className="flex items-center gap-4">
                            <StatusBadge status={report.status?.toLowerCase()} />
                            {report.processedAt && (
                                <div className="text-sm text-muted-foreground">
                                    처리일: {new Date(report.processedAt).toLocaleString()}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* 액션 버튼 */}
                    {report.status === "OPEN" && (
                        <>
                            <Separator />
                            <div className="flex gap-3 justify-end">
                                <Button
                                    variant="outline"
                                    onClick={() => handleProcess("REJECT")}
                                    disabled={processing}
                                >
                                    거절
                                </Button>
                                <Button
                                    onClick={() => handleProcess("APPROVE")}
                                    disabled={processing}
                                >
                                    승인 (조치)
                                </Button>
                            </div>
                        </>
                    )}
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default ReportDetailModal;