package com.example.pproject.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberGradeHistoryResponse {

    private Long historyId;
    private Long memberId;
    private String memberName;

    private String previousGrade;
    private String newGrade;
    private String changeType;
    private String changeReason;

    private Long adminMemberId;
    private String adminName;
    private String adminNotes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}