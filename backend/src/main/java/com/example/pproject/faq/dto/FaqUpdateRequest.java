package com.example.pproject.faq.dto;

import com.example.pproject.faq.entity.Faq;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * FAQ 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqUpdateRequest {

    @NotBlank(message = "질문은 필수입니다")
    private String question;

    @NotBlank(message = "답변은 필수입니다")
    private String answer;

    @NotNull(message = "공개 여부는 필수입니다")
    private Boolean isPublic;

    private Boolean locked;
}