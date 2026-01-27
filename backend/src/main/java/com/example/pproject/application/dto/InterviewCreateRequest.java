package com.example.pproject.application.dto;

import com.example.pproject.Constant.InterviewMethod;
import com.example.pproject.Constant.InterviewStage;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 면접 일정 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class InterviewCreateRequest {

    @NotNull(message = "지원 ID는 필수입니다")
    private Long applicationId;

    @NotNull(message = "면접 단계는 필수입니다")
    private InterviewStage stage;

    @NotNull(message = "면접 방식은 필수입니다")
    private InterviewMethod method;

    private String location;      // 대면 면접 장소

    private String meetingUrl;    // 화상 면접 URL

    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startAt;

    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endAt;
}
