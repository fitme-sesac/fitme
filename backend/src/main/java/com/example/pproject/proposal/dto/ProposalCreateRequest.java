package com.example.pproject.proposal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProposalCreateRequest {

    @NotNull(message = "대상 인재 ID는 필수입니다")
    private Long candidateId;

    private Long jobId; // 특정 공고 제안 시 (선택)

    @NotBlank(message = "제안 제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내로 작성해주세요")
    private String title;

    @Size(max = 2000, message = "메시지는 2000자 이내로 작성해주세요")
    private String message;

    @Size(max = 100, message = "제안 연봉은 100자 이내로 작성해주세요")
    private String offeredSalary;

    @Size(max = 100, message = "제안 포지션은 100자 이내로 작성해주세요")
    private String offeredPosition;

    private Integer expirationDays = 14; // 만료일 (기본 14일)
}
