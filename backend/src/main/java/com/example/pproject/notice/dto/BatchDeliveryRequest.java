package com.example.pproject.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 대량 발송 요청
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDeliveryRequest {

    @NotNull(message = "발송 채널은 필수입니다")
    private String channel;  // EMAIL, SMS, LMS, PUSH

    @NotNull(message = "대상 회원 ID 목록은 필수입니다")
    private List<Long> targetMemberIds;
}