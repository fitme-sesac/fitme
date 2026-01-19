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
 * 공지사항 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "본문은 필수입니다")
    private String body;

    @NotNull(message = "공지 타입은 필수입니다")
    private String noticeType;  // POLICY, OPS

    private Boolean isImportant = false;

    private Boolean isPublic = true;

    public Notice toEntity() {
        return Notice.builder()
                .title(this.title)
                .body(this.body)
                .noticeType(Notice.NoticeType.valueOf(this.noticeType))
                .isImportant(this.isImportant)
                .isPublic(this.isPublic)
                .build();
    }
}
