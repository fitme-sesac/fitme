package com.example.pproject.faq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FAQ 수정 요청
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFaqRequest {

    @NotBlank(message = "질문은 필수입니다")
    private String question;

    @NotBlank(message = "답변은 필수입니다")
    private String answer;

    @NotNull(message = "공개 여부는 필수입니다")
    private Boolean isPublic;

    @Builder.Default
    private Boolean locked = true;
}
