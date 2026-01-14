package com.example.pproject.notice.dto;

import com.example.pproject.Constant.DeliveryChannel;
import com.example.pproject.Constant.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 발송 결과 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResultResponse {

    private Long deliveryId;

    private Long noticeId;

    private Long memberId;

    private DeliveryChannel channel;

    private DeliveryStatus status;

    private String failReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}