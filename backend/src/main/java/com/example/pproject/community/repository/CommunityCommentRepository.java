package com.example.pproject.community.repository;

import com.example.pproject.community.entity.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    // 삭제되지 않은 댓글 조회
    @Query("SELECT c FROM CommunityComment c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CommunityComment> findByIdAndNotDeleted(@Param("id") Long id);

    // 게시글의 댓글 목록
    @Query("SELECT c FROM CommunityComment c WHERE c.post.id = :postId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<CommunityComment> findByPostId(@Param("postId") Long postId);

    // 내 댓글 목록
    @Query("SELECT c FROM CommunityComment c WHERE c.author.id = :authorId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<CommunityComment> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    // 작성자별 댓글 수
    long countByAuthorIdAndDeletedAtIsNull(Long authorId);
}
