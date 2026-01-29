package com.example.pproject.inquiry.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInquiryStatusRequest {

    @NotNull(message = "문의 ID는 필수입니다")
    private Long inquiryId;

    @NotNull(message = "상태는 필수입니다")
    private String status;  // OPEN, ANSWERED, CLOSED
}