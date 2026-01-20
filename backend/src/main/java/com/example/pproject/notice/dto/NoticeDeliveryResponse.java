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

import java.time.LocalDateTime;



/**
 * 발송 결과 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDeliveryResponse {

    private Long id;
    private Long memberId;
    private String channel;
    private String status;
    private LocalDateTime deliveredAt;
    private String failedReason;

    public static NoticeDeliveryResponse fromEntity(NoticeDelivery delivery) {
        return NoticeDeliveryResponse.builder()
                .id(delivery.getId())
                .memberId(delivery.getMemberId())
                .channel(delivery.getChannel().name())
                .status(delivery.getStatus().name())
                .deliveredAt(delivery.getDeliveredAt())
                .failedReason(delivery.getFailedReason())
                .build();
    }
}
