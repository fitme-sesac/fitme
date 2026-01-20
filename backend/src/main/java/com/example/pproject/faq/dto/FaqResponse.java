package com.example.pproject.faq.dto;

import com.example.pproject.faq.entity.Faq;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
/**
 * FAQ 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqResponse {

    private Long id;
    private String question;
    private String answer;
    private Boolean isPublic;
    private Boolean locked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FaqResponse fromEntity(Faq faq) {
        return FaqResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .isPublic(faq.getIsPublic())
                .locked(faq.getLocked())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
