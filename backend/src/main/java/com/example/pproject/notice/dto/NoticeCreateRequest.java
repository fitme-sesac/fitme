package com.example.pproject.notice.dto;

import com.example.pproject.notice.entity.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
    private String noticeType;

    // ▼ 수정된 부분: @Builder.Default 추가
    @Builder.Default
    private Boolean isImportant = false;

    // ▼ 수정된 부분: @Builder.Default 추가
    @Builder.Default
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