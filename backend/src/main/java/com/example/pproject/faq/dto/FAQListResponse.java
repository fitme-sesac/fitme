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
public class FAQListResponse {

    private Long id;
    private String question;
    private Boolean isPublic;
    private LocalDateTime createdAt;

    public static FAQListResponse from(FAQ faq) {
        return FAQListResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .isPublic(faq.getIsPublic())
                .createdAt(faq.getCreatedAt())
                .build();
    }
}