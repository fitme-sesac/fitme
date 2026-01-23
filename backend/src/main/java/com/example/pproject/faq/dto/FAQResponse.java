package com.example.pproject.faq.dto;

import com.example.pproject.faq.entity.FAQ;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// ==================== Response DTO ====================

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FAQResponse {

    private Long id;
    private String question;
    private String answer;
    private Boolean locked;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FAQResponse from(FAQ faq) {
        return FAQResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .locked(faq.getLocked())
                .isPublic(faq.getIsPublic())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
