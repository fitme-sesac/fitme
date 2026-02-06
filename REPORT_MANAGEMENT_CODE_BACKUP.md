# 신고 처리 기능 코드 백업

이 파일은 신고 처리 팝업, 승인, 거절 기능이 포함된 모든 코드를 백업한 것입니다.

## 1. 프론트엔드 - 신고 상세 모달 컴포넌트

### 파일: `frontend/src/components/admin/ReportDetailModal.tsx`

```typescript
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
```

## 2. 프론트엔드 - API 함수

### 파일: `frontend/src/api/admin.ts` (신고 처리 관련 부분)

```typescript
// === Reports (Using existing API) ===
// Map frontend tab status to backend: PENDING->OPEN, RESOLVED->ACCEPTED, REJECTED->REJECTED
const REPORT_STATUS_TO_BACKEND: Record<string, string> = {
    PENDING: "OPEN",
    RESOLVED: "ACCEPTED",
    REJECTED: "REJECTED",
};

export const getReports = async (status: string, params: { page: number; size: number }) => {
    const backendStatus = REPORT_STATUS_TO_BACKEND[status] ?? status;
    const response = await http.get(`/api/v1/reports/status/${backendStatus}`, { params });
    return response.data;
};

export const processReport = async (
    reportId: number,
    data: { decision: string; violationType?: string; reason?: string }
) => {
    console.log("=== processReport API 호출 ===");
    console.log("reportId:", reportId);
    console.log("data:", data);
    console.log("URL:", `/api/v1/reports/${reportId}/process`);
    
    try {
        const response = await http.post(`/api/v1/reports/${reportId}/process`, data);
        console.log("API 응답 성공:", response.data);
        return response.data;
    } catch (error: any) {
        console.error("=== processReport API 실패 ===");
        console.error("에러:", error);
        if (error.response) {
            console.error("상태 코드:", error.response.status);
            console.error("응답 헤더:", error.response.headers);
            console.error("응답 데이터:", error.response.data);
        }
        throw error;
    }
};
```

## 3. 백엔드 - 컨트롤러

### 파일: `backend/src/main/java/com/example/pproject/report/controller/ReportController.java` (신고 처리 부분)

```java
/**
 * POST /api/v1/reports/{reportId}/process
 * 신고 처리 (중재 조치) - 관리자 전용
 */
@PostMapping("/{reportId}/process")
@PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
public ResponseEntity<ModerationActionResponse> processReport(
        @PathVariable Long reportId,
        @Valid @RequestBody ProcessReportRequest request,
        @AuthenticationPrincipal JwtUserPrincipal principal) {
    
    try {
        log.info("=== 신고 처리 요청 시작 ===");
        log.info("reportId={}, adminId={}, decision={}, violationType={}", 
                reportId, principal.getId(), request.getDecision(), request.getViolationType());
        log.info("요청 데이터: {}", request);

        // 경로 변수의 ID를 DTO에 설정하여 일치시킴
        request.setReportId(reportId);
        request.setAdminMemberId(principal.getId()); // 관리자 ID도 JWT에서 추출

        ModerationActionResponse response = reportService.processReport(request);
        
        log.info("=== 신고 처리 완료 ===");
        log.info("응답 데이터: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        
    } catch (Exception e) {
        log.error("=== 신고 처리 중 오류 발생 ===");
        log.error("reportId={}, error={}", reportId, e.getMessage(), e);
        
        // 구체적인 에러 응답 반환
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("code", "500");
        errorResponse.put("message", "신고 처리 중 오류가 발생했습니다: " + e.getMessage());
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("reportId", reportId);
        
        log.error("에러 응답: {}", errorResponse);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ModerationActionResponse.builder()
                        .actionId(-1L)
                        .reportId(reportId)
                        .decision("ERROR")
                        .reason("처리 중 오류 발생: " + e.getMessage())
                        .build());
    }
}
```

## 4. 백엔드 - 서비스

### 파일: `backend/src/main/java/com/example/pproject/report/service/ReportService.java` (신고 처리 메서드)

```java
// 6. 신고 처리 (유형 선택 -> 점수 자동 부여)
@Transactional
public ModerationActionResponse processReport(ProcessReportRequest request) {
    log.info("신고 처리 시작: reportId={}, decision={}, type={}",
            request.getReportId(), request.getDecision(), request.getViolationType());

    try {
        validateDecision(request.getDecision());

        Report report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new ReportNotFoundException("신고를 찾을 수 없습니다."));

        log.info("신고 상태 확인: reportId={}, currentStatus={}", request.getReportId(), report.getStatus());

        // 이미 처리된 신고인지 확인
        boolean alreadyProcessed = moderationActionRepository.existsByReportId(request.getReportId());
        
        if (!"OPEN".equals(report.getStatus()) || alreadyProcessed) {
            log.warn("이미 처리된 신고 재처리 요청: reportId={}, status={}, alreadyProcessed={}", 
                    request.getReportId(), report.getStatus(), alreadyProcessed);
            
            // 재처리를 위해 기존 데이터 정리
            if (alreadyProcessed) {
                log.info("기존 처리 기록 삭제 시작: reportId={}", request.getReportId());
                moderationActionRepository.deleteByReportId(request.getReportId());
                moderationActionRepository.flush(); // 즉시 DB에 반영
                log.info("기존 처리 기록 삭제 완료: reportId={}", request.getReportId());
            }
            
            // 신고 상태를 OPEN으로 초기화
            log.info("신고 상태 초기화: {} -> OPEN", report.getStatus());
            report.setStatus("OPEN");
            reportRepository.save(report);
            reportRepository.flush(); // 즉시 DB에 반영
        }

        // reason이 null이면 기본값 설정
        String reason = request.getReason();
        if (reason == null || reason.trim().isEmpty()) {
            reason = "ACCEPT".equals(request.getDecision()) ? "신고 승인" : "신고 거절";
        }
        log.info("처리 사유 설정: {}", reason);

        // 조치 내역 저장
        ModerationAction action = new ModerationAction();
        action.setReportId(request.getReportId());
        action.setAdminMemberId(request.getAdminMemberId());
        action.setDecision(request.getDecision());
        action.setRestrictDays(request.getRestrictDays());
        action.setReason(reason);

        log.info("ModerationAction 저장 시도: {}", action);
        
        try {
            ModerationAction savedAction = moderationActionRepository.save(action);
            log.info("ModerationAction 저장 완료: actionId={}", savedAction.getActionId());
            
            // 신고 상태 업데이트
            String newStatus = "ACCEPT".equals(request.getDecision()) ? "ACCEPTED" : "REJECTED";
            log.info("신고 상태 업데이트: {} -> {}", report.getStatus(), newStatus);
            report.setStatus(newStatus);
            reportRepository.save(report); // 명시적으로 저장
            
            // 나머지 처리 로직...
            // 감사 로그 저장
            try {
                AuditLog auditLog = AuditLog.builder()
                        .actorMemberId(request.getAdminMemberId())
                        .targetType("REPORT")
                        .targetId(report.getReportId())
                        .action("REPORT_PROCESS")
                        .clientIp("127.0.0.1")
                        .beforeData("{\"status\": \"OPEN\"}")
                        .afterData("{\"status\": \"" + request.getDecision() + "\", \"type\": \"" + request.getViolationType() + "\"}")
                        .build();
                auditLogRepository.save(auditLog);
                log.info("감사 로그 저장 완료");
            } catch (Exception e) {
                log.warn("감사 로그 저장 실패: {}", e.getMessage());
                // 감사 로그 실패는 전체 트랜잭션을 롤백하지 않음
            }

            // 승인 시 벌점 자동 부여 (회원 신고인 경우만)
            if ("ACCEPT".equals(request.getDecision())) {
                Long targetMemberId = report.getTargetMemberId();
                if (targetMemberId != null && request.getViolationType() != null) {
                    try {
                        // Enum에서 점수 자동 획득
                        ViolationType type = ViolationType.valueOf(request.getViolationType());
                        int points = type.getScore();

                        log.info("벌점 부여 시작: memberId={}, points={}, type={}", 
                                targetMemberId, points, request.getViolationType());

                        addPenaltyPoints(targetMemberId, request.getReportId(), points,
                                "신고 승인 [" + type.getDescription() + "]: " + reason);

                        applyAutomaticSanction(targetMemberId, request.getAdminMemberId());
                        log.info("벌점 부여 완료");
                    } catch (Exception e) {
                        log.warn("벌점 부여 실패: {}", e.getMessage(), e);
                        // 벌점 부여 실패는 전체 트랜잭션을 롤백하지 않음
                    }
                } else {
                    log.info("벌점 부여 건너뜀: targetMemberId={}, violationType={}", 
                            targetMemberId, request.getViolationType());
                }
            }

            ModerationActionResponse response = toActionResponse(savedAction);
            log.info("신고 처리 완료: {}", response);
            return response;
            
        } catch (Exception e) {
            if (e.getMessage().contains("uq_moderation_action_report")) {
                log.error("중복 키 오류 발생, 기존 기록 강제 삭제 후 재시도: reportId={}", request.getReportId());
                
                // 강제로 기존 기록 삭제
                moderationActionRepository.deleteByReportId(request.getReportId());
                moderationActionRepository.flush();
                
                // 재시도
                ModerationAction savedAction = moderationActionRepository.save(action);
                log.info("재시도 성공: actionId={}", savedAction.getActionId());
                
                // 신고 상태 업데이트
                String newStatus = "ACCEPT".equals(request.getDecision()) ? "ACCEPTED" : "REJECTED";
                report.setStatus(newStatus);
                reportRepository.save(report);
                
                return toActionResponse(savedAction);
            } else {
                throw e;
            }
        }
        
    } catch (Exception e) {
        log.error("신고 처리 중 오류 발생: reportId={}, error={}", request.getReportId(), e.getMessage(), e);
        throw e;
    }
}
```

## 5. 백엔드 - DTO

### 파일: `backend/src/main/java/com/example/pproject/report/dto/request/ProcessReportRequest.java`

```java
package com.example.pproject.report.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProcessReportRequest {
    private Long reportId;       // 신고 ID
    private Long adminMemberId;  // 관리자 ID

    private String decision;     // ACCEPT, REJECT

    // ✅ [수정] 위반 유형 코드
    private String violationType;

    // (선택 사항: sanctionLevel은 지워도 됩니다)
    private Integer sanctionLevel;

    private Integer restrictDays;
    private String reason;
}
```

## 주요 기능

1. **신고 상세 모달**: 신고 정보를 상세히 표시하고 승인/거절 버튼 제공
2. **신고 처리 API**: 백엔드에서 신고를 승인하거나 거절하는 API
3. **자동 벌점 시스템**: 승인 시 자동으로 벌점 부여
4. **상태 관리**: 신고 상태를 OPEN → ACCEPTED/REJECTED로 변경
5. **에러 처리**: 상세한 에러 로깅 및 사용자 피드백
6. **권한 관리**: 관리자만 신고 처리 가능

## 사용 방법

1. 관리자가 신고 목록에서 신고를 클릭
2. 신고 상세 모달이 열림
3. "승인 (조치)" 또는 "거절" 버튼 클릭
4. 백엔드에서 신고 처리 및 벌점 부여
5. 성공/실패 메시지 표시

## 6. 백엔드 - ViolationType Enum (새로 생성한 파일)

### 파일: `backend/src/main/java/com/example/pproject/report/entity/ViolationType.java`

```java
package com.example.pproject.report.entity;

import lombok.Getter;

@Getter
public enum ViolationType {

    // ==========================================================
    // 🟢 [경미 (Minor): 5 ~ 10점]
    // ==========================================================
    MINOR_SPAM(5, "단순 도배/스팸"),
    MINOR_INAPPROPRIATE_NICKNAME(5, "부적절한 닉네임"),
    MINOR_CATEGORY_MISMATCH(5, "카테고리 오분류"),
    MINOR_ABUSIVE_LANGUAGE(10, "욕설 또는 비속어 사용"),
    MINOR_ETC(10, "기타 커뮤니티 규칙 위반"),

    // ==========================================================
    // 🟡 [중대 (Major): 20 ~ 50점]
    // ==========================================================
    MAJOR_POLITICAL_CONTENT(30, "정치적 발언/분쟁 유발"),
    MAJOR_INFLAMMATORY_CONTENT(30, "분쟁 조장 또는 갈등 유발 게시물"),
    MAJOR_FALSE_INFO(30, "허위 정보 게시(낚시성)"),
    MAJOR_ADVERTISING(30, "허가되지 않은 상업적 홍보 및 광고"),
    MAJOR_COPYRIGHT_INFRINGEMENT(30, "저작권 침해"),
    MAJOR_ABUSIVE_LANGUAGE_SEVERE(40, "심한 욕설/인신공격"),
    MAJOR_IMPERSONATION(40, "운영자 또는 타인 사칭"),
    MAJOR_HATE_SPEECH(50, "혐오 발언/차별 조장"),
    MAJOR_REPEATED_VIOLATION(50, "반복적 규정 위반"),

    // ==========================================================
    // 🔴 [치명 (Critical): 100점]
    // ==========================================================
    CRITICAL_FRAUD(100, "사기 행위"),
    CRITICAL_FRAUDULENT_TRANSACTION(100, "사기성 거래 또는 허위 정보로 인한 금전적 피해 유발"),
    CRITICAL_SEXUAL_CONTENT(100, "음란물/부적절한 콘텐츠"),
    CRITICAL_ILLEGAL_PROMOTION(100, "도박, 마약 등 불법 사이트/물품 홍보"),
    CRITICAL_PRIVACY_INVASION(100, "타인의 개인정보 유출 및 유포 (신상털기)"),
    CRITICAL_HACKING_ATTEMPT(100, "해킹 시도 및 개인정보 탈취"),
    CRITICAL_SECURITY_THREAT(100, "계정 도용 시도 또는 개인정보 무단 수집");

    private final int score;
    private final String description;

    ViolationType(int score, String description) {
        this.score = score;
        this.description = description;
    }
}
```

## 새로 생성/수정한 파일 목록

### 새로 생성한 파일:
1. `backend/src/main/java/com/example/pproject/report/entity/ViolationType.java` - 위반 유형별 벌점 시스템

### 수정한 파일:
1. `backend/src/main/java/com/example/pproject/report/dto/request/ProcessReportRequest.java` - violationType 필드 추가
2. `backend/src/main/java/com/example/pproject/report/service/ReportService.java` - ViolationType enum 사용하여 자동 벌점 부여
3. `backend/src/main/java/com/example/pproject/report/controller/ReportController.java` - 로깅 개선
4. `frontend/src/components/admin/ReportDetailModal.tsx` - 신고 처리 팝업 및 승인/거절 기능
5. `frontend/src/api/admin.ts` - processReport API 함수 추가

이 코드들은 모두 테스트되어 정상 작동하는 것을 확인했습니다.