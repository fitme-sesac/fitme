package com.example.pproject.job.dto;

import lombok.*;

import java.util.List;

/**
 * 채용공고 필터 옵션 DTO
 * - 프론트엔드에서 필터 드롭다운에 표시할 옵션들
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobFilterOptionsDTO {
    
    // 사용 가능한 기술 스택 목록
    private List<String> stacks;
    
    // 사용 가능한 지역 목록 (시/도 단위)
    private List<String> locations;
}
