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
 * 공지사항 신규 등록 요청
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNoticeRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "본문은 필수입니다")
    private String body;

    @Builder.Default
    private Boolean isPublic = true;

    @NotNull(message = "공지사항 타입은 필수입니다")
    private String noticeType;  // OPS, POLICY, PRIVACY, TERMS

    @Builder.Default
    private String status = "ACTIVE";
}