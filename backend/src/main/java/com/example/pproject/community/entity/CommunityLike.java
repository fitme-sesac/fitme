package com.example.pproject.community.entity;

import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 커뮤니티 좋아요 엔티티
 */
@Entity
@Table(name = "community_like", uniqueConstraints = {
        @UniqueConstraint(name = "uq_community_like", columnNames = {"post_id", "member_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private UserEntity member;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
