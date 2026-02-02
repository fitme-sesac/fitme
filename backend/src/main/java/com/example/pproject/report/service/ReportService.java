package com.example.pproject.report.service;

import com.example.pproject.admin.entity.Member;
import com.example.pproject.admin.entity.MemberGradeHistory;
import com.example.pproject.admin.repository.AdminMemberRepository;
import com.example.pproject.admin.repository.MemberGradeHistoryRepository;
import com.example.pproject.audit.entity.AuditLog;
import com.example.pproject.audit.repository.AuditLogRepository;
import com.example.pproject.report.dto.request.CreateReportRequest;
import com.example.pproject.report.dto.request.ProcessReportRequest;
import com.example.pproject.report.dto.response.*;
import com.example.pproject.report.entity.*;
import com.example.pproject.report.exception.InvalidReportStatusException;
import com.example.pproject.report.exception.ReportNotFoundException;
import com.example.pproject.report.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final MemberPenaltyPointRepository penaltyPointRepository;
    private final AdminMemberRepository memberRepository;
    private final MemberGradeHistoryRepository memberGradeHistoryRepository;
    private final AuditLogRepository auditLogRepository;

    // 1. 신고 생성
    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        validateTargetType(request.getTargetType());

        if (!memberRepository.existsById(request.getReporterMemberId())) {
            throw new ReportNotFoundException("신고자를 찾을 수 없습니다.");
        }

        Report report = new Report();
        report.setReporterMemberId(request.getReporterMemberId());

        report.setTargetType(request.getTargetType());
        report.setTargetJobId(request.getTargetJobId());
        report.setTargetMemberId(request.getTargetMemberId());
        report.setReasonCode(request.getReasonCode());
        report.setReasonDetail(request.getReasonDetail());

        return toReportResponse(reportRepository.save(report));
    }

    // 2. 신고 상세 조회
    public ReportResponse getReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("신고를 찾을 수 없습니다."));
        return toReportResponse(report);
    }

    // 3. 목록 조회들
    public Page<ReportResponse> getReportsByReporter(Long reporterMemberId, Pageable pageable) {
        return reportRepository.findByReporterMemberId(reporterMemberId, pageable).map(this::toReportResponse);
    }
    public Page<ReportResponse> getReportsByStatus(String status, Pageable pageable) {
        validateStatus(status);
        return reportRepository.findByStatus(status, pageable).map(this::toReportResponse);
    }
    public Page<ReportResponse> getReportsByTargetType(String targetType, Pageable pageable) {
        validateTargetType(targetType);
        return reportRepository.findByTargetType(targetType, pageable).map(this::toReportResponse);
    }

    // 6. 신고 처리 (유형 선택 -> 점수 자동 부여)
    @Transactional
    public ModerationActionResponse processReport(ProcessReportRequest request) {
        log.info("신고 처리 시작: reportId={}, decision={}, type={}",
                request.getReportId(), request.getDecision(), request.getViolationType());

        validateDecision(request.getDecision());

        Report report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new ReportNotFoundException("신고를 찾을 수 없습니다."));

        if (!"OPEN".equals(report.getStatus())) {
            throw new InvalidReportStatusException("이미 처리된 신고입니다.");
        }

        // 조치 내역 저장
        ModerationAction action = new ModerationAction();
        action.setReportId(request.getReportId());
        action.setAdminMemberId(request.getAdminMemberId());
        action.setDecision(request.getDecision());
        action.setRestrictDays(request.getRestrictDays());
        action.setReason(request.getReason());

        ModerationAction savedAction = moderationActionRepository.save(action);

        // 신고 상태 업데이트
        String newStatus = "ACCEPT".equals(request.getDecision()) ? "ACCEPTED" : "REJECTED";
        report.setStatus(newStatus);

        // 감사 로그 저장
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

        // 승인 시 벌점 자동 부여
        if ("ACCEPT".equals(request.getDecision())) {
            Long targetMemberId = report.getTargetMemberId();
            if (targetMemberId != null && request.getViolationType() != null) {

                // Enum에서 점수 자동 획득
                ViolationType type = ViolationType.valueOf(request.getViolationType());
                int points = type.getScore();

                addPenaltyPoints(targetMemberId, request.getReportId(), points,
                        "신고 승인 [" + type.getDescription() + "]: " + request.getReason());

                applyAutomaticSanction(targetMemberId, request.getAdminMemberId());
            }
        }

        return toActionResponse(savedAction);
    }

    // 자동 제재 로직
    private void applyAutomaticSanction(Long memberId, Long adminMemberId) {
        Integer totalPoints = penaltyPointRepository.sumPointsByMemberId(memberId);
        if (totalPoints == null) totalPoints = 0;

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ReportNotFoundException("회원을 찾을 수 없습니다."));

        String oldStatus = member.getStatus();
        String newStatus = oldStatus;
        String changeReason = null;
        String changeType = null;

        if (totalPoints >= 100) {
            if (!"WITHDRAWN".equals(oldStatus)) {
                newStatus = "WITHDRAWN";
                changeReason = "누적 벌점 100점 초과로 인한 영구 정지 (Automated)";
                changeType = "FORCE_WITHDRAWAL";
                member.setDeletedAt(LocalDateTime.now());
                member.setPhone(null);
                String deletedEmail = "deleted_" + member.getMemberId() + "_" + member.getEmail();
                if (deletedEmail.length() > 320) deletedEmail = deletedEmail.substring(0, 320);
                member.setEmail(deletedEmail);
            }
        } else if (totalPoints >= 60) {
            if (!"SUSPENDED".equals(oldStatus) && !"WITHDRAWN".equals(oldStatus)) {
                newStatus = "SUSPENDED";
                changeReason = "누적 벌점 60점 초과로 인한 이용 정지 (Automated)";
                changeType = "SUSPEND";
            }
        }

        if (!newStatus.equals(oldStatus)) {
            member.setStatus(newStatus);
            MemberGradeHistory history = MemberGradeHistory.builder()
                    .targetMemberId(member.getMemberId())
                    .adminMemberId(adminMemberId)
                    .prevGrade(member.getMemberGrade())
                    .newGrade(member.getMemberGrade())
                    .prevStatus(oldStatus)
                    .newStatus(newStatus)
                    .changeType(changeType)
                    .changeReason(changeReason)
                    .adminNotes("신고 처리에 따른 시스템 자동 제재 적용 (총 벌점: " + totalPoints + ")")
                    .build();
            memberGradeHistoryRepository.save(history);
        }
    }

    // 기타 메서드들
    public long getReportCountByTargetMember(Long targetMemberId) { return reportRepository.countByTargetMemberId(targetMemberId); }
    public long getReportCountByTargetJob(Long targetJobId) { return reportRepository.countByTargetJobId(targetJobId); }
    public Integer getMemberPenaltyPoints(Long memberId) { return penaltyPointRepository.sumPointsByMemberId(memberId); }
    public List<MemberPenaltyPointResponse> getMemberPenaltyHistory(Long memberId) {
        return penaltyPointRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(this::toPenaltyResponse).collect(Collectors.toList());
    }
    public boolean isMemberBanned(Long memberId) {
        Integer totalPoints = penaltyPointRepository.sumPointsByMemberId(memberId);
        return totalPoints != null && totalPoints >= 100;
    }
    private void addPenaltyPoints(Long memberId, Long reportId, Integer points, String reason) {
        MemberPenaltyPoint penalty = new MemberPenaltyPoint();
        penalty.setMemberId(memberId);
        penalty.setReportId(reportId);
        penalty.setPoints(points);
        if (reason.length() > 200) reason = reason.substring(0, 197) + "...";
        penalty.setReason(reason);
        penaltyPointRepository.save(penalty);
    }
    private void validateTargetType(String targetType) {
        if (!targetType.matches("^(JOB_POSTING|MEMBER|ETC)$")) throw new InvalidReportStatusException("Invalid type");
    }
    private void validateStatus(String status) {
        if (!status.matches("^(OPEN|ACCEPTED|REJECTED)$")) throw new InvalidReportStatusException("Invalid status");
    }
    private void validateDecision(String decision) {
        if (!decision.matches("^(ACCEPT|REJECT)$")) throw new InvalidReportStatusException("Invalid decision");
    }

    private ReportResponse toReportResponse(Report r) {
        return ReportResponse.builder()
                .reportId(r.getReportId())
                .reporterMemberId(r.getReporterMemberId())
                .targetType(r.getTargetType())
                .targetJobId(r.getTargetJobId())
                .targetMemberId(r.getTargetMemberId())
                .reasonCode(r.getReasonCode())
                .reasonDetail(r.getReasonDetail())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ModerationActionResponse toActionResponse(ModerationAction m) {
        return ModerationActionResponse.builder().actionId(m.getActionId()).reportId(m.getReportId()).adminMemberId(m.getAdminMemberId()).decision(m.getDecision()).sanctionLevel(m.getSanctionLevel()).restrictDays(m.getRestrictDays()).reason(m.getReason()).decidedAt(m.getDecidedAt()).build();
    }
    private MemberPenaltyPointResponse toPenaltyResponse(MemberPenaltyPoint p) {
        return MemberPenaltyPointResponse.builder().penaltyId(p.getPenaltyId()).memberId(p.getMemberId()).reportId(p.getReportId()).points(p.getPoints()).reason(p.getReason()).createdAt(p.getCreatedAt()).build();
    }
}