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
     * 공지사항 수정 요청
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class UpdateNoticeRequest {

        @NotBlank(message = "제목은 필수입니다")
        private String title;

        @NotBlank(message = "본문은 필수입니다")
        private String body;

        @Builder.Default
        private Boolean isPublic = true;

        private String status;  // ACTIVE, PENDING_DELETE
    }

