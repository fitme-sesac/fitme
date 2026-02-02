package com.example.pproject.community.dto;

import com.example.pproject.community.entity.CommunityPost;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isPinned;
    private Instant createdAt;
    private Instant updatedAt;
    
    // 작성자 정보
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    
    // 현재 사용자 좋아요 여부
    private Boolean isLiked;

    public static PostDTO from(CommunityPost post) {
        return PostDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isPinned(post.getIsPinned())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getUsername())
                .isLiked(false)
                .build();
    }
}
