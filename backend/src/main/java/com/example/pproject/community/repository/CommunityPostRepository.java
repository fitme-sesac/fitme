package com.example.pproject.community.repository;

import com.example.pproject.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    // 삭제되지 않은 게시글 조회
    @Query("SELECT p FROM CommunityPost p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<CommunityPost> findByIdAndNotDeleted(@Param("id") Long id);

    // 게시글 목록 (삭제되지 않은 것만)
    @Query("SELECT p FROM CommunityPost p WHERE p.deletedAt IS NULL ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<CommunityPost> findAllNotDeleted(Pageable pageable);

    // 카테고리별 게시글 목록
    @Query("SELECT p FROM CommunityPost p WHERE p.category = :category AND p.deletedAt IS NULL ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<CommunityPost> findByCategory(@Param("category") String category, Pageable pageable);

    // 키워드 검색
    @Query("SELECT p FROM CommunityPost p WHERE p.deletedAt IS NULL AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY p.createdAt DESC")
    Page<CommunityPost> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 카테고리 + 키워드 검색
    @Query("SELECT p FROM CommunityPost p WHERE p.category = :category AND p.deletedAt IS NULL AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY p.createdAt DESC")
    Page<CommunityPost> searchByCategoryAndKeyword(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    // 내 게시글 목록
    @Query("SELECT p FROM CommunityPost p WHERE p.author.id = :authorId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<CommunityPost> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    // 인기 게시글 (좋아요 순)
    @Query("SELECT p FROM CommunityPost p WHERE p.deletedAt IS NULL ORDER BY p.likeCount DESC, p.viewCount DESC")
    List<CommunityPost> findPopularPosts(Pageable pageable);

    // 공지사항 (고정 게시글)
    @Query("SELECT p FROM CommunityPost p WHERE p.isPinned = true AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<CommunityPost> findPinnedPosts(Pageable pageable);

    // 작성자별 게시글 수
    long countByAuthorIdAndDeletedAtIsNull(Long authorId);

    // 작성자가 받은 총 좋아요 수
    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM CommunityPost p WHERE p.author.id = :authorId AND p.deletedAt IS NULL")
    long sumLikesByAuthorId(@Param("authorId") Long authorId);
}
