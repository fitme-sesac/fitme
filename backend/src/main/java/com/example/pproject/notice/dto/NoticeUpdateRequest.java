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
 * 공지사항 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeUpdateRequest {

    private String title;

    private String body;

    private String noticeType;

    private Boolean isImportant;

    private Boolean isPublic;

    private String status;  // ACTIVE, PENDING_DELETE
}