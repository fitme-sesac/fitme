package com.example.pproject.job.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobListResponseDTO {
    
    private List<JobDTO> jobs;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
