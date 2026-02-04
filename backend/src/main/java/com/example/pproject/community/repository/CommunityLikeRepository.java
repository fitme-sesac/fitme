package com.example.pproject.community.repository;

import com.example.pproject.community.entity.CommunityLike;
import com.example.pproject.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityLikeRepository extends JpaRepository<CommunityLike, Long> {

    // 특정 게시글에 대한 사용자의 좋아요 조회
    Optional<CommunityLike> findByPostIdAndMemberId(Long postId, Long memberId);

    // 좋아요 존재 여부
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    // 내가 좋아요한 게시글 목록
    @Query("SELECT l.post FROM CommunityLike l WHERE l.member.id = :memberId AND l.post.deletedAt IS NULL ORDER BY l.createdAt DESC")
    Page<CommunityPost> findLikedPostsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    // 게시글 좋아요 삭제
    void deleteByPostIdAndMemberId(Long postId, Long memberId);
}
