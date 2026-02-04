package com.example.pproject.community.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityStatsDTO {
    private long postCount;
    private long commentCount;
    private long receivedLikes;
}
