package com.example.pproject.notice.dto;

import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.NoticeAttachment; // [추가]
import com.example.pproject.notice.entity.Notice.NoticeStatus;
import com.example.pproject.notice.entity.Notice.NoticeType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List; // [추가]
import java.util.stream.Collectors; // [추가]

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoticeResponse {

    private Long id;
    private String title;
    private String body;
    private Boolean isPublic;
    private NoticeType noticeType;
    private NoticeStatus status;
    private LocalDateTime purgeAfter;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // [추가] 첨부파일 목록 필드
    private List<AttachmentDto> attachments;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .body(notice.getBody())
                .isPublic(notice.getIsPublic())
                .noticeType(notice.getNoticeType())
                .status(notice.getStatus())
                .purgeAfter(notice.getPurgeAfter())
                .createdBy(notice.getCreatedBy())
                .updatedBy(notice.getUpdatedBy())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                // [추가] 첨부파일 엔티티 -> DTO 변환 매핑
                .attachments(notice.getAttachments() != null ?
                        notice.getAttachments().stream()
                                .map(AttachmentDto::from)
                                .collect(Collectors.toList()) : List.of())
                .build();
    }

    // [추가] 첨부파일 정보를 담을 내부 DTO 클래스
    @Getter
    @Builder
    @AllArgsConstructor
    public static class AttachmentDto {
        private Long id;
        private String fileUrl;
        private String fileName;

        public static AttachmentDto from(NoticeAttachment entity) {
            return AttachmentDto.builder()
                    .id(entity.getId())
                    .fileUrl(entity.getFileUrl())
                    .fileName(entity.getFileName())
                    .build();
        }
    }
}