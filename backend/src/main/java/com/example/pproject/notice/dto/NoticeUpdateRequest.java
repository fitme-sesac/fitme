package com.example.pproject.notice.dto;

import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.Notice.NoticeStatus;
import com.example.pproject.notice.entity.Notice.NoticeType;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoticeUpdateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "본문은 필수입니다")
    private String body;

    @NotNull(message = "공개 여부는 필수입니다")
    private Boolean isPublic;

    @NotNull(message = "공지 타입은 필수입니다")
    private NoticeType noticeType;

    private LocalDateTime purgeAfter;

    private NoticeStatus status;
}