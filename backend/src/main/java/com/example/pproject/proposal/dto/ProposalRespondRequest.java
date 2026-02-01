package com.example.pproject.proposal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProposalRespondRequest {

    @NotNull(message = "응답 여부는 필수입니다")
    private Boolean accept; // true: 수락, false: 거절

    @Size(max = 1000, message = "응답 메시지는 1000자 이내로 작성해주세요")
    private String message;
}
