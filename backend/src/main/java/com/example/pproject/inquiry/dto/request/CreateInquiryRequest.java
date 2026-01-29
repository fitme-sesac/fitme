package com.example.pproject.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInquiryRequest {

    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "문의 내용은 필수입니다")
    private String content;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;  // 문의 카테고리
}