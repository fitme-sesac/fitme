package com.example.pproject.notice.dto;

import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.NoticeAttachment;
import com.example.pproject.notice.entity.NoticeDelivery;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 공지사항 목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeListResponse {

    private Long id;
    private String title;
    private String noticeType;
    private Boolean isImportant;
    private String status;
    private LocalDateTime createdAt;

    public static NoticeListResponse fromEntity(Notice notice) {
        return NoticeListResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .noticeType(notice.getNoticeType().name())
                .isImportant(notice.getIsImportant())
                .status(notice.getStatus().name())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}