package com.example.pproject.community.entity;

import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 커뮤니티 게시글 엔티티
 */
@Entity
@Table(name = "community_post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 카테고리: career_tips, qna, company_news, general
     */
    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        if (viewCount == null) viewCount = 0;
        if (likeCount == null) likeCount = 0;
        if (commentCount == null) commentCount = 0;
        if (isPinned == null) isPinned = false;
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
