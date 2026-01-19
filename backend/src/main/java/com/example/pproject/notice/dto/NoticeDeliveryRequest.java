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

import java.util.List;


/**
 * 공지사항 배송 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDeliveryRequest {

    @NotNull(message = "배송 채널은 필수입니다")
    private String channel;  // EMAIL, SMS

    @NotNull(message = "대상 회원 ID 목록은 필수입니다")
    private List<Long> targetMemberIds;
}
