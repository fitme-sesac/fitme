package com.example.pproject.faq.dto;

import com.example.pproject.faq.entity.FAQ;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FAQDetailResponse {

    private Long id;
    private String question;
    private String answer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FAQDetailResponse from(FAQ faq) {
        return FAQDetailResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}