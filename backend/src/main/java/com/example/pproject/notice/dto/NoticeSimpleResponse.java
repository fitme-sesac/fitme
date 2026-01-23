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
public class NoticeSimpleResponse {

    private Long id;
    private String title;
    private LocalDateTime createdAt;

    public static NoticeSimpleResponse from(Notice notice) {
        return NoticeSimpleResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
