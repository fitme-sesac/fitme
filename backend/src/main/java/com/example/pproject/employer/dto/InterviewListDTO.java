package com.example.pproject.employer.dto;

import lombok.*;
import java.util.List;

/**
 * 면접 일정 목록 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewListDTO {
    private List<InterviewDTO> interviews;
    private int total;
    private int year;
    private int month;
}
