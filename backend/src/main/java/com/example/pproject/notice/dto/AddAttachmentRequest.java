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
     * 첨부파일 추가 요청
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class AddAttachmentRequest {

        @NotBlank(message = "파일 URL은 필수입니다")
        private String fileUrl;

        @NotBlank(message = "파일 이름은 필수입니다")
        private String fileName;
    }

