package com.example.pproject.proposal.dto;

import com.example.pproject.Constant.ProposalStatus;
import com.example.pproject.proposal.entity.TalentProposal;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@Builder
public class ProposalResponse {

    private Long proposalId;
    private ProposalStatus status;

    // 기업 정보
    private Long employerId;
    private String employerName;
    private String employerLogo;

    // 구직자 정보
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;

    // 공고 정보 (있을 경우)
    private Long jobId;
    private String jobTitle;

    // 제안 내용
    private String title;
    private String message;
    private String offeredSalary;
    private String offeredPosition;

    // 시간 정보
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime viewedAt;
    private LocalDateTime respondedAt;
    private String responseMessage;

    public static ProposalResponse from(TalentProposal proposal) {
        ProposalResponseBuilder builder = ProposalResponse.builder()
                .proposalId(proposal.getId())
                .status(proposal.getStatus())
                .title(proposal.getTitle())
                .message(proposal.getMessage())
                .offeredSalary(proposal.getOfferedSalary())
                .offeredPosition(proposal.getOfferedPosition())
                .createdAt(toLocalDateTime(proposal.getCreatedAt()))
                .expiresAt(proposal.getExpiresAt())
                .viewedAt(proposal.getViewedAt())
                .respondedAt(proposal.getRespondedAt())
                .responseMessage(proposal.getResponseMessage());

        // 기업 정보
        if (proposal.getEmployer() != null) {
            builder.employerId(proposal.getEmployer().getId())
                    .employerName(proposal.getEmployer().getName())
                    .employerLogo(proposal.getEmployer().getLogoUrl());
        }

        // 구직자 정보
        if (proposal.getCandidate() != null) {
            builder.candidateId(proposal.getCandidate().getId())
                    .candidateName(proposal.getCandidate().getUsername())
                    .candidateEmail(proposal.getCandidate().getEmail());
        }

        // 공고 정보
        if (proposal.getJob() != null) {
            builder.jobId(proposal.getJob().getId())
                    .jobTitle(proposal.getJob().getTitle());
        }

        return builder.build();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
