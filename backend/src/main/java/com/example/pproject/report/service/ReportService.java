package com.example.pproject.report.service;

import com.example.pproject.report.entity.Report;
import com.example.pproject.report.entity.ModerationAction;
import com.example.pproject.report.entity.MemberPenaltyPoint;
import com.example.pproject.report.repository.ReportRepository;
import com.example.pproject.report.repository.ModerationActionRepository;
import com.example.pproject.report.repository.MemberPenaltyPointRepository;
import com.example.pproject.report.dto.request.CreateReportRequest;
import com.example.pproject.report.dto.request.ProcessReportRequest;
import com.example.pproject.report.dto.response.ReportResponse;
import com.example.pproject.report.dto.response.ModerationActionResponse;
import com.example.pproject.report.dto.response.MemberPenaltyPointResponse;
import com.example.pproject.report.exception.ReportNotFoundException;
import com.example.pproject.report.exception.InvalidReportStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ModerationActionRepository moderationRepository;
    private final MemberPenaltyPointRepository penaltyRepository;

    /**
     * 신고 생성
     */
    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        log.info("신고 생성 시작: reporterMemberId={}, targetType={}",
                request.getReporterMemberId(), request.getTargetType());

        validateTargetType(request.getTargetType());

        Report report = new Report();
        report.setReporterMemberId(request.getReporterMemberId());
        report.setTargetType(request.getTargetType());
        report.setTargetJobId(request.getTargetJobId());
        report.setTargetMemberId(request.getTargetMemberId());
        report.setReasonCode(request.getReasonCode());
        report.setReasonDetail(request.getReasonDetail());
        report.setStatus("OPEN");

        Report saved = reportRepository.save(report);
        log.info("신고 생성 완료: reportId={}", saved.getReportId());

        return toResponse(saved);
    }

    /**
     * 신고 조회
     */
    public ReportResponse getReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("신고를 찾을 수 없습니다. reportId=" + reportId));

        return toResponse(report);
    }

    /**
     * 신고자별 신고 목록
     */
    public Page<ReportResponse> getReportsByReporter(Long reporterMemberId, Pageable pageable) {
        return reportRepository.findByReporterMemberId(reporterMemberId, pageable)
                .map(this::toResponse);
    }

    /**
     * 상태별 신고 목록 (관리자용)
     */
    public Page<ReportResponse> getReportsByStatus(String status, Pageable pageable) {
        validateStatus(status);
        return reportRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }

    /**
     * 신고 대상 타입별 조회
     */
    public Page<ReportResponse> getReportsByTargetType(String targetType, Pageable pageable) {
        validateTargetType(targetType);
        return reportRepository.findByTargetType(targetType, pageable)
                .map(this::toResponse);
    }

    /**
     * 신고 처리 (중재 조치)
     */
    @Transactional
    public ModerationActionResponse processReport(ProcessReportRequest request) {
        log.info("신고 처리 시작: reportId={}, decision={}", request.getReportId(), request.getDecision());

        validateDecision(request.getDecision());

        Report report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new ReportNotFoundException("신고를 찾을 수 없습니다."));

        if (!"OPEN".equals(report.getStatus())) {
            throw new InvalidReportStatusException("이미 처리된 신고입니다.");
        }

        // 중재 조치 저장
        ModerationAction action = new ModerationAction();
        action.setReportId(request.getReportId());
        action.setAdminMemberId(request.getAdminMemberId());
        action.setDecision(request.getDecision());
        action.setSanctionLevel(request.getSanctionLevel());
        action.setRestrictDays(request.getRestrictDays());
        action.setReason(request.getReason());
        action.setDecidedAt(LocalDateTime.now());

        ModerationAction savedAction = moderationRepository.save(action);

        // 신고 상태 업데이트
        report.setStatus("ACCEPT".equals(request.getDecision()) ? "ACCEPTED" : "REJECTED");
        reportRepository.save(report);

        // ACCEPT인 경우 경고 점수 부여
        if ("ACCEPT".equals(request.getDecision())) {
            addPenaltyPoints(report.getTargetMemberId(), request.getReportId(),
                    request.getSanctionLevel() != null ? request.getSanctionLevel() * 10 : 10,
                    "신고에 의한 경고: " + request.getReason());
        }

        log.info("신고 처리 완료: reportId={}, decision={}", request.getReportId(), request.getDecision());

        return toModerationResponse(savedAction);
    }

    /**
     * 신고 대상별 신고 건수 조회
     */
    public long getReportCountByTargetMember(Long targetMemberId) {
        return reportRepository.countByTargetMemberId(targetMemberId);
    }

    public long getReportCountByTargetJob(Long targetJobId) {
        return reportRepository.countByTargetJobId(targetJobId);
    }

    /**
     * 회원 경고 점수 조회
     */
    public Integer getMemberPenaltyPoints(Long memberId) {
        return penaltyRepository.getTotalPenaltyPoints(memberId);
    }

    /**
     * 회원 경고 이력 조회
     */
    public List<MemberPenaltyPointResponse> getMemberPenaltyHistory(Long memberId) {
        return penaltyRepository.findByMemberId(memberId)
                .stream()
                .map(this::toPenaltyResponse)
                .collect(Collectors.toList());
    }

    /**
     * 경고 점수 추가 (내부 메서드)
     */
    @Transactional
    private void addPenaltyPoints(Long memberId, Long reportId, Integer points, String reason) {
        if (memberId == null) return;

        MemberPenaltyPoint penalty = new MemberPenaltyPoint();
        penalty.setMemberId(memberId);
        penalty.setReportId(reportId);
        penalty.setPoints(points);
        penalty.setReason(reason);

        penaltyRepository.save(penalty);
        log.info("경고 점수 추가: memberId={}, points={}", memberId, points);
    }

    /**
     * 신고 대상 타입 유효성 검증
     */
    private void validateTargetType(String targetType) {
        if (!targetType.matches("^(JOB_POSTING|MEMBER|ETC)$")) {
            throw new InvalidReportStatusException("유효하지 않은 신고 대상 타입입니다: " + targetType);
        }
    }

    /**
     * 신고 상태 유효성 검증
     */
    private void validateStatus(String status) {
        if (!status.matches("^(OPEN|ACCEPTED|REJECTED)$")) {
            throw new InvalidReportStatusException("유효하지 않은 신고 상태입니다: " + status);
        }
    }

    /**
     * 판정 유효성 검증
     */
    private void validateDecision(String decision) {
        if (!decision.matches("^(ACCEPT|REJECT)$")) {
            throw new InvalidReportStatusException("유효하지 않은 판정입니다: " + decision);
        }
    }

    /**
     * Entity → DTO 변환
     */
    private ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .reporterMemberId(report.getReporterMemberId())
                .targetType(report.getTargetType())
                .targetJobId(report.getTargetJobId())
                .targetMemberId(report.getTargetMemberId())
                .reasonCode(report.getReasonCode())
                .reasonDetail(report.getReasonDetail())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private ModerationActionResponse toModerationResponse(ModerationAction action) {
        return ModerationActionResponse.builder()
                .actionId(action.getActionId())
                .reportId(action.getReportId())
                .adminMemberId(action.getAdminMemberId())
                .decision(action.getDecision())
                .sanctionLevel(action.getSanctionLevel())
                .restrictDays(action.getRestrictDays())
                .reason(action.getReason())
                .decidedAt(action.getDecidedAt())
                .build();
    }

    private MemberPenaltyPointResponse toPenaltyResponse(MemberPenaltyPoint penalty) {
        return MemberPenaltyPointResponse.builder()
                .penaltyId(penalty.getPenaltyId())
                .memberId(penalty.getMemberId())
                .reportId(penalty.getReportId())
                .points(penalty.getPoints())
                .reason(penalty.getReason())
                .createdAt(penalty.getCreatedAt())
                .build();
    }
}