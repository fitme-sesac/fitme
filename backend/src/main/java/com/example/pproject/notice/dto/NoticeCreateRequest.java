package com.example.pproject.notice.dto;

import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.Notice.NoticeStatus;
import com.example.pproject.notice.entity.Notice.NoticeType;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List; // [추가]

// ==================== Request DTO ====================

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoticeCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "본문은 필수입니다")
    private String body;

    @NotNull(message = "공개 여부는 필수입니다")
    private Boolean isPublic;

    @NotNull(message = "공지 타입은 필수입니다")
    private NoticeType noticeType;

    private LocalDateTime purgeAfter;

    // [추가] 첨부파일 정보 리스트 (URL, 파일명)
    private List<AttachmentRequest> attachments;

    // Notice 엔티티로 변환 (첨부파일 제외, 기본 정보만 빌드)
    public Notice toEntity() {
        return Notice.builder()
                .title(this.title)
                .body(this.body)
                .isPublic(this.isPublic)
                .noticeType(this.noticeType)
                .status(NoticeStatus.ACTIVE)
                .build();
    }

    // 첨부파일 요청용 내부 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentRequest {
        private String fileUrl;
        private String fileName;
    }
}