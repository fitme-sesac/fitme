package com.example.pproject.employer.dto;

import lombok.*;

/**
 * 면접 일정 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewDTO {
    private Long interviewId;
    private Long applicationId;
    private Long applicantMemberId;
    private String applicantName;
    private String applicantEmail;
    private String jobTitle;
    
    // 면접 정보
    private String stage;       // 1ST, 2ND, FINAL
    private String method;      // ONSITE, VIDEO, PHONE
    private String location;
    private String meetingUrl;
    private String startAt;
    private String endAt;
    private String status;      // PROPOSED, CONFIRMED, CANCELED, DONE
    private String memo;
    
    private String createdAt;
    private String updatedAt;
}
