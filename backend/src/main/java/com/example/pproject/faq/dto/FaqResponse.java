package com.example.pproject.faq.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FAQ 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqResponse {

    private Long faqId;

    private String question;

    private String answer;

    private Boolean locked;

    private Boolean isPublic;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
