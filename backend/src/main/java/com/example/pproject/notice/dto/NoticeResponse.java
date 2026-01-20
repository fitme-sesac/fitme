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
 * 공지사항 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeResponse {

    private Long id;
    private String title;
    private String body;
    private String noticeType;
    private Boolean isImportant;
    private Boolean isPublic;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<NoticeAttachmentResponse> attachments;

    public static NoticeResponse fromEntity(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .body(notice.getBody())
                .noticeType(notice.getNoticeType().name())
                .isImportant(notice.getIsImportant())
                .isPublic(notice.getIsPublic())
                .status(notice.getStatus().name())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .attachments(notice.getAttachments() != null ?
                        notice.getAttachments().stream()
                                .map(NoticeAttachmentResponse::fromEntity)
                                .collect(Collectors.toList())
                        : null)
                .build();
    }
}
