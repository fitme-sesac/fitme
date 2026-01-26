package com.example.pproject.employer.dto;

import lombok.*;
import java.util.List;

/**
 * 공개 기업 목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicEmployerListDTO {
    
    private List<PublicEmployerDTO> employers;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
