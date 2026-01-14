package com.example.pproject.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

    /**
     * 공지사항 응답
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class NoticeResponse {

        private Long noticeId;

        private String title;

        private String body;

        private Boolean isPublic;

        private String noticeType;

        private String status;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        private List<AttachmentResponse> attachments;
    }

