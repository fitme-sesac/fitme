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
import lombok.*;
/**
 * 첨부파일 업로드 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeAttachmentRequest {

    @NotBlank(message = "파일 URL은 필수입니다")
    private String fileUrl;

    @NotBlank(message = "파일명은 필수입니다")
    private String fileName;

    @NotNull(message = "파일 사이즈는 필수입니다")
    private Long fileSize;

    private String fileType;

    public NoticeAttachment toEntity() {
        return NoticeAttachment.builder()
                .fileUrl(this.fileUrl)
                .fileName(this.fileName)
                .fileSize(this.fileSize)
                .fileType(this.fileType)
                .build();
    }
}