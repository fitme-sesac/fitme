package com.example.pproject.application.dto;

import com.example.pproject.Constant.InterviewResponseType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 면접 응답 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class InterviewRespondRequest {

    @NotNull(message = "응답 유형은 필수입니다")
    private InterviewResponseType response;

    private String message;  // 추가 메시지 (거절 사유, 변경 요청 내용 등)
}
