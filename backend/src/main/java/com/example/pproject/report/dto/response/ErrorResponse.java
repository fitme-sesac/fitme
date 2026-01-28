package com.example.pproject.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Integer code;
    private String message;
    private String status;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
