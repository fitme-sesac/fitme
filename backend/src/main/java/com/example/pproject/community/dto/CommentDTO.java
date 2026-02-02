package com.example.pproject.community.dto;

import com.example.pproject.community.entity.CommunityComment;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private Long id;
    private Long postId;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
    
    // 작성자 정보
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    public static CommentDTO from(CommunityComment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getUsername())
                .build();
    }
}
