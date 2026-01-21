package com.example.pproject.employer.dto;

import lombok.*;
import java.util.List;

/**
 * 지원자 목록 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicantListDTO {
    private List<ApplicantDTO> applicants;
    private int total;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicantDTO {
        private Long applicationId;
        private Long jobId;
        private String jobTitle;
        private Long memberId;
        private String name;
        private String email;
        private String phone;
        private Long resumeId;
        private String resumeTitle;
        private String status;
        private String appliedAt;
        private String viewedAt;
    }
}
