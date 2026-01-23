package com.example.pproject.faq.dto;

import com.example.pproject.faq.entity.FAQ;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// ==================== Request DTO ====================

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FAQCreateRequest {

    @NotBlank(message = "질문은 필수입니다")
    private String question;

    @NotBlank(message = "답변은 필수입니다")
    private String answer;

    @NotNull(message = "공개 여부는 필수입니다")
    private Boolean isPublic;

    @Builder.Default
    private Boolean locked = true;

    public FAQ toEntity() {
        return FAQ.builder()
                .question(this.question)
                .answer(this.answer)
                .isPublic(this.isPublic)
                .locked(this.locked)
                .build();
    }
}
