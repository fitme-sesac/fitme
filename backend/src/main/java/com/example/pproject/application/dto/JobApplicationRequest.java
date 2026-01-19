package com.example.pproject.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JobApplicationRequest {

    @NotNull(message = "공고 ID는 필수입니다.")
    private Long jobId;

    @NotNull(message = "이력서 ID는 필수입니다.")
    private Long resumeId;

    private String answers;
}