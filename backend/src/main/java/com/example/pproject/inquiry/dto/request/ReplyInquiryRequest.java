package com.example.pproject.inquiry.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyInquiryRequest {

    @NotNull(message = "문의 ID는 필수입니다")
    private Long inquiryId;

    @NotBlank(message = "답변 내용은 필수입니다")
    private String body;

    private Long adminMemberId;  // 답변하는 관리자 ID
}