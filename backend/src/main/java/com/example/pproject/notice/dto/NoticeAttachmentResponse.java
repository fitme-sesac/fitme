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
 * 첨부파일 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeAttachmentResponse {

    private Long id;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime createdAt;

    public static NoticeAttachmentResponse fromEntity(NoticeAttachment attachment) {
        return NoticeAttachmentResponse.builder()
                .id(attachment.getId())
                .fileUrl(attachment.getFileUrl())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .fileType(attachment.getFileType())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
